package com.tangem.features.introduction.impl.addwallet

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.OnboardingAnalyticsEvent
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.message.dialog.Dialogs
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_20
import com.tangem.core.ui.res.generated.icons.ic_wallet_plus_20
import com.tangem.domain.card.analytics.IntroductionProcess
import com.tangem.domain.cloudbackup.usecase.ResolveCloudRestoreBackupsUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.hotwallet.IsHotWalletCreationSupported
import com.tangem.domain.sociallogin.models.SocialLoginProvider
import com.tangem.domain.wallets.analytics.Settings
import com.tangem.domain.wallets.analytics.Settings.NoticeMobileWalletLimit
import com.tangem.domain.wallets.analytics.toAnalyticsWalletsType
import com.tangem.domain.wallets.models.HotWalletAdditionRestriction
import com.tangem.domain.wallets.usecase.GetHotWalletAdditionRestrictionUseCase
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.features.introduction.AddWalletComponent
import com.tangem.features.introduction.impl.analytics.SocialLoginAnalyticsEvents
import com.tangem.features.introduction.impl.model.IntroductionMobileWalletCreator
import com.tangem.features.introduction.impl.model.IntroductionSocialLoginHandler
import com.tangem.features.introduction.impl.model.WalletOptionsSheetController
import com.tangem.features.introduction.impl.ui.state.AddWalletUM
import com.tangem.features.introduction.impl.ui.state.HaveWalletUM
import com.tangem.features.introduction.impl.ui.state.SocialLoginConsentUM
import com.tangem.features.introduction.impl.ui.state.SocialLoginProgressUM
import com.tangem.features.introduction.impl.ui.state.WalletOptionUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.R as CoreUiR

@Suppress("LongParameterList", "TooManyFunctions")
@ModelScoped
internal class AddWalletModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    router: Router,
    hotWalletFeatureToggles: HotWalletFeatureToggles,
    resolveCloudRestoreBackups: ResolveCloudRestoreBackupsUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val isHotWalletCreationSupported: IsHotWalletCreationSupported,
    private val getHotWalletAdditionRestriction: GetHotWalletAdditionRestrictionUseCase,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val socialLoginHandler: IntroductionSocialLoginHandler,
    private val mobileWalletCreator: IntroductionMobileWalletCreator,
    private val trackingContextProxy: TrackingContextProxy,
    @GlobalUiMessageSender private val uiMessageSender: UiMessageSender,
) : Model(), WalletOptionsSheetController.Host {

    private val params = paramsContainer.require<AddWalletComponent.Params>()

    private val activity = MutableStateFlow(Activity.Idle)

    private val sheetCollapsed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val uiState: StateFlow<AddWalletUM>
        field = MutableStateFlow(
            AddWalletUM(
                sheetConfig = null,
                socialLoginProgress = null,
                socialLoginConsent = null,
                onSheetCollapsed = ::onSheetCollapsed,
            ),
        )

    private val sheet = Sheet(
        router = router,
        hotWalletFeatureToggles = hotWalletFeatureToggles,
        resolveCloudRestoreBackups = resolveCloudRestoreBackups,
    )

    override val haveWalletSheetConfig: TangemBottomSheetConfig? get() = uiState.value.sheetConfig

    init {
        modelScope.launch {
            socialLoginHandler.resolveProvider()
            sheet.refreshOptions()
        }
    }

    fun open() {
        if (sheet.isShown || isBusy()) return
        sheet.open()
    }

    override fun updateHaveWalletSheetConfig(transform: (TangemBottomSheetConfig?) -> TangemBottomSheetConfig?) {
        uiState.update { it.copy(sheetConfig = transform(it.sheetConfig)) }
    }

    override fun onTerminalFlowEntered() = Unit

    override fun ensureHotWalletCreationSupported(): Boolean {
        if (isHotWalletCreationSupported()) return true
        uiMessageSender.send(
            Dialogs.hotWalletCreationNotSupportedDialog(isHotWalletCreationSupported.getLeastVersionName()),
        )
        return false
    }

    private fun isBusy(): Boolean = activity.value != Activity.Idle

    private fun isRowRunning(): Boolean = activity.value.isRowRunning

    // A row leaving the screen with its step stays tappable until the next step has slid in.
    private fun canTapRow(): Boolean = sheet.isShown && !sheet.isOnImport && !isBusy()

    private fun setActivity(value: Activity) {
        activity.value = value
        sheet.refreshOptions()
    }

    private fun onAddCardClick() {
        if (!canTapRow()) return
        val walletsType = userWalletsListRepository.userWallets.value.toAnalyticsWalletsType()
        analyticsEventHandler.send(Settings.ButtonAddHardwareWallet(walletsType))
        sheet.dismiss()
        params.callbacks.onAddCardClick()
    }

    private fun onSocialLoginClick(provider: SocialLoginProvider) {
        if (!canTapRow()) return
        // Keeps the selected wallet's Product Type off these events; released unless a wallet opens.
        trackingContextProxy.addHotWalletContext()
        analyticsEventHandler.send(
            SocialLoginAnalyticsEvents.ButtonContinue(
                provider = provider,
                source = AnalyticsParam.ScreensSources.AddNewWallet,
            ),
        )
        if (!ensureHotWalletCreationSupported()) {
            analyticsEventHandler.send(SocialLoginAnalyticsEvents.MinOsNotSupported())
            trackingContextProxy.removeContext()
            return
        }
        checkRestriction(
            isSocialLogin = true,
            onAbandoned = trackingContextProxy::removeContext,
            onFailed = {
                trackingContextProxy.removeContext()
                showUnknownError()
            },
        ) { restriction ->
            if (restriction != null) trackingContextProxy.removeContext()
            when (restriction) {
                HotWalletAdditionRestriction.LimitReached -> showLimitReached(NoticeMobileWalletLimit.Type.SocialLogin)
                HotWalletAdditionRestriction.SocialWalletAlreadyAdded -> showSocialWalletAlreadyAdded(provider)
                null -> startSocialLogin(provider)
            }
        }
    }

    private fun startSocialLogin(provider: SocialLoginProvider) {
        setActivity(Activity.SigningInSocially)
        socialLoginHandler.start(scope = modelScope, provider = provider, host = SocialLoginHost())
    }

    private fun onSeedPhraseClick() {
        if (!canTapRow()) return
        trackingContextProxy.addHotWalletContext()
        analyticsEventHandler.send(
            OnboardingAnalyticsEvent.Onboarding.ButtonMobileWallet(
                source = AnalyticsParam.ScreensSources.AddNewWallet.value,
            ),
        )
        if (!ensureHotWalletCreationSupported()) {
            trackingContextProxy.removeContext()
            return
        }
        checkRestriction(isSocialLogin = false, onAbandoned = trackingContextProxy::removeContext) { restriction ->
            if (restriction == HotWalletAdditionRestriction.LimitReached) {
                trackingContextProxy.removeContext()
                showLimitReached(NoticeMobileWalletLimit.Type.Create)
            } else {
                createMobileWallet()
            }
        }
    }

    private fun createMobileWallet() {
        setActivity(Activity.CreatingMobileWallet)
        modelScope.launch {
            mobileWalletCreator.createAndOpen(
                source = AnalyticsParam.ScreensSources.AddNewWallet,
                beforeOpen = ::collapseSheet,
                onFailed = {
                    trackingContextProxy.removeContext()
                    setActivity(Activity.Idle)
                },
            )
        }
    }

    private fun onImportWalletClick() {
        if (!canTapRow()) return
        analyticsEventHandler.send(
            IntroductionProcess.ButtonImportExistingWallet(source = AnalyticsParam.ScreensSources.AddNewWallet),
        )
        if (!ensureHotWalletCreationSupported()) return
        checkRestriction(isSocialLogin = false) { restriction ->
            if (restriction == HotWalletAdditionRestriction.LimitReached) {
                showLimitReached(NoticeMobileWalletLimit.Type.Import)
            } else {
                sheet.startImport()
            }
        }
    }

    /** Without [onFailed], a failed check lets the flow start: it checks the limit again when it saves the wallet. */
    private fun checkRestriction(
        isSocialLogin: Boolean,
        onAbandoned: () -> Unit = {},
        onFailed: (() -> Unit)? = null,
        onChecked: (HotWalletAdditionRestriction?) -> Unit,
    ) {
        if (!activity.compareAndSet(Activity.Idle, Activity.CheckingRestriction)) return
        modelScope.launch {
            val result = try {
                runSuspendCatching { getHotWalletAdditionRestriction(isSocialLogin = isSocialLogin) }
            } finally {
                activity.compareAndSet(Activity.CheckingRestriction, Activity.Idle)
            }
            result.onFailure { TangemLogger.e("Failed to check the hot wallet addition restriction", it) }
            when {
                !sheet.isShown -> onAbandoned()
                result.isFailure && onFailed != null -> onFailed()
                else -> onChecked(result.getOrNull())
            }
        }
    }

    private fun showLimitReached(type: NoticeMobileWalletLimit.Type) {
        analyticsEventHandler.send(
            NoticeMobileWalletLimit(type = type, limit = NoticeMobileWalletLimit.Limit.MobileWallets),
        )
        uiMessageSender.send(Dialogs.hotWalletsLimitReachedDialog())
    }

    private fun showUnknownError() {
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(CoreResR.string.common_something_went_wrong),
                message = resourceReference(CoreResR.string.common_unknown_error),
            ),
        )
    }

    private fun showSocialWalletAlreadyAdded(provider: SocialLoginProvider) {
        analyticsEventHandler.send(
            NoticeMobileWalletLimit(
                type = NoticeMobileWalletLimit.Type.SocialLogin,
                limit = NoticeMobileWalletLimit.Limit.SocialLogin,
            ),
        )
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(
                    id = CoreResR.string.social_wallet_already_added_alert_title,
                    formatArgs = wrappedList(provider.displayName),
                ),
                message = resourceReference(
                    id = CoreResR.string.social_wallet_already_added_alert_message,
                    formatArgs = wrappedList(provider.displayName),
                ),
                firstAction = EventMessageAction(
                    title = resourceReference(CoreResR.string.common_got_it),
                    onClick = {},
                ),
            ),
        )
    }

    private fun onSheetCollapsed() {
        sheetCollapsed.tryEmit(Unit)
    }

    /** The sheet outlives the host screen, so the stack swaps only after it has collapsed. */
    private suspend fun collapseSheet() {
        if (!sheet.isShown) return
        sheet.dismiss()
        withTimeoutOrNull(SHEET_COLLAPSE_TIMEOUT) { sheetCollapsed.first() }
    }

    private inner class SocialLoginHost : IntroductionSocialLoginHandler.Host {

        private var isOpeningWallet = false
        private var isSheetSteppedAside = false

        // The tapped row already spins in the sheet, so it steps aside only for a screen that says something.
        override fun onProgress(progress: SocialLoginProgressUM?) {
            if (progress == SocialLoginProgressUM.Loader && sheet.isShown) return
            if (progress != null) stepSheetAside()
            uiState.update { it.copy(socialLoginProgress = progress) }
        }

        override fun onConsent(consent: SocialLoginConsentUM?) {
            if (consent != null) stepSheetAside()
            uiState.update { it.copy(socialLoginConsent = consent) }
        }

        override fun onWalletOpening() {
            isOpeningWallet = true
        }

        override fun onFinished() {
            if (!isOpeningWallet) trackingContextProxy.removeContext()
            setActivity(Activity.Idle)
            if (isSheetSteppedAside && !isOpeningWallet) sheet.open()
        }

        private fun stepSheetAside() {
            if (!sheet.isShown) return
            isSheetSteppedAside = true
            sheet.dismiss()
        }
    }

    private inner class Sheet(
        router: Router,
        hotWalletFeatureToggles: HotWalletFeatureToggles,
        resolveCloudRestoreBackups: ResolveCloudRestoreBackupsUseCase,
    ) : WalletOptionsSheetController(
        host = this@AddWalletModel,
        scope = modelScope,
        router = router,
        analyticsEventHandler = analyticsEventHandler,
        hotWalletFeatureToggles = hotWalletFeatureToggles,
        resolveCloudRestoreBackups = resolveCloudRestoreBackups,
        isLocked = ::isRowRunning,
    ) {

        override val optionsSheet = IntroductionProcess.SheetDismissed.Sheet.AddWallet

        override val importAnalyticsSource = AnalyticsParam.ScreensSources.AddNewWallet

        override val isSelectedWalletUnrelated = true

        val isOnImport: Boolean get() = isOnImportStep

        fun open() = showOptions()

        fun startImport() = openImportStep()

        override fun optionsStep() = HaveWalletUM.Step.Options(
            cardOrRing = WalletOptionUM(
                icon = WalletOptionUM.Icon.Tinted(Icons.ic_logo_tangem_20),
                title = resourceReference(CoreResR.string.introduction_create_wallet_hardware_title),
                description = resourceReference(CoreResR.string.introduction_i_have_wallet_hardware_description),
                isEnabled = !isBusy(),
                onClick = ::onAddCardClick,
            ),
            socialLogin = socialLoginHandler.provider?.let { provider ->
                WalletOptionUM(
                    icon = WalletOptionUM.Icon.Image(CoreUiR.drawable.img_google_32),
                    title = resourceReference(
                        id = CoreResR.string.introduction_i_have_wallet_social_title,
                        formatArgs = wrappedList(provider.displayName),
                    ),
                    description = resourceReference(CoreResR.string.introduction_create_wallet_social_description),
                    isEnabled = !isBusy(),
                    isLoading = activity.value == Activity.SigningInSocially,
                    isLoaderSpaceReserved = true,
                    onClick = { onSocialLoginClick(provider) },
                )
            },
            seedPhrase = WalletOptionUM(
                icon = WalletOptionUM.Icon.Tinted(Icons.ic_wallet_plus_20),
                title = resourceReference(CoreResR.string.introduction_create_wallet_seed_phrase_title),
                description = resourceReference(CoreResR.string.introduction_create_wallet_seed_phrase_description),
                isEnabled = activity.value != Activity.SigningInSocially,
                isLoading = activity.value == Activity.CreatingMobileWallet,
                onClick = ::onSeedPhraseClick,
            ),
            importWallet = importWalletOption(onClick = ::onImportWalletClick),
        )
    }

    private enum class Activity(val isRowRunning: Boolean) {
        Idle(isRowRunning = false),
        CheckingRestriction(isRowRunning = false),
        SigningInSocially(isRowRunning = true),
        CreatingMobileWallet(isRowRunning = true),
    }

    private companion object {
        val SHEET_COLLAPSE_TIMEOUT = 600.milliseconds
    }
}