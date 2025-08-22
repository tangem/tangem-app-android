package com.tangem.features.staking.impl.presentation.state.previewdata

import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.list.RoundedListWithDividersItemData
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.staking.model.stakekit.BalanceType
import com.tangem.domain.staking.model.stakekit.RewardBlockType
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.BalanceState
import com.tangem.features.staking.impl.presentation.state.InnerYieldBalanceState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.YieldReward
import kotlinx.collections.immutable.persistentListOf

internal object InitialStakingStatePreview {
    val defaultState = StakingStates.InitialInfoState.Data(
        isPrimaryButtonEnabled = true,
        showBanner = true,
        aprRange = stringReference("2.54-5.12%"),
        infoItems = persistentListOf(
            RoundedListWithDividersItemData(
                id = R.string.staking_details_available,
                startText = TextReference.Res(R.string.staking_details_available),
                endText = TextReference.Str("15 SOL"),
                isEndTextHideable = true,
            ),
            RoundedListWithDividersItemData(
                id = R.string.staking_details_annual_percentage_rate,
                startText = TextReference.Res(R.string.staking_details_annual_percentage_rate),
                endText = TextReference.Str("2.54-5.12%"),
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
        pullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = {}),
    )

    val stateWithYield = defaultState.copy(
        yieldBalance = InnerYieldBalanceState.Data(
            integrationId = null,
            reward = YieldReward(
                rewardsFiat = "100 $",
                rewardsCrypto = "100 SOL",
                rewardBlockType = RewardBlockType.RewardUnavailable,
                rewardConstraints = null,
            ),
            isActionable = true,
            balances = persistentListOf(
                BalanceState(
                    groupId = "groupId",
                    title = stringReference("Binance"),
                    cryptoValue = "100",
                    formattedCryptoAmount = stringReference("100 SOL"),
                    cryptoAmount = "100".toBigDecimal(),
                    fiatAmount = null,
                    formattedFiatAmount = stringReference("100 $"),
                    rawCurrencyId = null,
                    validator = Yield.Validator(
                        address = "address",
                        status = Yield.Validator.ValidatorStatus.ACTIVE,
                        name = "Binance",
                        image = null,
                        website = null,
                        apr = "5".toBigDecimal(),
                        commission = null,
                        stakedBalance = null,
                        votingPower = null,
                        preferred = false,
                        isStrategicPartner = false,
                    ),
                    pendingActions = persistentListOf(),
                    isClickable = true,
                    type = BalanceType.STAKED,
                    subtitle = null,
                    isPending = false,
                    validatorAddress = "",
                ),
            ),
        ),
    )
}