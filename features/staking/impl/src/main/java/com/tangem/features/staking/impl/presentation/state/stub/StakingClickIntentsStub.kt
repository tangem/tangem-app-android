package com.tangem.features.staking.impl.presentation.state.stub

import com.tangem.domain.staking.model.stakekit.PendingAction
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.features.staking.impl.presentation.state.BalanceState
import com.tangem.features.staking.impl.presentation.state.transformers.InfoType
import com.tangem.features.staking.impl.presentation.viewmodel.StakingClickIntents
import kotlinx.collections.immutable.ImmutableList

object StakingClickIntentsStub : StakingClickIntents {

    override fun onBackClick() {}

    override fun onNextClick(pendingActions: ImmutableList<PendingAction>) {}

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
