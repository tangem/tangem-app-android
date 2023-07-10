package com.tangem.tap.features.details.ui.details

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.tangem.core.analytics.Analytics
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.tap.common.analytics.events.Settings
import com.tangem.tap.common.feedback.FeedbackEmail
import com.tangem.tap.common.feedback.SupportInfo
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.details.redux.DetailsState
import com.tangem.tap.features.disclaimer.redux.DisclaimerAction
import com.tangem.tap.features.home.LocaleRegionProvider
import com.tangem.tap.features.home.RUSSIA_COUNTRY_CODE
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.wallet.BuildConfig
import org.rekotlin.Store

class DetailsViewModel(private val store: Store<AppState>) {

    var detailsScreenState: MutableState<DetailsScreenState> = mutableStateOf(updateState(store.state.detailsState))
        private set

    @Suppress("ComplexMethod")
    fun updateState(state: DetailsState): DetailsScreenState {
        val cardTypesResolver = state.scanResponse?.cardTypesResolver
        val settings = SettingsElement.values().mapNotNull {
            when (it) {
                SettingsElement.WalletConnect -> {
                    if (cardTypesResolver?.isMultiwalletAllowed() == true) it else null
                }
                SettingsElement.SendFeedback -> it
                SettingsElement.LinkMoreCards -> if (state.createBackupAllowed) it else null
                SettingsElement.PrivacyPolicy -> {
                    if (state.privacyPolicyUrl != null) it else null
                }
                SettingsElement.AppSettings -> if (state.appSettingsState.isBiometricsAvailable) it else null
                SettingsElement.AppCurrency -> if (cardTypesResolver?.isMultiwalletAllowed() != true) it else null
                SettingsElement.ReferralProgram -> if (cardTypesResolver?.isTangemWallet() == true) it else null
                SettingsElement.TesterMenu -> if (BuildConfig.TESTER_MENU_ENABLED) it else null
                else -> it
            }
        }

        return DetailsScreenState(
            elements = settings,
            tangemLinks = getSocialLinks(),
            tangemVersion = getTangemAppVersion(),
            appCurrency = state.appCurrency.name,
            onItemsClick = { handleClickingSettingsItem(it) },
            onSocialNetworkClick = { handleSocialNetworkClick(it) },
        )
    }

    private fun handleSocialNetworkClick(link: SocialNetworkLink) {
        Analytics.send(Settings.ButtonSocialNetwork(link.network))
        store.dispatch(NavigationAction.OpenUrl(link.url))
    }

    private fun handleClickingSettingsItem(item: SettingsElement) {
        when (item) {
            SettingsElement.WalletConnect -> {
                Analytics.send(Settings.ButtonWalletConnect())
                store.dispatch(NavigationAction.NavigateTo(AppScreen.WalletConnectSessions))
            }
            SettingsElement.Chat -> {
                Analytics.send(Settings.ButtonChat())
                store.dispatch(GlobalAction.OpenChat(SupportInfo()))
            }
            SettingsElement.SendFeedback -> {
                Analytics.send(Settings.ButtonSendFeedback())
                store.dispatch(GlobalAction.SendEmail(FeedbackEmail()))
            }
            SettingsElement.CardSettings -> {
                Analytics.send(Settings.ButtonCardSettings())
                store.dispatch(NavigationAction.NavigateTo(AppScreen.CardSettings))
            }
            SettingsElement.AppCurrency -> {
                store.dispatch(WalletAction.AppCurrencyAction.ChooseAppCurrency)
            }
            SettingsElement.AppSettings -> {
                Analytics.send(Settings.ButtonAppSettings())
                store.dispatch(NavigationAction.NavigateTo(AppScreen.AppSettings))
            }
            SettingsElement.LinkMoreCards -> {
                Analytics.send(Settings.ButtonCreateBackup())
                store.dispatch(WalletAction.MultiWallet.BackupWallet)
            }
            SettingsElement.TermsOfService -> {
                store.dispatch(DisclaimerAction.Show(AppScreen.Details))
            }
            SettingsElement.PrivacyPolicy -> {
                // TODO: To be available later
            }
            SettingsElement.ReferralProgram -> {
                store.dispatch(NavigationAction.NavigateTo(AppScreen.ReferralProgram))
            }
            SettingsElement.TesterMenu -> {
                store.state.daggerGraphState.testerRouter?.startTesterScreen()
            }
        }
    }

    private fun getSocialLinks(): List<SocialNetworkLink> {
        val locale = LocaleRegionProvider().getRegion()
        return if (locale.lowercase() == RUSSIA_COUNTRY_CODE) {
            TangemSocialAccounts.accountsRu
        } else {
            TangemSocialAccounts.accountsEn
        }
    }

    private fun getTangemAppVersion(): String {
        val versionCode: Int = BuildConfig.VERSION_CODE
        val versionName: String = BuildConfig.VERSION_NAME
        return "$versionName ($versionCode)"
    }
}