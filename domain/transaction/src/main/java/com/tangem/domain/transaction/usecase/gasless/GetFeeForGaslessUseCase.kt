package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.blockchain.common.TransactionData
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.provider.NonPortfolioCurrenciesProvider
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations.getCoinStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.GetFeeError.GaslessError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.raiseIllegalStateError
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import java.math.BigDecimal

/**
 * Fee with the payment currency picked automatically, dispatched to the resolver that owns the network's
 * compensation protocol. The resolvers cover disjoint networks, so the order they are asked in does not
 * change the answer.
 */
class GetFeeForGaslessUseCase(
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val nonPortfolioCurrenciesProvider: NonPortfolioCurrenciesProvider,
    private val currencyChecksRepository: CurrencyChecksRepository,
    private val getFeeUseCase: GetFeeUseCase,
    private val evmGaslessFeeResolver: EvmGaslessFeeResolver,
    private val tronGaslessFeeResolver: TronGaslessFeeResolver,
) {

    /**
     * @param sentAmount required for a yield-supply send, whose [TransactionData.Uncompiled.amount] is zeroed.
     * @param onNativeFeePreferred fires when the network coin was picked over a compensation fee and covers
     * the fee; see [TronGaslessFeeResolver.resolveWithAutoFeeToken].
     * @param sentCurrencyStatus the caller's status of the sent currency, used while the account list lags behind.
     */
    suspend operator fun invoke(
        userWallet: UserWallet,
        network: Network,
        transactionData: TransactionData,
        sentAmount: BigDecimal? = null,
        sentCurrencyStatus: CryptoCurrencyStatus? = null,
        onNativeFeePreferred: () -> Unit = {},
    ): Either<GetFeeError, TransactionFeeExtended> {
        return either {
            catch(
                block = {
                    val accountStatusList = singleAccountStatusListSupplier.getSyncOrNull(userWallet.walletId)
                        ?: raiseIllegalStateError("AccountStatusList is null for ${userWallet.walletId}")

                    val nativeCurrencyStatus = accountStatusList.getCoinStatus(network).getOrNull()
                        ?: nonPortfolioCurrenciesProvider.getStatuses(userWallet.walletId, network)
                            .firstOrNull { it.currency is CryptoCurrency.Coin }
                        ?: raiseIllegalStateError("No native currency found: ${network.id}")

                    if (currencyChecksRepository.isNetworkSupportedForGaslessTx(network)) {
                        evmGaslessFeeResolver.resolveWithAutoFeeToken(
                            userWallet = userWallet,
                            network = network,
                            transactionData = transactionData,
                            nativeCurrencyStatus = nativeCurrencyStatus,
                            accountStatusList = accountStatusList,
                            sentAmount = sentAmount,
                        ).bind()
                    } else {
                        resolveNativeOrCompensationFee(
                            userWallet = userWallet,
                            network = network,
                            transactionData = transactionData,
                            nativeCurrencyStatus = nativeCurrencyStatus,
                            accountStatusList = accountStatusList,
                            sentCurrencyStatus = sentCurrencyStatus,
                            onNativeFeePreferred = onNativeFeePreferred,
                        )
                    }
                },
                catch = {
                    raise(GaslessError.DataError(it))
                },
            )
        }
    }

    /** The plain native fee, unless the network has a compensation flow of its own. */
    @Suppress("LongParameterList")
    private suspend fun Raise<GetFeeError>.resolveNativeOrCompensationFee(
        userWallet: UserWallet,
        network: Network,
        transactionData: TransactionData,
        nativeCurrencyStatus: CryptoCurrencyStatus,
        accountStatusList: AccountStatusList,
        sentCurrencyStatus: CryptoCurrencyStatus?,
        onNativeFeePreferred: () -> Unit,
    ): TransactionFeeExtended {
        val loadNativeFee: suspend () -> Either<GetFeeError, TransactionFeeExtended> = {
            getFeeUseCase.invoke(userWallet, network, transactionData).map { fee ->
                TransactionFeeExtended(
                    transactionFee = fee,
                    feeTokenId = nativeCurrencyStatus.currency.id,
                    nativeFee = fee,
                )
            }
        }

        val compensationFee = tronGaslessFeeResolver.resolveWithAutoFeeToken(
            userWalletId = userWallet.walletId,
            network = network,
            transactionData = transactionData,
            nativeCurrencyStatus = nativeCurrencyStatus,
            accountStatusList = accountStatusList,
            sentCurrencyStatus = sentCurrencyStatus,
            loadNativeFee = loadNativeFee,
            onNativeFeePreferred = onNativeFeePreferred,
        )
        return (compensationFee ?: loadNativeFee()).bind()
    }
}