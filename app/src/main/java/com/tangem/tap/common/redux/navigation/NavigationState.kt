package com.tangem.tap.common.redux.navigation

import androidx.fragment.app.FragmentActivity
import org.rekotlin.StateType
import java.lang.ref.WeakReference

data class NavigationState(
    val backStack: List<AppScreen> = listOf(AppScreen.Home),
    val activity: WeakReference<FragmentActivity>? = null
) : StateType

enum class AppScreen { Home, Wallet, Send, Details }
