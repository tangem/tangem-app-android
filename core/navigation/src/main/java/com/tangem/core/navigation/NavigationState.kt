package com.tangem.core.navigation

import androidx.appcompat.app.AppCompatActivity
import org.rekotlin.StateType
import java.lang.ref.WeakReference

data class NavigationState(
    val backStack: List<AppScreen> = emptyList(),
    val activity: WeakReference<AppCompatActivity>? = null,
) : StateType

enum class AppScreen(val isDialogFragment: Boolean = false) {
    Home,
    Disclaimer,
    OnboardingNote,
    OnboardingWallet,
    OnboardingTwins,
    OnboardingOther,
    Wallet,
    WalletDetails,
    Send(isDialogFragment = true),
    Details,
    DetailsSecurity,
    CardSettings,
    AppSettings,
    ResetToFactory,
    AccessCodeRecovery,
    ManageTokens,
    AddCustomToken,
    WalletConnectSessions,
    QrScanning,
    ReferralProgram,
    Swap,
    Welcome,
    SaveWallet(isDialogFragment = true),
    AppCurrencySelector,
    ModalNotification(isDialogFragment = true),
}
