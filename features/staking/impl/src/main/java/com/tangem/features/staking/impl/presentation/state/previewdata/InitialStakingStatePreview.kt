package com.tangem.features.staking.impl.presentation.state.previewdata

import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.features.staking.impl.presentation.state.*
import kotlinx.collections.immutable.persistentListOf

internal object InitialStakingStatePreview {
    val defaultState = StakingStates.InitialInfoState.Data(
        isPrimaryButtonEnabled = true,
        available = "15 SOL",
        onStake = "0 SOL",
        aprRange = stringReference("2.54-5.12%"),
        unbondingPeriod = "3d",
        minimumRequirement = "12 SOL",
        rewardClaiming = "Auto",
        warmupPeriod = "Days",
        rewardSchedule = "Block",
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