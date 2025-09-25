package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.model.remove.RemoveCurrencyError
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.walletmanager.WalletManagersFacade

class RemoveCurrencyUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val walletManagersFacade: WalletManagersFacade,
    private val multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
    ): Either<RemoveCurrencyError, Unit> {
        return either {
            if (hasLinkedTokens(userWalletId, currency)) {
                raise(RemoveCurrencyError.HasLinkedTokens)
            }

            catch(
                block = {
                    currenciesRepository.removeCurrency(userWalletId, currency)

                    when (currency) {
                        is CryptoCurrency.Coin -> {
                            walletManagersFacade.remove(userWalletId, setOf(currency.network))
                        }
                        is CryptoCurrency.Token -> {
                            walletManagersFacade.removeTokens(userWalletId, setOf(currency))
                        }
                    }
                },
                catch = { raise(RemoveCurrencyError.DataError(it)) },
            )
        }
    }

    suspend fun hasLinkedTokens(userWalletId: UserWalletId, currency: CryptoCurrency): Boolean {
        return when (currency) {
            is CryptoCurrency.Coin -> {
                val walletCurrencies = multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
                    params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId),
                )
                    .orEmpty()

                walletCurrencies.any { it is CryptoCurrency.Token && it.network == currency.network }
            }
            is CryptoCurrency.Token -> false
        }
    }
}