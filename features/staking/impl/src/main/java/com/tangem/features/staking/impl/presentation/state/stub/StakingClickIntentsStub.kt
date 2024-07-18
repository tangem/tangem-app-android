package com.tangem.features.staking.impl.presentation.state.stub

import com.tangem.domain.staking.model.Yield
import com.tangem.features.staking.impl.presentation.state.BalanceState
import com.tangem.features.staking.impl.presentation.state.transformers.InfoType
import com.tangem.features.staking.impl.presentation.viewmodel.StakingClickIntents

object StakingClickIntentsStub : StakingClickIntents {

    override fun onBackClick() {}

    override fun onNextClick() {}

    override fun onPrevClick() {}

    override fun onInfoClick(infoType: InfoType) {}

    override fun onAmountValueChange(value: String) {}

    override fun onAmountPasteTriggerDismiss() {}

    override fun onMaxValueClick() {}

    override fun onCurrencyChangeClick(isFiat: Boolean) {}

    override fun onAmountNext() {}

    override fun openValidators() {}

    override fun onValidatorSelect(validator: Yield.Validator) {}

    override fun openRewardsValidators() {}

    override fun selectRewardValidator(rewardValue: String) {}

    override fun onExploreClick() {}

    override fun onShareClick() {}

    override fun onActiveStake(activeStake: BalanceState) {}
}