package com.tangem.features.staking.impl.presentation.viewmodel

import com.tangem.common.ui.amountScreen.AmountScreenClickIntents
import com.tangem.domain.staking.model.Yield
import com.tangem.features.staking.impl.presentation.state.BalanceState
import com.tangem.features.staking.impl.presentation.state.transformers.InfoType

internal interface StakingClickIntents : AmountScreenClickIntents {

    fun onBackClick()

    fun onNextClick()

    fun onPrevClick()

    fun onInfoClick(infoType: InfoType)

    override fun onAmountNext() = onNextClick()

    fun openValidators()

    fun onValidatorSelect(validator: Yield.Validator)

    fun openRewardsValidators()

    fun selectRewardValidator(rewardValue: String)

    fun onActiveStake(activeStake: BalanceState)

    fun onExploreClick()

    fun onShareClick()
}
