package com.tangem.features.send.send.joint

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.flatMap
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.jointaccount.gas.JointGasTokenResolver
import com.tangem.domain.jointaccount.gas.SafeTxGasBuilder
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperationCreateError
import com.tangem.domain.jointaccount.model.SafeGasToken
import com.tangem.domain.jointaccount.network.JointNetworkReader
import com.tangem.domain.jointaccount.safe.JointTransferPayloadCodec
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.usecase.CreateJointOperationUseCase
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationUM
import com.tangem.utils.extensions.orZero
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.firstOrNull
import java.math.BigDecimal
import javax.inject.Inject

/** What the send flow needs from the joint account domain: the account itself, its fee coin and the fee estimate */
@Suppress("LongParameterList")
internal class JointSendDelegate @Inject constructor(
    private val jointAccountListSupplier: SingleJointAccountListSupplier,
    private val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase,
    private val gasBuilder: SafeTxGasBuilder,
    private val createJointOperationUseCase: CreateJointOperationUseCase,
    private val payloadCodec: JointTransferPayloadCodec,
    private val networkReader: JointNetworkReader,
    private val gasTokenResolver: JointGasTokenResolver,
) {

    suspend fun findJointAccount(userWalletId: UserWalletId, account: Account.Joint): JointAccount? {
        return jointAccountListSupplier(userWalletId = userWalletId)
            .firstOrNull()
            .orEmpty()
            .firstOrNull { it.cryptoAccountId.equals(account.accountId.value, ignoreCase = true) }
    }

    /** An unreadable chain is taken as deployed: no note beats a setup fee that is not charged */
    suspend fun isSafeDeployed(userWalletId: UserWalletId, network: Network): Boolean {
        return networkReader.isSafeDeployed(userWalletId = userWalletId, network = network).getOrElse { true }
    }

    /**
     * The Safe pays the fee in the network's coin unless a Smart Gas token is picked. The regular fee-paid lookup
     * resolves the coin among personal accounts, so for a joint token the account's own coin is taken; without it
     * the fee has no fiat part.
     */
    suspend fun resolveFeeCurrencyStatus(
        userWalletId: UserWalletId,
        account: Account.Joint,
        status: CryptoCurrencyStatus,
    ): CryptoCurrencyStatus? {
        if (status.currency is CryptoCurrency.Coin) return status

        val coin = account.cryptoCurrencies
            .firstOrNull { it is CryptoCurrency.Coin && it.network == status.currency.network }
            ?: return null

        return getAccountCurrencyStatusUseCase
            .invokeSync(userWalletId = userWalletId, currency = coin)
            .getOrNull()
            ?.status
    }

    /** The estimate reuses the [SafeTxGasBuilder] the creation path signs with, so both see the same figure */
    suspend fun loadFee(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        amountUM: AmountState,
        destinationUM: DestinationUM,
    ): Either<GetFeeError, TransactionFee> {
        val intent = JointTransferIntentFactory.create(amountUM, destinationUM, currency)
            ?: return GetFeeError.DataError(IllegalStateException("The joint transfer form is incomplete")).left()
        val call = payloadCodec.encode(intent)
            ?: return GetFeeError.DataError(IllegalStateException("The joint transfer cannot be encoded")).left()

        return gasBuilder
            .build(userWalletId = userWalletId, network = currency.network, call = call, gasToken = SafeGasToken.Coin)
            .map(JointSendFeeConverter(network = currency.network)::convert)
            .mapLeft(GetFeeError::DataError)
    }

    /**
     * The coin when [feeCurrencyStatus] is null or the coin itself, otherwise the Smart Gas token it names. The coin
     * figure is always kept as the native fee, which the fee-token list compares with the coin balance.
     */
    @Suppress("LongParameterList")
    suspend fun loadFeeExtended(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        feeCoin: CryptoCurrency,
        feeCurrencyStatus: CryptoCurrencyStatus?,
        amountUM: AmountState,
        destinationUM: DestinationUM,
    ): Either<GetFeeError, TransactionFeeExtended> = either {
        val intent = ensureNotNull(JointTransferIntentFactory.create(amountUM, destinationUM, currency)) {
            GetFeeError.DataError(IllegalStateException("The joint transfer form is incomplete"))
        }
        val call = ensureNotNull(payloadCodec.encode(intent)) {
            GetFeeError.DataError(IllegalStateException("The joint transfer cannot be encoded"))
        }
        val coinSafeTx = gasBuilder
            .build(userWalletId = userWalletId, network = currency.network, call = call, gasToken = SafeGasToken.Coin)
            .mapLeft(GetFeeError::DataError)
            .bind()
        val nativeFee = JointSendFeeConverter(network = currency.network).convert(coinSafeTx)

        val feeToken = feeCurrencyStatus?.currency as? CryptoCurrency.Token
            ?: return@either TransactionFeeExtended(
                transactionFee = nativeFee,
                feeTokenId = feeCoin.id,
                nativeFee = nativeFee,
            )

        val gasToken = resolveGasToken(network = currency.network, feeToken = feeToken)
            .onLeft { TangemLogger.e("Unable to price the joint fee in ${feeToken.symbol}", it) }
            .mapLeft(GetFeeError.GaslessError::DataError)
            .bind()

        // The same reading the builder would price in the token: its coin price already carries the margin
        val tokenSafeTx = coinSafeTx.copy(
            gasPrice = gasToken.gasPriceInTokenCurrency(coinGasPrice = coinSafeTx.gasPrice),
            gasToken = gasToken.address,
        )
        val tokenFee = JointSendFeeConverter(network = currency.network, feeToken = feeToken, gasToken = gasToken)
            .convert(tokenSafeTx)

        val feeTokenBalance = feeCurrencyStatus.value.amount ?: BigDecimal.ZERO
        ensure(feeTokenBalance >= tokenFee.normal.amount.value.orZero()) { GetFeeError.GaslessError.NotEnoughFunds }

        TransactionFeeExtended(transactionFee = tokenFee, feeTokenId = feeToken.id, nativeFee = nativeFee)
    }

    suspend fun createOperation(
        account: Account.Joint,
        currency: CryptoCurrency,
        amount: BigDecimal,
        destinationUM: DestinationUM,
        feeToken: CryptoCurrency.Token?,
    ): Either<JointOperationCreateError, Unit> = either {
        val intent = ensureNotNull(JointTransferIntentFactory.create(amount, destinationUM, currency)) {
            JointOperationCreateError.InvalidPayload
        }
        val gasToken = resolveGasToken(network = currency.network, feeToken = feeToken)
            .mapLeft(JointOperationCreateError::Failed)
            .bind()

        createJointOperationUseCase(account = account, network = currency.network, intent = intent, gasToken = gasToken)
            .bind()
    }

    private suspend fun resolveGasToken(
        network: Network,
        feeToken: CryptoCurrency.Token?,
    ): Either<Throwable, SafeGasToken> {
        if (feeToken == null) return Either.Right(SafeGasToken.Coin)

        return gasTokenResolver.resolve(
            network = network,
            address = feeToken.contractAddress,
            rawCurrencyId = feeToken.id.rawCurrencyId,
        ).flatMap { gasToken ->
            gasToken?.let { Either.Right(it) }
                ?: Either.Left(IllegalStateException("Not a Smart Gas token: ${feeToken.contractAddress}"))
        }
    }
}