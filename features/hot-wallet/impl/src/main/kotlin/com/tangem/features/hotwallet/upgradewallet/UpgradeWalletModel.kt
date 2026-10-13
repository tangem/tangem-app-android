package com.tangem.features.hotwallet.upgradewallet

import com.tangem.common.core.TangemError
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.card.analytics.IntroductionProcess
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.card.models.ScanConfig
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.sociallogin.usecase.IsSocialBackupCompletedUseCase
import com.tangem.domain.wallets.analytics.WalletSettingsAnalyticEvents
import com.tangem.domain.wallets.builder.ColdUserWalletBuilder
import com.tangem.domain.wallets.usecase.ClearHotWalletContextualUnlockUseCase
import com.tangem.domain.wallets.usecase.GenerateBuyTangemCardLinkUseCase
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.features.hotwallet.UpgradeWalletComponent
import com.tangem.features.hotwallet.upgradewallet.entity.UpgradeWalletUM
import com.tangem.features.onboarding.v2.util.ResetCardsComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.DELAY_SDK_DIALOG_CLOSE
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class UpgradeWalletModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val generateBuyTangemCardLinkUseCase: GenerateBuyTangemCardLinkUseCase,
    private val urlOpener: UrlOpener,
    private val settingsRepository: SettingsRepository,
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val uiMessageSender: UiMessageSender,
    private val clearHotWalletContextualUnlockUseCase: ClearHotWalletContextualUnlockUseCase,
    private val scanCardProcessor: ScanCardProcessor,
    private val coldUserWalletBuilderFactory: ColdUserWalletBuilder.Factory,
    private val trackingContextProxy: TrackingContextProxy,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val hotWalletFeatureToggles: HotWalletFeatureToggles,
    private val isSocialBackupCompletedUseCase: IsSocialBackupCompletedUseCase,
) : Model() {
    private val params = paramsContainer.require<UpgradeWalletComponent.Params>()

    val resetCardsComponentCallbacks = ResetCardsModelCallbacks()
    val startResetCardsFlow = MutableSharedFlow<UserWallet.Cold>()

    internal val uiState: StateFlow<UpgradeWalletUM>
        field = MutableStateFlow(
            UpgradeWalletUM(
                onBackClick = { router.pop() },
                onBuyTangemWalletClick = ::onBuyTangemWalletClick,
                onContinueClick = ::onContinueClick,
                isCloudBackupRemovalShown = hotWalletFeatureToggles.isGoogleDriveBackupEnabled,
            ),
        )

    init {
        trackingContextProxy.addHotWalletContext()
        analyticsEventHandler.send(WalletSettingsAnalyticEvents.HardwareUpgradeScreenOpened())
        modelScope.launch {
            if (isSocialBackupCompletedUseCase(params.userWalletId)) {
                uiState.update { it.copy(isSocialLoginWallet = true) }
            }
        }
    }

    override fun onDestroy() {
        trackingContextProxy.removeContext()
        clearHotWalletContextualUnlockUseCase.invoke(params.userWalletId)
        super.onDestroy()
    }

    private fun onBuyTangemWalletClick() {
        analyticsEventHandler.send(Basic.ButtonBuy(source = AnalyticsParam.ScreensSources.Upgrade))
        modelScope.launch {
            generateBuyTangemCardLinkUseCase
                .invoke(GenerateBuyTangemCardLinkUseCase.Source.Upgrade).let { urlOpener.openUrl(it) }
        }
    }

    private fun onContinueClick() {
        analyticsEventHandler.send(IntroductionProcess.ButtonScanCard(AnalyticsParam.ScreensSources.Upgrade))
        analyticsEventHandler.send(WalletSettingsAnalyticEvents.ButtonStartUpgrade())
        scanCard()
    }

    private fun scanCard() {
        modelScope.launch {
            setLoading(true)

            val shouldSaveAccessCodes = settingsRepository.shouldSaveAccessCodes()
            cardSdkConfigRepository.setAccessCodeRequestPolicy(
                isBiometricsRequestPolicy = shouldSaveAccessCodes,
            )

            scanCardProcessor.scan(
                config = ScanConfig(
                    analyticsSource = AnalyticsParam.ScreensSources.Upgrade,
                    shouldConfirmWalletOwnership = true,
                    canRequestAccessCodeFromRepository = false,
                    shouldUpdateCardIdDisplayFormat = true,
                    shouldTrackScanFails = true,
                    shouldLaunchOnboarding = false,
                    callback = createScanCallback(),
                ),
            )
        }
    }

    private fun createScanCallback(): ScanConfig.Callback = object : ScanConfig.Callback {
        override suspend fun onProgressStateChange(showProgress: Boolean) {
            setLoading(showProgress)
        }

        override suspend fun onFailure(error: TangemError) {
            setLoading(false)
        }

        override suspend fun onSuccess(scanResponse: ScanResponse) {
            checkIsWalletSuitableToBeUsedAsUpgrade(scanResponse = scanResponse) {
                delay(DELAY_SDK_DIALOG_CLOSE)
                navigateToUpgradeFlow(scanResponse)
            }
            setLoading(false)
        }
    }

    private fun setLoading(isLoading: Boolean) {
        uiState.update { it.copy(isLoading = isLoading) }
    }

    private suspend fun checkIsWalletSuitableToBeUsedAsUpgrade(
        scanResponse: ScanResponse,
        onSuccess: suspend () -> Unit,
    ) {
        val userWallet = coldUserWalletBuilderFactory.create(scanResponse).build()

        if (userWallet?.walletId == params.userWalletId) {
            startResetCardsFlow.emit(userWallet)
            return
        }

        // Check for the correct card type
        when {
            scanResponse.card.wallets.isNotEmpty() -> uiMessageSender.send(
                DialogMessage(resourceReference(R.string.hw_upgrade_error_card_already_has_wallet)),
            )
            scanResponse.cardTypesResolver.isWallet2().not() -> uiMessageSender.send(
                DialogMessage(resourceReference(R.string.hw_upgrade_error_wallet2_card_required)),
            )
            scanResponse.card.settings.isKeysImportAllowed.not() -> uiMessageSender.send(
                DialogMessage(resourceReference(R.string.hw_upgrade_error_card_key_import)),
            )
            else -> onSuccess()
        }
    }

    private fun navigateToUpgradeFlow(scanResponse: ScanResponse) {
        setLoading(false)
        router.push(
            AppRoute.Onboarding(
                scanResponse = scanResponse,
                mode = AppRoute.Onboarding.Mode.UpgradeHotWallet(
                    userWalletId = params.userWalletId,
                ),
            ),
        )
    }

    inner class ResetCardsModelCallbacks : ResetCardsComponent.ModelCallbacks {
        override fun onCancel() {
            setLoading(false)
        }

        override fun onComplete() {
            scanCard()
        }
    }
}