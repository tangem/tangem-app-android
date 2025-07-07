package com.tangem.domain.tokens

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import java.math.BigDecimal

/**
 * Use case for getting balance not enough warning to cover fee.
 *
 * This warning is shown when current currency is not paying fee and paying fee currency balance is not enough
 *
 * Current | Paying fee | Warning
 * Coin    | Coin       | -
 * Token   | Coin       | +
 * Coin    | PToken     | + (VTO - VTHO)
 * Token   | PToken     | + (Other VeChainToken - VTHO)
 * PToken  | PToken     | - (VTHO - VTHO or TerraToken - TerraToken)
 */
class GetBalanceNotEnoughForFeeWarningUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    private val tokensFeatureToggles: TokensFeatureToggles,
    private val dispatchers: CoroutineDispatcherProvider,
) {
    suspend operator fun invoke(
        fee: BigDecimal,
        userWalletId: UserWalletId,
        tokenStatus: CryptoCurrencyStatus,
        coinStatus: CryptoCurrencyStatus,
    ): Either<Throwable, CryptoCurrencyWarning?> = Either.catch {
        withContext(dispatchers.io) {
            val feePaidCurrency = currenciesRepository.getFeePaidCurrency(userWalletId, tokenStatus.currency.network)
            val coinBalance = coinStatus.value.amount ?: BigDecimal.ZERO

            val isFeePaidByCoin = tokenStatus.currency is CryptoCurrency.Token
            val isFeePaidByToken =
                feePaidCurrency is FeePaidCurrency.Token && tokenStatus.currency.id != feePaidCurrency.tokenId

            val warning = when {
                feePaidCurrency is FeePaidCurrency.Coin && isFeePaidByCoin && fee > coinBalance -> {
                    CryptoCurrencyWarning.BalanceNotEnoughForFee(
                        tokenCurrency = tokenStatus.currency,
                        coinCurrency = coinStatus.currency,
                    )
                }
                feePaidCurrency is FeePaidCurrency.Token && isFeePaidByToken && fee > feePaidCurrency.balance -> {
                    constructTokenBalanceNotEnoughWarning(
                        userWalletId = userWalletId,
                        tokenStatus = tokenStatus,
                        feePaidToken = feePaidCurrency,
                    )
                }
                else -> null
            }
            warning
        }
    }

    /**
     * Check if fee paying token [feePaidToken] is added to wallet [userWalletId]
     */
    private suspend fun constructTokenBalanceNotEnoughWarning(
        userWalletId: UserWalletId,
        tokenStatus: CryptoCurrencyStatus,
        feePaidToken: FeePaidCurrency.Token,
    ): CryptoCurrencyWarning {
        val tokens = if (tokensFeatureToggles.isWalletBalanceFetcherEnabled) {
            multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
                params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId),
            )
                .orEmpty()
        } else {
            currenciesRepository.getMultiCurrencyWalletCurrenciesSync(userWalletId)
        }

        val token = tokens.find {
            it is CryptoCurrency.Token &&
                it.contractAddress.equals(feePaidToken.contractAddress, ignoreCase = true) &&
                it.network.derivationPath == tokenStatus.currency.network.derivationPath
        }

        return if (token != null) {
            CryptoCurrencyWarning.CustomTokenNotEnoughForFee(
                currency = tokenStatus.currency,
                feeCurrency = token,
                networkName = token.network.name,
                feeCurrencyName = feePaidToken.name,
                feeCurrencySymbol = feePaidToken.symbol,
            )
        } else {
            CryptoCurrencyWarning.CustomTokenNotEnoughForFee(
                currency = tokenStatus.currency,
                feeCurrency = null,
                networkName = tokenStatus.currency.network.name,
                feeCurrencyName = feePaidToken.name,
                feeCurrencySymbol = feePaidToken.symbol,
            )
        }
    }
}