package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.blockchain.common.TransactionData
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations.getCoinStatus
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations.getCryptoCurrencyStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.GetFeeError.GaslessError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.raiseIllegalStateError
import java.math.BigDecimal

/**
 * Mirror of [GetFeeForGaslessUseCase] with the fee token given rather than chosen. Neither resolver
 * taking the network is an error here: the user asked to pay in a token, there is no native fee to fall
 * back to.
 */
class GetFeeForTokenUseCase(
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val currencyChecksRepository: CurrencyChecksRepository,
    private val evmGaslessFeeResolver: EvmGaslessFeeResolver,
    private val tronGaslessFeeResolver: TronGaslessFeeResolver,
) {

    /**
     * @param sentAmount amount sent by the main transaction, in the sent token. Required for a yield-supply
     * send, whose [TransactionData.Uncompiled.amount] is zeroed.
     */
    suspend operator fun invoke(
        userWallet: UserWallet,
        token: CryptoCurrency,
        transactionData: TransactionData,
        sentAmount: BigDecimal? = null,
    ): Either<GetFeeError, TransactionFeeExtended> {
        return either {
            catch(
                block = {
                    tronGaslessFeeResolver.resolveWithSelectedFeeToken(userWallet.walletId, token, transactionData)
                        ?.let { return@either it.bind() }

                    if (!currencyChecksRepository.isEvmGaslessNetwork(token.network)) {
                        raise(GaslessError.NetworkIsNotSupported)
                    }

                    val accountStatusList = singleAccountStatusListSupplier.getSyncOrNull(userWallet.walletId)
                        ?: raiseIllegalStateError("AccountStatusList is null for ${userWallet.walletId}")

                    val nativeCurrencyStatus = accountStatusList.getCoinStatus(token.network).getOrElse {
                        raiseIllegalStateError("Native currency not found for network ${token.network.id}")
                    }

                    val tokenCurrencyStatus = accountStatusList.getCryptoCurrencyStatus(currency = token).getOrElse {
                        raiseIllegalStateError("Token currency not found for network ${token.network.id}")
                    }

                    evmGaslessFeeResolver.resolveWithSelectedFeeToken(
                        userWallet = userWallet,
                        network = token.network,
                        transactionData = transactionData,
                        nativeCurrencyStatus = nativeCurrencyStatus,
                        tokenCurrencyStatus = tokenCurrencyStatus,
                        sentAmount = sentAmount,
                    ).bind()
                },
                catch = {
                    raise(GaslessError.DataError(it))
                },
            )
        }
    }
}