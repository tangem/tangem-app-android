package com.tangem.features.staking.impl.presentation.viewmodel

import com.tangem.common.ui.amountScreen.AmountScreenClickIntents
import com.tangem.domain.staking.model.Yield

internal interface StakingClickIntents : AmountScreenClickIntents {

    fun onBackClick()

    fun onNextClick()

    fun onPrevClick()

    override fun onAmountNext() = onNextClick()

    fun openValidators()

    fun onValidatorSelect(validator: Yield.Validator)
}