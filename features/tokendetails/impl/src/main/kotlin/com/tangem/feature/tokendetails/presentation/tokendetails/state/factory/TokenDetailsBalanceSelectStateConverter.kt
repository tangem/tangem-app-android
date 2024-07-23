package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.tokendetails.presentation.tokendetails.state.*
import com.tangem.feature.tokendetails.presentation.tokendetails.state.utils.getBalance
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal class TokenDetailsBalanceSelectStateConverter(
    private val currentStateProvider: Provider<TokenDetailsState>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus?>,
) : Converter<TokenBalanceSegmentedButtonConfig, TokenDetailsState> {

    override fun convert(value: TokenBalanceSegmentedButtonConfig): TokenDetailsState {
        return with(currentStateProvider()) {
            if (stakingBlocksState !is StakingBlockUM.Staked) return this
            val cryptoCurrencyStatus = cryptoCurrencyStatusProvider() ?: return this

            val stakingCryptoAmount = stakingBlocksState.cryptoAmount
            val stakingFiatAmount = stakingBlocksState.fiatAmount

            copy(
                tokenBalanceBlockState = if (tokenBalanceBlockState is TokenDetailsBalanceBlockState.Content) {
                    tokenBalanceBlockState.copy(
                        selectedBalanceType = value.type,
                        displayFiatBalance = formatFiatAmount(
                            status = cryptoCurrencyStatus.value,
                            stakingFiatAmount = stakingFiatAmount,
                            selectedBalanceType = value.type,
                            appCurrency = appCurrencyProvider(),
                        ),
                        displayCryptoBalance = formatCryptoAmount(
                            status = cryptoCurrencyStatus,
                            stakingCryptoAmount = stakingCryptoAmount,
                            selectedBalanceType = value.type,
                        ),
                    )
                } else {
                    tokenBalanceBlockState
                },
            )
        }
    }

    private fun formatFiatAmount(
        status: CryptoCurrencyStatus.Value,
        stakingFiatAmount: BigDecimal?,
        selectedBalanceType: BalanceType,
        appCurrency: AppCurrency,
    ): String {
        val fiatAmount = status.fiatAmount ?: return BigDecimalFormatter.EMPTY_BALANCE_SIGN
        val totalAmount = fiatAmount.getBalance(selectedBalanceType, stakingFiatAmount)

        return BigDecimalFormatter.formatFiatAmount(
            fiatAmount = totalAmount,
            fiatCurrencyCode = appCurrency.code,
            fiatCurrencySymbol = appCurrency.symbol,
        )
    }

    private fun formatCryptoAmount(
        status: CryptoCurrencyStatus,
        stakingCryptoAmount: BigDecimal?,
        selectedBalanceType: BalanceType,
    ): String {
        val amount = status.value.amount ?: return BigDecimalFormatter.EMPTY_BALANCE_SIGN
        val totalAmount = amount.getBalance(selectedBalanceType, stakingCryptoAmount)

        return BigDecimalFormatter.formatCryptoAmount(totalAmount, status.currency.symbol, status.currency.decimals)
    }
}