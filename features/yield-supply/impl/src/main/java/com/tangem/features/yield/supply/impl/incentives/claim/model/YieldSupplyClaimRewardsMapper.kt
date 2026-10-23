package com.tangem.features.yield.supply.impl.incentives.claim.model

import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.extensions.iconResId
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.getTintForTokenIcon
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.tryGetBackgroundForTokenIcon
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.yield.supply.models.incentives.YieldReward
import com.tangem.domain.yield.supply.models.incentives.YieldRewardDestination
import com.tangem.domain.yield.supply.models.incentives.YieldRewardsSummary
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.common.formatter.formatAmount
import com.tangem.features.yield.supply.impl.common.formatter.formatUsd
import com.tangem.features.yield.supply.impl.incentives.claim.entity.YieldSupplyClaimUM
import com.tangem.utils.StringsSigns.NON_BREAKING_SPACE
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal
import java.math.BigInteger

/**
 * @property moduleAddress pins the claim destination during calldata validation
 * @property supportedRewardTokens reward tokens the calldata validation accepts
 */
internal data class YieldSupplyClaimRewards(
    val moduleAddress: String,
    val claimable: List<YieldReward>,
    val unsupported: List<YieldReward>,
    val supportedRewardTokens: Set<String>,
    val totalNetUsd: BigDecimal,
)

internal fun YieldRewardsSummary.toClaimRewards(): YieldSupplyClaimRewards? = when (this) {
    is YieldRewardsSummary.Claimable -> YieldSupplyClaimRewards(
        moduleAddress = moduleAddress,
        claimable = claimable,
        unsupported = unsupported,
        supportedRewardTokens = supportedRewardTokens,
        totalNetUsd = totalNetUsd,
    )
    is YieldRewardsSummary.Deferred -> YieldSupplyClaimRewards(
        moduleAddress = moduleAddress,
        claimable = claimable,
        unsupported = unsupported,
        supportedRewardTokens = supportedRewardTokens,
        totalNetUsd = totalNetUsd,
    )
    is YieldRewardsSummary.None, is YieldRewardsSummary.PendingOnly -> null
}

internal class YieldSupplyClaimRewardsMapper(private val cryptoCurrency: CryptoCurrency) {

    fun toGroups(
        rewards: YieldSupplyClaimRewards,
        storage: AccountTitleUM.Account?,
    ): ImmutableList<YieldSupplyClaimUM.RewardGroupUM> = buildList {
        CLAIMABLE_DESTINATION_ORDER.forEach { destination ->
            val destinationRewards = rewards.claimable.filter { it.finalDestination == destination }
            if (destinationRewards.isNotEmpty()) {
                add(
                    group(
                        titleResId = destination.titleResId(),
                        rewards = destinationRewards.map(::netRow),
                        storage = storage.takeIf { destination == YieldRewardDestination.Owner },
                    ),
                )
            }
        }

        val unroutable = rewards.unsupported.filter { it.claimableAmount > BigInteger.ZERO }
        if (unroutable.isNotEmpty()) {
            add(group(YieldRewardDestination.Unsupported.titleResId(), unroutable.map(::grossRow)))
        }
    }.toPersistentList()

    /** Shows the rate, as "(15%) $1.40", only when all rewards share it: the backend reports the rate per reward. */
    fun serviceFeeValue(rewards: List<YieldReward>, amountUsd: BigDecimal): TextReference {
        val amount = amountUsd.formatUsd()
        val rate = rewards.map(YieldReward::serviceFeeRate).distinct().singleOrNull() ?: return amount
        val formattedRate = rate.format { percent(minFractionDigits = 0) }

        return combinedReference(stringReference("($formattedRate)$NON_BREAKING_SPACE"), amount)
    }

    private fun group(
        titleResId: Int,
        rewards: List<YieldSupplyClaimUM.RewardUM>,
        storage: AccountTitleUM.Account? = null,
    ) = YieldSupplyClaimUM.RewardGroupUM(
        title = resourceReference(titleResId),
        rewards = rewards.toPersistentList(),
        storage = storage,
    )

    private fun netRow(reward: YieldReward) = reward.toRow(
        amount = reward.estimatedNetAmount,
        fiatAmount = reward.estimatedNetValueUsd.formatUsd(),
    )

    /** This version cannot route the reward, so neither a net amount nor a USD value is promised for it. */
    private fun grossRow(reward: YieldReward) = reward.toRow(amount = reward.claimableAmount, fiatAmount = null)

    private fun YieldReward.toRow(amount: BigInteger, fiatAmount: TextReference?): YieldSupplyClaimUM.RewardUM {
        val background = tryGetBackgroundForTokenIcon(contractAddress = rewardToken)

        return YieldSupplyClaimUM.RewardUM(
            icon = CurrencyIconState.TokenIcon(
                url = null,
                topBadgeIconResId = cryptoCurrency.network.iconResId,
                isGrayscale = false,
                shouldShowCustomBadge = false,
                fallbackTint = getTintForTokenIcon(background),
                fallbackBackground = background,
            ),
            symbol = symbol,
            networkName = cryptoCurrency.network.name,
            amount = stringReference(formatAmount(amount)),
            fiatAmount = fiatAmount,
        )
    }

    private fun YieldRewardDestination.titleResId(): Int = when (this) {
        YieldRewardDestination.Owner -> R.string.yield_module_claim_destination_owner_title
        YieldRewardDestination.Aave -> R.string.yield_module_claim_destination_aave_title
        YieldRewardDestination.Module -> R.string.yield_module_claim_destination_module_title
        YieldRewardDestination.Unsupported -> R.string.yield_module_claim_destination_unsupported_title
    }

    private companion object {
        val CLAIMABLE_DESTINATION_ORDER = listOf(
            YieldRewardDestination.Owner,
            YieldRewardDestination.Aave,
            YieldRewardDestination.Module,
        )
    }
}