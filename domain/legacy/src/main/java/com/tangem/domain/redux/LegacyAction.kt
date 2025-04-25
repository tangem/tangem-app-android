package com.tangem.domain.redux

import org.rekotlin.Action

sealed interface LegacyAction : Action {

    data object PrepareDetailsScreen : LegacyAction
}