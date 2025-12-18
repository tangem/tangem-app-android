package com.tangem.domain.staking.model

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.staking.YieldToken
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

    override val enterArgs: Yield.Args.Enter = yield.args.enter

    override val exitArgs: Yield.Args.Enter? = yield.args.exit

    // Metadata

    override val warmupPeriodDays: Int = yield.metadata.warmupPeriod.days

    override val cooldownPeriodDays: Int? = yield.metadata.cooldownPeriod?.days

    override val rewardSchedule: Yield.Metadata.RewardSchedule = yield.metadata.rewardSchedule

    override val rewardClaiming: Yield.Metadata.RewardClaiming = yield.metadata.rewardClaiming

    // Basic

    override fun getCurrentToken(rawCurrencyId: CryptoCurrency.RawID?): YieldToken =
        tokens.firstOrNull { rawCurrencyId?.value == it.coinGeckoId } ?: token
}