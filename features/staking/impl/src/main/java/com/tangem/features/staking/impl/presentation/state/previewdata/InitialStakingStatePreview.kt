package com.tangem.features.staking.impl.presentation.state.previewdata

import com.tangem.core.ui.components.list.RoundedListWithDividersItemData
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.*
import kotlinx.collections.immutable.persistentListOf

internal object InitialStakingStatePreview {
    val defaultState = StakingStates.InitialInfoState.Data(
        isPrimaryButtonEnabled = true,
        aprRange = stringReference("2.54-5.12%"),
        infoItems = persistentListOf(
            RoundedListWithDividersItemData(
                id = R.string.staking_details_available,
                startText = TextReference.Res(R.string.staking_details_available),
                endText = TextReference.Str("15 SOL"),
            ),
            RoundedListWithDividersItemData(
                id = R.string.staking_details_apy,
                startText = TextReference.Res(R.string.staking_details_apy),
                endText = TextReference.Str("2.54-5.12%"),
            ),
            RoundedListWithDividersItemData(
                id = R.string.staking_details_on_stake,
                startText = TextReference.Res(R.string.staking_details_on_stake),
                endText = TextReference.Str("0 SOL"),
            ),
            RoundedListWithDividersItemData(
                id = R.string.staking_details_unbonding_period,
                startText = TextReference.Res(R.string.staking_details_unbonding_period),
                endText = TextReference.Str("3d"),
            ),
            RoundedListWithDividersItemData(
                id = R.string.staking_details_minimum_requirement,
                startText = TextReference.Res(R.string.staking_details_minimum_requirement),
                endText = TextReference.Str("12 SOL"),
            ),
            RoundedListWithDividersItemData(
                id = R.string.staking_details_reward_claiming,
                startText = TextReference.Res(R.string.staking_details_reward_claiming),
                endText = TextReference.Str("Auto"),
            ),
            RoundedListWithDividersItemData(
                id = R.string.staking_details_warmup_period,
                startText = TextReference.Res(R.string.staking_details_warmup_period),
                endText = TextReference.Str("Days"),
            ),
            RoundedListWithDividersItemData(
                id = R.string.staking_details_reward_schedule,
                startText = TextReference.Res(R.string.staking_details_reward_schedule),
                endText = TextReference.Str("Block"),
            ),
        ),
        onInfoClick = {},
        yieldBalance = InnerYieldBalanceState.Empty,
        isStakeMoreAvailable = true,
    )

    val stateWithYield = defaultState.copy(
        yieldBalance = InnerYieldBalanceState.Data(
            rewardsFiat = "100 $",
            rewardsCrypto = "100 SOL",
            isRewardsToClaim = false,
            balance = listOf(
                BalanceGroupedState(
                    title = stringReference("Staked"),
                    footer = null,
                    type = BalanceGroupType.ACTIVE,
                    items = persistentListOf(
                        BalanceState(
                            cryptoValue = "100",
                            cryptoAmount = stringReference("100 SOL"),
                            cryptoDecimal = "100".toBigDecimal(),
                            fiatAmount = stringReference("100 $"),
                            rawCurrencyId = null,
                            validator = Yield.Validator(
                                address = "address",
                                status = "status",
                                name = "Binance",
                                image = null,
                                website = null,
                                apr = "5".toBigDecimal(),
                                commission = null,
                                stakedBalance = null,
                                votingPower = null,
                                preferred = false,
                            ),
                            unbondingPeriod = stringReference("3 days"),
                            pendingActions = persistentListOf(),
                        ),
                    ),
                ),
            ),
        ),
    )
}