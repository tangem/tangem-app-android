package com.tangem.features.staking.impl.presentation.state.stub

import com.tangem.domain.staking.model.Yield
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
}