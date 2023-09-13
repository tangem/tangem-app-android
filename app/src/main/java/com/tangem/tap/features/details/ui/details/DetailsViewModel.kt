package com.tangem.tap.features.details.ui.details

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.tangem.core.analytics.Analytics
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.event.triggeredEvent
import com.tangem.core.ui.extensions.TextReference
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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
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
            showSnackbar = triggerErrorSnackbarIfNeeded(state.error),
            onSocialNetworkClick = ::handleSocialNetworkClick,
        )
    }

    private fun createSettingsItems(state: DetailsState): ImmutableList<SettingsItem> {
        val scanResponse = state.scanResponse ?: return persistentListOf()
        val cardTypesResolver = scanResponse.cardTypesResolver

        return buildList {
            SettingsItem.WalletConnect(::navigateToWalletConnect)
                .takeIf { cardTypesResolver.isMultiwalletAllowed() }
                ?.let(::add)

            SettingsItem.AddWallet(showProgress = state.isScanningInProgress, ::scanAndSaveUserWallet)
                .takeIf { state.appSettingsState.saveWallets }
                ?.let(::add)

            SettingsItem.ScanWallet(showProgress = state.isScanningInProgress, ::scanAndSaveUserWallet)
                .takeUnless { state.appSettingsState.saveWallets }
                ?.let(::add)

            SettingsItem.LinkMoreCards(::linkMoreCards)
                .takeIf { state.createBackupAllowed }
                ?.let(::add)

            SettingsItem.CardSettings(::navigateToCardSettings)
                .let(::add)

            SettingsItem.AppSettings(::navigateToAppSettings)
                .let(::add)

            SettingsItem.Chat(::navigateToChat)
                .let(::add)

            SettingsItem.SendFeedback(::sendFeedback)
                .let(::add)

            SettingsItem.ReferralProgram(::navigateToReferralProgram)
                .takeIf { cardTypesResolver.isTangemWallet() }
                ?.let(::add)

            SettingsItem.TermsOfService(::navigateToToS)
                .let(::add)

            SettingsItem.TesterMenu(::navigateToTesterMenu)
                .takeIf { BuildConfig.TESTER_MENU_ENABLED }
                ?.let(::add)
        }.toImmutableList()
    }

    private fun triggerErrorSnackbarIfNeeded(text: TextReference?): StateEvent<TextReference> {
        return if (text == null) {
            consumedEvent()
        } else {
            triggeredEvent(text) {
                store.dispatch(DetailsAction.DismissError)
            }
        }
    }

    private fun getTangemAppVersion(): String {
        val versionCode: Int = BuildConfig.VERSION_CODE
        val versionName: String = BuildConfig.VERSION_NAME
        return "$versionName ($versionCode)"
    }

    private fun navigateToTesterMenu() {
        store.state.daggerGraphState.testerRouter?.startTesterScreen()
    }

    private fun navigateToToS() {
        store.dispatch(DisclaimerAction.Show(AppScreen.Details))
    }

    private fun navigateToReferralProgram() {
        store.dispatch(NavigationAction.NavigateTo(AppScreen.ReferralProgram))
    }

    private fun sendFeedback() {
        Analytics.send(Settings.ButtonSendFeedback())
        store.dispatch(GlobalAction.SendEmail(FeedbackEmail()))
    }

    private fun navigateToChat() {
        Analytics.send(Settings.ButtonChat())
        store.dispatch(GlobalAction.OpenChat(SupportInfo()))
    }

    private fun navigateToAppSettings() {
        Analytics.send(Settings.ButtonAppSettings())
        store.dispatch(NavigationAction.NavigateTo(AppScreen.AppSettings))
    }

    private fun navigateToCardSettings() {
        Analytics.send(Settings.ButtonCardSettings())
        store.dispatch(NavigationAction.NavigateTo(AppScreen.CardSettings))
    }

    private fun linkMoreCards() {
        Analytics.send(Settings.ButtonCreateBackup())
        store.dispatch(WalletAction.MultiWallet.BackupWallet)
    }

    private fun scanAndSaveUserWallet() {
        store.dispatch(DetailsAction.ScanAndSaveUserWallet)
    }

    private fun navigateToWalletConnect() {
        Analytics.send(Settings.ButtonWalletConnect())
        store.dispatch(NavigationAction.NavigateTo(AppScreen.WalletConnectSessions))
    }

    private fun handleSocialNetworkClick(link: SocialNetworkLink) {
        Analytics.send(Settings.ButtonSocialNetwork(link.network))
        store.dispatch(NavigationAction.OpenUrl(link.url))
    }

    private fun getSocialLinks(): ImmutableList<SocialNetworkLink> {
        val locale = LocaleRegionProvider().getRegion()
        return if (locale.lowercase() == RUSSIA_COUNTRY_CODE) {
            TangemSocialAccounts.accountsRu
        } else {
            TangemSocialAccounts.accountsEn
        }
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
