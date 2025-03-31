package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingEntryInfo
import com.tangem.domain.staking.model.stakekit.RewardBlockType
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.staking.utils.getRewardStakingBalance
import com.tangem.domain.staking.utils.getTotalStakingBalance
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.tokendetails.presentation.tokendetails.model.TokenDetailsClickIntents
import com.tangem.feature.tokendetails.presentation.tokendetails.state.IconState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.StakingBlockUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.features.tokendetails.impl.R
import com.tangem.lib.crypto.BlockchainUtils.isBSC
import com.tangem.lib.crypto.BlockchainUtils.isSolana
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import com.tangem.utils.isNullOrZero
import java.math.BigDecimal

internal class TokenDetailsStakingInfoConverter(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val clickIntents: TokenDetailsClickIntents,
    private val currentState: TokenDetailsState,
    private val stakingEntryInfo: StakingEntryInfo?,
) : Converter<StakingAvailability, TokenDetailsState> {

    override fun convert(value: StakingAvailability): TokenDetailsState {
        return currentState.copy(
            stakingBlocksState = getYieldBalance(cryptoCurrencyStatus, currentState, value),
        )
    }

    private fun getYieldBalance(
        status: CryptoCurrencyStatus,
        state: TokenDetailsState,
        stakingAvailability: StakingAvailability,
    ): StakingBlockUM? {
        return when (stakingAvailability) {
            StakingAvailability.TemporaryUnavailable -> StakingBlockUM.TemporaryUnavailable
            StakingAvailability.Unavailable -> null
            is StakingAvailability.Available -> getStakingInfoBlock(status, state)
        }
    }

    private fun getStakingInfoBlock(status: CryptoCurrencyStatus, state: TokenDetailsState): StakingBlockUM? {
        val yieldBalance = status.value.yieldBalance as? YieldBalance.Data

        val stakingCryptoAmount = yieldBalance?.getTotalStakingBalance(status.currency.network.id.value)
        val pendingBalances = yieldBalance?.balance?.items ?: emptyList()

        val iconState = state.tokenInfoBlockState.iconState

        return when {
            stakingCryptoAmount.isNullOrZero() && stakingEntryInfo != null -> {
                if (pendingBalances.isEmpty()) {
                    getStakeAvailableState(stakingEntryInfo, iconState, isStakingButtonEnabled(status))
                } else {
                    getStakedBlockWithFiatAmount(status, pendingBalances.sumOf { it.amount }, null)
                }
            }
            stakingCryptoAmount.isNullOrZero() && stakingEntryInfo == null -> {
                null
            }
            else -> getStakedBlockWithFiatAmount(status, stakingCryptoAmount, yieldBalance?.getRewardStakingBalance())
        }
    }

    private fun isStakingButtonEnabled(status: CryptoCurrencyStatus): Boolean {
        return status.value is CryptoCurrencyStatus.Loaded ||
            status.value is CryptoCurrencyStatus.NoQuote ||
            status.value is CryptoCurrencyStatus.Custom
    }

    private fun getStakedBlockWithFiatAmount(
        status: CryptoCurrencyStatus,
        stakingAmount: BigDecimal?,
        rewardAmount: BigDecimal?,
    ): StakingBlockUM.Staked {
        val fiatRate = status.value.fiatRate
        return getStakedState(
            status = status,
            stakingCryptoAmount = stakingAmount,
            stakingFiatAmount = stakingAmount?.let { fiatRate?.multiply(it) },
            stakingRewardAmount = rewardAmount?.let { fiatRate?.multiply(it) },
        )
    }

    private fun getStakeAvailableState(
        stakingEntryInfo: StakingEntryInfo,
        iconState: IconState,
        isEnabled: Boolean,
    ): StakingBlockUM.StakeAvailable {
        val apr = stakingEntryInfo.apr.format { percent() }
        return StakingBlockUM.StakeAvailable(
            titleText = resourceReference(
                id = R.string.token_details_staking_block_title,
                formatArgs = wrappedList(apr),
            ),
            subtitleText = resourceReference(
                id = R.string.staking_notification_earn_rewards_text,
                formatArgs = wrappedList(stakingEntryInfo.tokenSymbol),
            ),
            iconState = iconState,
            isEnabled = isEnabled,
            onStakeClicked = clickIntents::onStakeBannerClick,
        )
    }

    private fun getStakedState(
        status: CryptoCurrencyStatus,
        stakingCryptoAmount: BigDecimal?,
        stakingFiatAmount: BigDecimal?,
        stakingRewardAmount: BigDecimal?,
    ): StakingBlockUM.Staked {
        return StakingBlockUM.Staked(
            cryptoAmount = stakingCryptoAmount,
            fiatAmount = stakingFiatAmount,
            cryptoValue = stringReference(
                stakingCryptoAmount.format {
                    crypto(
                        symbol = status.currency.symbol,
                        decimals = status.currency.decimals,
                    )
                },
            ),
            fiatValue = stringReference(
                stakingFiatAmount.format {
                    fiat(
                        appCurrencyProvider().code,
                        appCurrencyProvider().symbol,
                    )
                },
            ),
            rewardValue = getRewardText(status, stakingRewardAmount),
            onStakeClicked = clickIntents::onStakeBannerClick,
        )
    }

    private fun getRewardText(status: CryptoCurrencyStatus, stakingRewardAmount: BigDecimal?): TextReference {
        val blockchainId = status.currency.network.id.value
        val rewardBlockType = when {
            isSolana(blockchainId) || isBSC(blockchainId) -> RewardBlockType.RewardUnavailable
            stakingRewardAmount.isNullOrZero() -> RewardBlockType.NoRewards
            else -> RewardBlockType.Rewards
        }

        return when (rewardBlockType) {
            RewardBlockType.NoRewards -> resourceReference(R.string.staking_details_no_rewards_to_claim)
            RewardBlockType.RewardUnavailable -> TextReference.EMPTY
            RewardBlockType.RewardsRequirementsError,
            RewardBlockType.Rewards,
            -> resourceReference(
                R.string.staking_details_rewards_to_claim,
                wrappedList(
                    stakingRewardAmount.format {
                        fiat(
                            appCurrencyProvider().code,
                            appCurrencyProvider().symbol,
                        )
                    },
                ),
            )
        }
    }
}