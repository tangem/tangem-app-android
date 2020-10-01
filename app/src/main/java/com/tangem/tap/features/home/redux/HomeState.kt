package com.tangem.tap.features.home.redux

import org.rekotlin.StateType

data class HomeState(
        val firstLaunch: Boolean = true
) : StateType
