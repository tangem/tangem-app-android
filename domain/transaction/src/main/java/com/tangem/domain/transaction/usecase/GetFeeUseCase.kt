package com.tangem.domain.transaction.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.math.BigDecimal

/**
 * Use case to get transaction fee
 */
class GetFeeUseCase(
    private val walletManagersFacade: WalletManagersFacade,
    private val dispatcher: CoroutineDispatcherProvider,
) {
    suspend operator fun invoke(
        amount: BigDecimal,
        destination: String,
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): Flow<Either<GetFeeError, TransactionFee>> {
        return flow {
            try {
                val result = requireNotNull(
                    walletManagersFacade.getFee(
                        amount = convertCryptoCurrencyToAmount(cryptoCurrency, amount),
                        destination = destination,
                        userWalletId = userWalletId,
                        network = cryptoCurrency.network,
                    ),
                ) { "Fee is null" }

                val maybeFee = when (result) {
                    is Result.Success -> result.data.right()
                    is Result.Failure -> GetFeeError.DataError(result.error).left()
                }
                emit(maybeFee)
            } catch (e: Exception) {
                emit(GetFeeError.DataError(e.cause).left())
            }
        }.flowOn(dispatcher.io)
    }

    private fun convertCryptoCurrencyToAmount(cryptoCurrency: CryptoCurrency, amount: BigDecimal) = Amount(
        currencySymbol = cryptoCurrency.symbol,
        value = amount,
        decimals = cryptoCurrency.decimals,
        type = when (cryptoCurrency) {
            is CryptoCurrency.Coin -> AmountType.Coin
            is CryptoCurrency.Token -> AmountType.Token(
                token = Token(
                    symbol = cryptoCurrency.symbol,
                    contractAddress = cryptoCurrency.contractAddress,
                    decimals = cryptoCurrency.decimals,
                ),
            )
        },
    )
}