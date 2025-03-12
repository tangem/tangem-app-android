package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.tokendetails.presentation.tokendetails.state.*
import com.tangem.feature.tokendetails.presentation.tokendetails.state.utils.getBalance
import com.tangem.lib.crypto.BlockchainUtils
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
            val stakingCryptoAmount = yieldBalance?.getTotalWithRewardsStakingBalance()
            val stakingFiatAmount = stakingCryptoAmount?.let { cryptoCurrencyStatus.value.fiatRate?.multiply(it) }
            val includeStakingTotalBalance =
                BlockchainUtils.isIncludeStakingTotalBalance(cryptoCurrencyStatus.currency.network.id.value)
            copy(
                tokenBalanceBlockState = if (tokenBalanceBlockState is TokenDetailsBalanceBlockState.Content) {
                    tokenBalanceBlockState.copy(
                        selectedBalanceType = value.type,
                        displayFiatBalance = formatFiatAmount(
                            status = cryptoCurrencyStatus.value,
                            stakingFiatAmount = stakingFiatAmount,
                            selectedBalanceType = value.type,
                            appCurrency = appCurrencyProvider(),
                            includeStaking = includeStakingTotalBalance,
                        ),
                        displayCryptoBalance = formatCryptoAmount(
                            status = cryptoCurrencyStatus,
                            stakingCryptoAmount = stakingCryptoAmount,
                            selectedBalanceType = value.type,
                            includeStaking = includeStakingTotalBalance,
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
        includeStaking: Boolean,
    ): String {
        val fiatAmount = status.fiatAmount?.getBalance(selectedBalanceType, stakingFiatAmount, includeStaking)

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
        includeStaking: Boolean,
    ): String {
        val amount = status.value.amount?.getBalance(selectedBalanceType, stakingCryptoAmount, includeStaking)

        return amount.format { crypto(status.currency) }
    }
}