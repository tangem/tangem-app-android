package com.tangem.domain.transaction.usecase

import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.extensions.Result
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId
import java.math.BigDecimal

/**
 * Use case to get transaction fee
 */
class GetFeeUseCase(
    private val walletManagersFacade: WalletManagersFacade,
) {
    suspend operator fun invoke(
        amount: BigDecimal,
        destination: String,
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ) = either {
        catch(
            block = {
                val result = requireNotNull(
                    walletManagersFacade.getFee(
                        amount = convertCryptoCurrencyToAmount(cryptoCurrency, amount),
                        destination = destination,
                        userWalletId = userWalletId,
                        network = cryptoCurrency.network,
                    ),
                ) { "Fee is null" }

                val maybeFee = when (result) {
                    is Result.Success -> result.data
                    is Result.Failure -> raise(GetFeeError.DataError(result.error))
                }
                maybeFee
            },
            catch = {
                raise(GetFeeError.DataError(it))
            },
        )
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