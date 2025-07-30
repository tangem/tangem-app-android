package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.staking.YieldBalance
import com.tangem.domain.staking.utils.getTotalWithRewardsStakingBalance
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
            val cryptoCurrencyStatus = cryptoCurrencyStatusProvider() ?: return this

            if (stakingBlocksState !is StakingBlockUM.Staked &&
                stakingBlocksState !is StakingBlockUM.TemporaryUnavailable
            ) {
                return this
            }

            val yieldBalance = cryptoCurrencyStatus.value.yieldBalance as? YieldBalance.Data
            val stakingCryptoAmount = yieldBalance?.getTotalWithRewardsStakingBalance(
                cryptoCurrencyStatus.currency.network.rawId,
            )
            val stakingFiatAmount = stakingCryptoAmount?.let { cryptoCurrencyStatus.value.fiatRate?.multiply(it) }
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
        val fiatAmount = status.fiatAmount?.getBalance(selectedBalanceType, stakingFiatAmount)

        return fiatAmount.format {
            fiat(
                fiatCurrencyCode = appCurrency.code,
                fiatCurrencySymbol = appCurrency.symbol,
            )
        }
    }

    private fun formatCryptoAmount(
        status: CryptoCurrencyStatus,
        stakingCryptoAmount: BigDecimal?,
        selectedBalanceType: BalanceType,
    ): String {
        val amount = status.value.amount?.getBalance(selectedBalanceType, stakingCryptoAmount)

        return amount.format { crypto(status.currency) }
    }
}