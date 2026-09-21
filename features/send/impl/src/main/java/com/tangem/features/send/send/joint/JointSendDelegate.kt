package com.tangem.features.send.send.joint

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.jointaccount.gas.SafeTxGasBuilder
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperationCreateError
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
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationUM
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/** What the send flow needs from the joint account domain: the account itself, its fee coin and the fee estimate */
internal class JointSendDelegate @Inject constructor(
    private val jointAccountListSupplier: SingleJointAccountListSupplier,
    private val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase,
    private val gasBuilder: SafeTxGasBuilder,
    private val createJointOperationUseCase: CreateJointOperationUseCase,
    private val payloadCodec: JointTransferPayloadCodec,
    private val networkReader: JointNetworkReader,
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
     * The Safe pays the fee in the network's coin. The regular fee-paid lookup resolves the coin among personal
     * accounts, so for a joint token the account's own coin is taken; without it the fee has no fiat part.
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
            .build(userWalletId = userWalletId, network = currency.network, call = call)
            .map(JointSendFeeConverter(network = currency.network)::convert)
            .mapLeft(GetFeeError::DataError)
    }

    suspend fun createOperation(
        account: Account.Joint,
        currency: CryptoCurrency,
        amountUM: AmountState,
        destinationUM: DestinationUM,
    ): Either<JointOperationCreateError, Unit> {
        val intent = JointTransferIntentFactory.create(amountUM, destinationUM, currency)
            ?: return JointOperationCreateError.InvalidPayload.left()

        return createJointOperationUseCase(account = account, network = currency.network, intent = intent)
    }
}