package com.tangem.features.staking.impl.presentation.state.stub

import com.tangem.features.staking.impl.presentation.viewmodel.StakingClickIntents

object StakingClickIntentsStub : StakingClickIntents {

    override fun onBackClick() {}

    override fun onNextClick() {}

    override fun onPrevClick() {}

    override fun onAmountValueChange(value: String) {}

    override fun onAmountPasteTriggerDismiss() {}

    override fun onMaxValueClick() {}

    override fun onCurrencyChangeClick(isFiat: Boolean) {}

    override fun onAmountNext() {}
}