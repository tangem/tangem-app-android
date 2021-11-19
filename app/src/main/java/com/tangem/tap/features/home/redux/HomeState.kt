package com.tangem.tap.features.home.redux

import com.tangem.tap.common.entities.IndeterminateProgressButton
import com.tangem.tap.common.redux.navigation.FragmentShareTransition
import com.tangem.tap.features.send.redux.states.ButtonState
import org.rekotlin.StateType

data class HomeState(
    val shouldScanCardOnResume: Boolean = false,
    val shareTransition: FragmentShareTransition? = null,
    val btnScanState: IndeterminateProgressButton = IndeterminateProgressButton(ButtonState.ENABLED),
) : StateType