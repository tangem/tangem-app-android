package com.tangem.tap.features.details.ui.details

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.tangem.core.analytics.Analytics
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.tap.common.analytics.events.Settings
import com.tangem.tap.common.extensions.dispatchWithMain
import com.tangem.tap.common.feedback.FeedbackEmail
import com.tangem.tap.common.feedback.SupportInfo
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.details.redux.DetailsState
import com.tangem.tap.features.disclaimer.redux.DisclaimerAction
import com.tangem.tap.features.home.LocaleRegionProvider
import com.tangem.tap.features.home.RUSSIA_COUNTRY_CODE
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.scope
import com.tangem.tap.userWalletsListManager
import com.tangem.wallet.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.rekotlin.Store

internal class DetailsViewModel(private val store: Store<AppState>) {

    var detailsScreenState: MutableState<DetailsScreenState> = mutableStateOf(updateState(store.state.detailsState))
        private set

    init {
        bootstrapScreenState()
    }

    fun updateState(state: DetailsState): DetailsScreenState {
        return DetailsScreenState(
            elements = createSettingsItems(state),
            tangemLinks = getSocialLinks(),
            tangemVersion = getTangemAppVersion(),
            onItemsClick = { handleClickingSettingsItem(it) },
            onSocialNetworkClick = { handleSocialNetworkClick(it) },
        )
    }

    @Suppress("ComplexMethod")
    private fun createSettingsItems(state: DetailsState): List<SettingsElement> {
        val scanResponse = state.scanResponse ?: return emptyList()
        val cardTypesResolver = scanResponse.cardTypesResolver

        return SettingsElement.values().mapNotNull {
            when (it) {
                SettingsElement.WalletConnect -> if (cardTypesResolver.isMultiwalletAllowed()) it else null
                SettingsElement.SendFeedback -> it
                SettingsElement.LinkMoreCards -> if (state.createBackupAllowed) it else null
                SettingsElement.PrivacyPolicy -> if (state.privacyPolicyUrl != null) it else null
                SettingsElement.AppSettings -> if (state.appSettingsState.isBiometricsAvailable) it else null
                SettingsElement.ReferralProgram -> if (cardTypesResolver.isTangemWallet()) it else null
                SettingsElement.TesterMenu -> if (BuildConfig.TESTER_MENU_ENABLED) it else null
                else -> it
            }
        }
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

    private fun bootstrapScreenState() {
        userWalletsListManager.selectedUserWallet
            .distinctUntilChanged()
            .onEach { selectedUserWallet ->
                store.dispatchWithMain(DetailsAction.PrepareScreen(selectedUserWallet.scanResponse))
            }
            .flowOn(Dispatchers.IO)
            .launchIn(scope)
    }
}