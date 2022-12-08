package com.tangem.tap.common.redux.navigation

import androidx.appcompat.app.AppCompatActivity
import org.rekotlin.StateType
import java.lang.ref.WeakReference

data class NavigationState(
    val backStack: List<AppScreen> = emptyList(),
    val activity: WeakReference<AppCompatActivity>? = null,
) : StateType

enum class AppScreen(
    val isDialogFragment: Boolean = false,
) {
    Home,
    Shop,
    Disclaimer,
    OnboardingNote, OnboardingWallet, OnboardingTwins, OnboardingOther,
    Wallet, WalletDetails,
    Send,
    Details, DetailsSecurity, CardSettings, AppSettings, ResetToFactory,
    AddTokens, AddCustomToken,
    WalletConnectSessions,
    QrScan,
    ReferralProgram,
    Welcome,
    SaveWallet(isDialogFragment = true),
    WalletSelector(isDialogFragment = true),
}
