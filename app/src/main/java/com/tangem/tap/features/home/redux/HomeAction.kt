package com.tangem.tap.features.home.redux

import android.content.Context
import org.rekotlin.Action

sealed class HomeAction : Action {
    object ReadCard : HomeAction()
    data class GoToShop(val context: Context) : HomeAction()
}
