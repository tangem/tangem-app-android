package com.tangem.domain.staking.model

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.staking.YieldToken
import com.tangem.domain.staking.model.common.RewardClaiming
import com.tangem.domain.staking.model.common.RewardSchedule
import com.tangem.domain.staking.model.common.StakingActionArgs
import com.tangem.domain.staking.model.common.StakingAmountRequirement
import com.tangem.domain.staking.model.stakekit.Yield
import java.math.BigDecimal

/**
 * StakingIntegration implementation for StakeKit.
 * Delegates all calls to the underlying Yield object.
 */
class StakeKitIntegration(
    override val integrationId: StakingIntegrationID,
    private val yield: Yield,
) : StakingIntegration {

    // Basic

    override val token: YieldToken = yield.token

    override val tokens: List<YieldToken> = yield.tokens

    // Targets (validators)

    override val targets: List<StakingTarget> = yield.validators.map { it.toStakingTarget() }

    override val preferredTargets: List<StakingTarget> = yield.preferredValidators.map { it.toStakingTarget() }

    override val areAllTargetsFull: Boolean = yield.allValidatorsFull

    // Enter/Exit Args

    override val isPartialAmountDisabled: Boolean = yield.args.enter.isPartialAmountDisabled

    override val enterMinimumAmount: BigDecimal? =
        yield.args.enter.args[Yield.Args.ArgType.AMOUNT]?.minimum

    override val exitMinimumAmount: BigDecimal? =
        yield.args.exit?.args
            ?.get(Yield.Args.ArgType.AMOUNT)?.minimum

    override val enterArgs: StakingActionArgs = yield.args.enter.toStakingActionArgs()

    override val exitArgs: StakingActionArgs? = yield.args.exit?.toStakingActionArgs()

    // Metadata

    override val warmupPeriodDays: Int = yield.metadata.warmupPeriod.days

    override val cooldownPeriod: CooldownPeriod? = yield.metadata.cooldownPeriod?.days?.let {
        CooldownPeriod.Fixed(it)
    }

    override val rewardSchedule: RewardSchedule = yield.metadata.rewardSchedule.toRewardSchedule()

    override val rewardClaiming: RewardClaiming = yield.metadata.rewardClaiming.toRewardClaiming()

    // Basic

    override fun getCurrentToken(rawCurrencyId: CryptoCurrency.RawID?): YieldToken =
        tokens.firstOrNull { rawCurrencyId?.value == it.coinGeckoId } ?: token

    private fun Yield.Args.Enter.toStakingActionArgs(): StakingActionArgs {
        val amountArg = args[Yield.Args.ArgType.AMOUNT]
        return StakingActionArgs(
            amountRequirement = amountArg?.let { arg ->
                StakingAmountRequirement(
                    isRequired = arg.required,
                    minimum = arg.minimum,
                    maximum = arg.maximum,
                )
            },
            isPartialAmountDisabled = isPartialAmountDisabled,
        )
    }

    private fun Yield.Metadata.RewardSchedule.toRewardSchedule(): RewardSchedule {
        return when (this) {
            Yield.Metadata.RewardSchedule.BLOCK -> RewardSchedule.BLOCK
            Yield.Metadata.RewardSchedule.HOUR -> RewardSchedule.HOUR
            Yield.Metadata.RewardSchedule.DAY -> RewardSchedule.DAY
            Yield.Metadata.RewardSchedule.WEEK -> RewardSchedule.WEEK
            Yield.Metadata.RewardSchedule.MONTH -> RewardSchedule.MONTH
            Yield.Metadata.RewardSchedule.ERA -> RewardSchedule.ERA
            Yield.Metadata.RewardSchedule.EPOCH -> RewardSchedule.EPOCH
            Yield.Metadata.RewardSchedule.UNKNOWN -> RewardSchedule.UNKNOWN
        }
    }

    private fun Yield.Metadata.RewardClaiming.toRewardClaiming(): RewardClaiming {
        return when (this) {
            Yield.Metadata.RewardClaiming.AUTO -> RewardClaiming.AUTO
            Yield.Metadata.RewardClaiming.MANUAL -> RewardClaiming.MANUAL
            Yield.Metadata.RewardClaiming.UNKNOWN -> RewardClaiming.UNKNOWN
        }
    }
}