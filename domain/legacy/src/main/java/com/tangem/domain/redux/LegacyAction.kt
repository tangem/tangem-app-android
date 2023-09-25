package com.tangem.domain.redux

import org.rekotlin.Action

sealed interface LegacyAction : Action {

    object SendEmailRateCanBeBetter : LegacyAction
}