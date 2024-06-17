package com.tangem.features.staking.impl.presentation.viewmodel

import com.tangem.common.ui.amountScreen.AmountScreenClickIntents

internal interface StakingClickIntents : AmountScreenClickIntents {

    fun onBackClick()

    fun onNextClick()

    fun onPrevClick()

    override fun onAmountNext() = onNextClick()
}