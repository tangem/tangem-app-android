package com.tangem.features.yield.supply.impl.incentives.claim.model

import com.tangem.common.ui.extensions.iconResId
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.getTintForTokenIcon
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.tryGetBackgroundForTokenIcon
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.yield.supply.models.incentives.YieldReward
import com.tangem.domain.yield.supply.models.incentives.YieldRewardDestination
import com.tangem.domain.yield.supply.models.incentives.YieldRewardsSummary
import com.tangem.features.yield.supply.impl.common.formatter.formatAmount
import com.tangem.features.yield.supply.impl.common.formatter.formatServiceFeeRate
import com.tangem.features.yield.supply.impl.common.formatter.formatUsd
import com.tangem.features.yield.supply.impl.incentives.claim.entity.YieldSupplyClaimUM
import com.tangem.utils.StringsSigns.NON_BREAKING_SPACE
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal
import java.math.BigInteger
import com.tangem.core.res.R as CoreResR

/**
 * Rewards a claim would pay out, as the sheet needs them: what it can route, what it cannot, and the net total.
 *
 * @property moduleAddress module the rewards belong to, used to pin the claim destination before signing
 * @property supportedRewardTokens every reward address the claim gate admits a payload for
 */
internal data class YieldSupplyClaimRewards(
    val moduleAddress: String,
    val claimable: List<YieldReward>,
    val unsupported: List<YieldReward>,
    val supportedRewardTokens: Set<String>,
    val totalNetUsd: BigDecimal,
)

/** Summaries without a claim behind them have nothing for the sheet to show. */
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

/** Rows and groups of the claim sheet: what each reward pays out and where it lands. */
internal class YieldSupplyClaimRewardsMapper(private val cryptoCurrency: CryptoCurrency) {

    /** @param storage named only on the owner group — the other destinations do not land in an account of the user's. */
    fun toGroups(
        rewards: YieldSupplyClaimRewards,
        storage: YieldSupplyClaimUM.StorageUM?,
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

    /**
     * The rate rides in the value, as "(15%) $1.40". It can only be stated for the claim as a whole when every
     * reward in it shares one — the backend reports the rate per reward — so a mixed claim shows the amount alone.
     */
    fun serviceFeeValue(rewards: List<YieldReward>, amountUsd: BigDecimal): TextReference {
        val amount = amountUsd.formatUsd()
        val rate = rewards.map(YieldReward::serviceFeeRateBps).distinct().singleOrNull()
            ?: return stringReference(amount)

        return stringReference("(${rate.formatServiceFeeRate()})$NON_BREAKING_SPACE$amount")
    }

    private fun group(
        titleResId: Int,
        rewards: List<YieldSupplyClaimUM.RewardUM>,
        storage: YieldSupplyClaimUM.StorageUM? = null,
    ) = YieldSupplyClaimUM.RewardGroupUM(
        title = resourceReference(titleResId),
        rewards = rewards.toPersistentList(),
        storage = storage,
    )

    private fun netRow(reward: YieldReward) = reward.toRow(
        amount = reward.estimatedNetAmount(),
        fiatAmount = stringReference(reward.estimatedNetValueUsd.formatUsd()),
    )

    /** The gross amount and no USD: this version cannot route the reward, so it promises no payout for it. */
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

    /** `floor(claimable × (10_000 − serviceFeeRateBps) / 10_000)`, kept in base units so nothing is lost on the way. */
    private fun YieldReward.estimatedNetAmount(): BigInteger {
        val whole = BigInteger.valueOf(BPS_IN_WHOLE)
        val rate = BigInteger.valueOf(serviceFeeRateBps.toLong().coerceIn(0L, BPS_IN_WHOLE))

        return claimableAmount * (whole - rate) / whole
    }

    private fun YieldRewardDestination.titleResId(): Int = when (this) {
        YieldRewardDestination.Owner -> CoreResR.string.yield_module_claim_destination_owner_title
        YieldRewardDestination.Aave -> CoreResR.string.yield_module_claim_destination_aave_title
        YieldRewardDestination.Module -> CoreResR.string.yield_module_claim_destination_module_title
        YieldRewardDestination.Unsupported -> CoreResR.string.yield_module_claim_destination_unsupported_title
    }

    private companion object {
        const val BPS_IN_WHOLE = 10_000L

        val CLAIMABLE_DESTINATION_ORDER = listOf(
            YieldRewardDestination.Owner,
            YieldRewardDestination.Aave,
            YieldRewardDestination.Module,
        )
    }
}