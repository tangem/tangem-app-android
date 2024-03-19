package com.tangem.tap.features.details.ui.details

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.tangem.common.extensions.guard
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.models.Basic
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.core.navigation.email.EmailSender
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.event.triggeredEvent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.feedback.FeedbackManagerFeatureToggles
import com.tangem.domain.feedback.GetSupportFeedbackEmailUseCase
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.tap.common.analytics.events.Settings
import com.tangem.tap.common.extensions.addContext
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.dispatchWithMain
import com.tangem.tap.common.feedback.FeedbackEmail
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.details.redux.DetailsState
import com.tangem.tap.features.disclaimer.redux.DisclaimerAction
import com.tangem.tap.features.home.LocaleRegionProvider
import com.tangem.tap.features.home.RUSSIA_COUNTRY_CODE
import com.tangem.tap.mainScope
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
import kotlinx.coroutines.launch
import org.rekotlin.Store
import timber.log.Timber

// TODO: change to Android ViewModel [REDACTED_JIRA]
internal class DetailsViewModel(
    private val store: Store<AppState>,
    private val walletsRepository: WalletsRepository,
    private val feedbackManagerFeatureToggles: FeedbackManagerFeatureToggles,
    private val getSupportFeedbackEmailUseCase: GetSupportFeedbackEmailUseCase,
    private val emailSender: EmailSender,
) {

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

            // removed chat in task [REDACTED_TASK_KEY]
            // SettingsItem.Chat(::navigateToChat)
            //     .let(::add)

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
        store.dispatchOnMain(DisclaimerAction.Show(AppScreen.Details))
    }

    private fun navigateToReferralProgram() {
        store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.ReferralProgram))
    }

    private fun sendFeedback() {
        Analytics.send(Basic.ButtonSupport())
        if (feedbackManagerFeatureToggles.isLocalLogsEnabled) {
            mainScope.launch {
                val email = getSupportFeedbackEmailUseCase()
                emailSender.send(
                    email = EmailSender.Email(
                        address = email.address,
                        subject = email.subject,
                        message = email.message,
                        attachment = email.file,
                    ),
                )
            }
        } else {
            store.dispatchOnMain(GlobalAction.SendEmail(FeedbackEmail()))
        }
    }

    private fun navigateToAppSettings() {
        Analytics.send(Settings.ButtonAppSettings())
        store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.AppSettings))
    }

    private fun navigateToCardSettings() {
        Analytics.send(Settings.ButtonCardSettings())
        store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.CardSettings))
    }

    private fun linkMoreCards() {
        Analytics.send(Settings.ButtonCreateBackup())

        val selectedUserWallet = userWalletsListManager.selectedUserWalletSync.guard {
            Timber.e("Unable to backup wallet, no user wallet selected")
            return
        }
        val scanResponse = selectedUserWallet.scanResponse
        Analytics.addContext(scanResponse)
        store.dispatch(GlobalAction.Onboarding.Start(scanResponse, canSkipBackup = false))
        store.dispatch(NavigationAction.NavigateTo(AppScreen.OnboardingWallet))
    }

    private fun scanAndSaveUserWallet() {
        Analytics.send(Settings.ScanNewCard)
        store.dispatchOnMain(DetailsAction.ScanAndSaveUserWallet)
    }

    private fun navigateToWalletConnect() {
        Analytics.send(Settings.ButtonWalletConnect())
        store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.WalletConnectSessions))
    }

    private fun handleSocialNetworkClick(link: SocialNetworkLink) {
        Analytics.send(Settings.ButtonSocialNetwork(link.network))
        store.dispatchOnMain(NavigationAction.OpenUrl(link.url))
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
                store.dispatchWithMain(
                    DetailsAction.PrepareScreen(
                        scanResponse = selectedUserWallet.scanResponse,
                        shouldSaveUserWallets = walletsRepository.shouldSaveUserWalletsSync(),
                    ),
                )
            }
            .flowOn(Dispatchers.IO)
            .launchIn(scope)
    }
}