package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import androidx.compose.ui.text.SpanStyle
import com.tangem.common.getRewardStakingBalance
import com.tangem.common.getTotalStakingBalance
import com.tangem.common.ui.earn.EarnBlockUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.defaultAmount
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.formatStyled
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.staking.RewardBlockType
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingEntryInfo
import com.tangem.domain.staking.model.StakingOption
import com.tangem.feature.tokendetails.presentation.tokendetails.model.TokenDetailsClickIntents
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsUM
import com.tangem.features.tokendetails.impl.R
import com.tangem.lib.crypto.BlockchainUtils.isStakingRewardUnavailable
import com.tangem.utils.isNullOrZero
import com.tangem.utils.transformer.Transformer
import java.math.BigDecimal
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.R as CoreUiR

internal class UpdateStakingNotificationTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val stakingAvailability: StakingAvailability,
    private val stakingEntryInfo: StakingEntryInfo?,
    private val appCurrency: AppCurrency,
    private val clickIntents: TokenDetailsClickIntents,
) : Transformer<TokenDetailsUM> {

    override fun transform(prevState: TokenDetailsUM): TokenDetailsUM {
        return prevState.copy(earnBlockState = buildEarnBlock(prevState.isBalanceHidden))
    }

    private fun buildEarnBlock(isBalanceHidden: Boolean): EarnBlockUM? {
        return when (val availability = stakingAvailability) {
            StakingAvailability.TemporaryUnavailable -> buildTemporaryUnavailable()
            StakingAvailability.Unavailable -> null
            is StakingAvailability.Available -> getStakingInfoBlock(availability, isBalanceHidden)
        }
    }

    private fun buildTemporaryUnavailable(): EarnBlockUM.Content {
        return EarnBlockUM.Content(
            type = EarnBlockUM.Type.Staking,
            backgroundUM = EarnBlockUM.BackgroundUM.Surface,
            iconUM = EarnBlockUM.IconUM.Plain(iconRes = CoreUiR.drawable.ic_staking_disable_40),
            titleUM = EarnBlockUM.TitleUM(
                text = resourceReference(CoreResR.string.staking_native),
                style = EarnBlockUM.TitleUM.Style.Large,
                tone = EarnBlockUM.TitleUM.Tone.Disabled,
            ),
            subtitleUM = EarnBlockUM.SubtitleUM.Text(
                text = resourceReference(CoreResR.string.staking_notification_network_error_text),
                style = EarnBlockUM.SubtitleUM.Style.Small,
                tone = EarnBlockUM.SubtitleUM.Tone.Disabled,
            ),
            trailingUM = null,
        )
    }

    private fun getStakingInfoBlock(
        availability: StakingAvailability.Available,
        isBalanceHidden: Boolean,
    ): EarnBlockUM? {
        val status = cryptoCurrencyStatus
        val stakingBalance = status.value.stakingBalance as? StakingBalance.Data
        val stakingCryptoAmount = stakingBalance?.getTotalStakingBalance(status.currency.network.rawId)

        return when {
            stakingCryptoAmount.isNullOrZero() && stakingEntryInfo != null -> {
                val hasPendingBalances = stakingBalance.hasPendingBalances()
                if (!hasPendingBalances) {
                    buildStakeAvailable(
                        availability = availability,
                        isEnabled = isStakingButtonEnabled(status),
                    )
                } else {
                    buildActiveBlock(
                        stakingAmount = stakingBalance.getPendingAmount(),
                        rewardAmount = null,
                        isBalanceHidden = isBalanceHidden,
                    )
                }
            }
            stakingCryptoAmount.isNullOrZero() && stakingEntryInfo == null -> null
            else -> buildActiveBlock(
                stakingAmount = stakingCryptoAmount,
                rewardAmount = stakingBalance.getRewardAmount(),
                isBalanceHidden = isBalanceHidden,
            )
        }
    }

    private fun isStakingButtonEnabled(status: CryptoCurrencyStatus): Boolean {
        return status.value is CryptoCurrencyStatus.Loaded ||
            status.value is CryptoCurrencyStatus.NoQuote ||
            status.value is CryptoCurrencyStatus.Custom
    }

    private fun buildStakeAvailable(
        availability: StakingAvailability.Available,
        isEnabled: Boolean,
    ): EarnBlockUM.Content {
        return EarnBlockUM.Content(
            type = EarnBlockUM.Type.Staking,
            backgroundUM = EarnBlockUM.BackgroundUM.AccentSoft,
            iconUM = EarnBlockUM.IconUM.Glowing(iconRes = CoreUiR.drawable.ic_staking_40),
            titleUM = EarnBlockUM.TitleUM(
                text = resourceReference(id = R.string.token_details_staking_block_title),
                style = EarnBlockUM.TitleUM.Style.Small,
                tone = EarnBlockUM.TitleUM.Tone.Accent,
            ),
            subtitleUM = EarnBlockUM.SubtitleUM.Text(
                text = stakeAvailableSubtitle(availability.option.displayApy),
                style = EarnBlockUM.SubtitleUM.Style.Large,
                tone = EarnBlockUM.SubtitleUM.Tone.Primary,
            ),
            trailingUM = EarnBlockUM.TrailingUM.Button(
                text = resourceReference(R.string.common_stake),
                isEnabled = isEnabled,
            ),
            onClick = clickIntents::onStakeBannerClick,
        )
    }

    private fun stakeAvailableSubtitle(apy: BigDecimal?): TextReference {
        return if (apy != null) {
            resourceReference(
                CoreResR.string.token_details_earn_staking_subtitle,
                wrappedList(apy.format { percent() }),
            )
        } else {
            resourceReference(CoreResR.string.staking_notification_earn_rewards_text)
        }
    }

    private fun buildActiveBlock(
        stakingAmount: BigDecimal?,
        rewardAmount: BigDecimal?,
        isBalanceHidden: Boolean,
    ): EarnBlockUM.Content {
        val status = cryptoCurrencyStatus
        val fiatRate = status.value.fiatRate
        val fiatAmount = stakingAmount?.let { fiatRate?.multiply(it) }
        val rewardFiatAmount = rewardAmount?.let { fiatRate?.multiply(it) }
        return EarnBlockUM.Content(
            type = EarnBlockUM.Type.Staking,
            backgroundUM = EarnBlockUM.BackgroundUM.Surface,
            iconUM = EarnBlockUM.IconUM.Glowing(iconRes = CoreUiR.drawable.ic_staking_40),
            titleUM = EarnBlockUM.TitleUM(
                text = resourceReference(CoreResR.string.staking_native),
                style = EarnBlockUM.TitleUM.Style.Large,
                tone = EarnBlockUM.TitleUM.Tone.Primary,
            ),
            subtitleUM = getRewardSubtitle(status, rewardFiatAmount),
            trailingUM = EarnBlockUM.TrailingUM.Balance(
                fiatValue = fiatAmount.formatStyled {
                    fiat(
                        fiatCurrencyCode = appCurrency.code,
                        fiatCurrencySymbol = appCurrency.symbol,
                        spanStyleReference = { SpanStyle(color = TangemTheme.colors2.text.neutral.secondary) },
                    ).defaultAmount(
                        spanStyleReference = { SpanStyle(color = TangemTheme.colors2.text.neutral.secondary) },
                    )
                },
                cryptoValue = stringReference(
                    stakingAmount.format {
                        crypto(
                            symbol = status.currency.symbol,
                            decimals = status.currency.decimals,
                        )
                    },
                ),
                isBalanceHidden = isBalanceHidden,
            ),
            onClick = clickIntents::onStakeBannerClick,
        )
    }

    private fun getRewardSubtitle(
        status: CryptoCurrencyStatus,
        stakingRewardAmount: BigDecimal?,
    ): EarnBlockUM.SubtitleUM? {
        val blockchainId = status.currency.network.rawId
        val isCoin = status.currency.id.isCoin
        val stakingBalance = status.value.stakingBalance

        val rewardBlockType = when {
            stakingBalance is StakingBalance.Data.P2PEthPool -> {
                if (stakingBalance.totalRewards.isNullOrZero()) {
                    RewardBlockType.RewardUnavailable.DefaultRewardUnavailable
                } else {
                    RewardBlockType.EthereumEarnedRewards
                }
            }
            isStakingRewardUnavailable(blockchainId, isCoin) -> {
                RewardBlockType.RewardUnavailable.DefaultRewardUnavailable
            }
            stakingRewardAmount.isNullOrZero() -> RewardBlockType.NoRewards
            else -> RewardBlockType.Rewards
        }

        val text = when (rewardBlockType) {
            RewardBlockType.NoRewards -> resourceReference(R.string.staking_details_no_rewards_to_claim)
            RewardBlockType.CardanoNoRewards -> resourceReference(R.string.staking_cardano_details_rewards_info_text)
            RewardBlockType.RewardUnavailable.DefaultRewardUnavailable,
            RewardBlockType.RewardUnavailable.SolanaRewardUnavailable,
            -> return null
            RewardBlockType.EthereumEarnedRewards -> {
                val cryptoRewardAmount = (stakingBalance as? StakingBalance.Data.P2PEthPool)?.totalRewards
                resourceReference(
                    R.string.staking_details_autocompound_rewards_earned,
                    wrappedList(
                        cryptoRewardAmount.format {
                            crypto(
                                symbol = status.currency.symbol,
                                decimals = status.currency.decimals,
                            )
                        },
                    ),
                )
            }
            RewardBlockType.RewardsRequirementsError,
            RewardBlockType.Rewards,
            -> resourceReference(
                R.string.staking_details_rewards_to_claim,
                wrappedList(
                    stakingRewardAmount.format { fiat(appCurrency.code, appCurrency.symbol) },
                ),
            )
        }

        val isAccent = rewardBlockType == RewardBlockType.Rewards ||
            rewardBlockType == RewardBlockType.RewardsRequirementsError ||
            rewardBlockType == RewardBlockType.EthereumEarnedRewards

        return EarnBlockUM.SubtitleUM.Text(
            text = text,
            style = EarnBlockUM.SubtitleUM.Style.Small,
            tone = if (isAccent) EarnBlockUM.SubtitleUM.Tone.Accent else EarnBlockUM.SubtitleUM.Tone.Disabled,
        )
    }
}

private fun StakingBalance.Data?.hasPendingBalances(): Boolean = when (this) {
    is StakingBalance.Data.StakeKit -> balance.items.isNotEmpty()
    is StakingBalance.Data.P2PEthPool -> !unstakingAmount.isNullOrZero()
    null -> false
}

private fun StakingBalance.Data?.getPendingAmount(): BigDecimal = when (this) {
    is StakingBalance.Data.StakeKit -> balance.items.sumOf { it.amount }
    is StakingBalance.Data.P2PEthPool -> unstakingAmount
    null -> BigDecimal.ZERO
}

private fun StakingBalance.Data?.getRewardAmount(): BigDecimal = when (this) {
    is StakingBalance.Data.StakeKit -> getRewardStakingBalance()
    is StakingBalance.Data.P2PEthPool -> totalRewards
    null -> BigDecimal.ZERO
}

private val StakingOption.displayApy: BigDecimal?
    get() = when (this) {
        is StakingOption.StakeKit -> yield.preferredValidators
            .mapNotNull { it.rewardInfo?.rate }
            .maxOrNull()
        is StakingOption.P2PEthPool -> apy
    }