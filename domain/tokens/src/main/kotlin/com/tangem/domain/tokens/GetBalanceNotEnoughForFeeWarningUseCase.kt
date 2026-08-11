package com.tangem.domain.tokens

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.lib.crypto.BlockchainUtils.isTron
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
    private val dispatchers: CoroutineDispatcherProvider,
    private val currencyChecksRepository: CurrencyChecksRepository,
) {
    suspend operator fun invoke(
        fee: BigDecimal,
        userWalletId: UserWalletId,
        tokenStatus: CryptoCurrencyStatus,
        feeStatus: CryptoCurrencyStatus,
    ): Either<Throwable, CryptoCurrencyWarning?> = Either.catch {
        withContext(dispatchers.io) {
            val feePaidCurrency = currenciesRepository.getFeePaidCurrency(userWalletId, tokenStatus.currency.network)
            val feeTokenBalance = feeStatus.value.amount ?: BigDecimal.ZERO

            val isSendingCurrencyToken = tokenStatus.currency is CryptoCurrency.Token
            val isFeePaidByToken =
                feePaidCurrency is FeePaidCurrency.Token && tokenStatus.currency.id != feePaidCurrency.tokenId

            val warning = when {
                // A Tron token fee currency only ever comes from the gasless path (Tron fees are
                // normally paid in TRX), so it owns the decision and the generic coin-fee rules below
                // don't also fire on it.
                isTronGaslessScenario(feeStatus) -> resolveTronGaslessWarning(
                    fee = fee,
                    tokenStatus = tokenStatus,
                    feeStatus = feeStatus,
                )
                isEvmGaslessTokenEmpty(feeStatus, feeTokenBalance) -> {
                    CryptoCurrencyWarning.BalanceNotEnoughForFee(
                        tokenCurrency = tokenStatus.currency,
                        coinCurrency = feeStatus.currency,
                    )
                }
                feePaidCurrency is FeePaidCurrency.Coin &&
                    isSendingCurrencyToken &&
                    fee > feeTokenBalance -> {
                    CryptoCurrencyWarning.BalanceNotEnoughForFee(
                        tokenCurrency = tokenStatus.currency,
                        coinCurrency = feeStatus.currency,
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

    private fun isTronGaslessScenario(feeStatus: CryptoCurrencyStatus): Boolean {
        return isTron(feeStatus.currency.network.rawId) && feeStatus.currency is CryptoCurrency.Token
    }

    // EVM gasless: the fee token pays for its own transfer; a zero balance is the only notification-level
    // guard (the authoritative check lives in the backend / plan resolver).
    private fun isEvmGaslessTokenEmpty(feeStatus: CryptoCurrencyStatus, feeTokenBalance: BigDecimal): Boolean {
        return currencyChecksRepository.isNetworkSupportedForGaslessTx(feeStatus.currency.network) &&
            feeStatus.currency is CryptoCurrency.Token &&
            feeTokenBalance == BigDecimal.ZERO
    }

    /**
     * Tron gasless: the compensation transfer is paid in the fee token, so its balance must cover it.
     *
     * Only a cross-token fee is checked here. When the fee token IS the sent token, the compensation
     * comes out of the balance being spent, so the amount-subtraction path owns the case — it reduces
     * the amount by the fee (see IsAmountSubtractAvailableUseCase), and a fee that doesn't fit even on
     * its own surfaces as TotalExceedsBalance. Warning here too would just duplicate that.
     *
     * Balance unknown → no warning (the fee still loads).
     */
    private fun resolveTronGaslessWarning(
        fee: BigDecimal,
        tokenStatus: CryptoCurrencyStatus,
        feeStatus: CryptoCurrencyStatus,
    ): CryptoCurrencyWarning? {
        if (feeStatus.currency.id == tokenStatus.currency.id) return null
        val feeTokenBalance = feeStatus.value.amount ?: return null
        return if (fee > feeTokenBalance) {
            CryptoCurrencyWarning.BalanceNotEnoughForFee(
                tokenCurrency = tokenStatus.currency,
                coinCurrency = feeStatus.currency,
            )
        } else {
            null
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
        val tokens = multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
            params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId),
        )
            .orEmpty()

        val token = tokens.find { currency ->
            currency is CryptoCurrency.Token &&
                currency.contractAddress.equals(feePaidToken.contractAddress, ignoreCase = true) &&
                currency.network.derivationPath == tokenStatus.currency.network.derivationPath
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