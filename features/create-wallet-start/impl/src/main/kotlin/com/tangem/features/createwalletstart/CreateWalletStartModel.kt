package com.tangem.features.createwalletstart

import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.analytics.models.event.OnboardingAnalyticsEvent
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.dialog.Dialogs.hotWalletCreationNotSupportedDialog
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.card.analytics.IntroductionProcess
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.error.SaveWalletError
import com.tangem.domain.hotwallet.IsHotWalletCreationSupported
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.wallets.builder.ColdUserWalletBuilder
import com.tangem.domain.wallets.usecase.GenerateBuyTangemCardLinkUseCase
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.features.createwalletstart.entity.CreateWalletStartUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private const val HIDE_PROGRESS_DELAY = 400L

@Suppress("LongParameterList")
@ModelScoped
internal class CreateWalletStartModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val scanCardProcessor: ScanCardProcessor,
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val settingsRepository: SettingsRepository,
    private val appRouter: AppRouter,
    private val coldUserWalletBuilderFactory: ColdUserWalletBuilder.Factory,
    private val saveWalletUseCase: SaveWalletUseCase,
    private val isHotWalletCreationSupported: IsHotWalletCreationSupported,
    private val userWalletsListRepository: UserWalletsListRepository,
    @GlobalUiMessageSender private val uiMessageSender: UiMessageSender,
    private val generateBuyTangemCardLinkUseCase: GenerateBuyTangemCardLinkUseCase,
    private val urlOpener: UrlOpener,
    private val trackingContextProxy: TrackingContextProxy,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model() {

    private val params = paramsContainer.require<CreateWalletStartComponent.Params>()

    internal val uiState: StateFlow<CreateWalletStartUM>
        field = MutableStateFlow(
            when (params.mode) {
                CreateWalletStartComponent.Mode.ColdWallet -> CreateWalletStartUM(
                    title = resourceReference(R.string.common_tangem_wallet),
                    description = resourceReference(R.string.welcome_create_wallet_hardware_description),
                    featureItems = persistentListOf(
                        CreateWalletStartUM.FeatureItem(
                            iconResId = R.drawable.ic_shield_check_16,
                            text = resourceReference(R.string.welcome_create_wallet_feature_class),
                        ),
                        CreateWalletStartUM.FeatureItem(
                            iconResId = R.drawable.ic_flash_16,
                            text = resourceReference(R.string.welcome_create_wallet_feature_delivery),
                        ),
                        CreateWalletStartUM.FeatureItem(
                            iconResId = R.drawable.ic_sparkles_16,
                            text = resourceReference(R.string.welcome_create_wallet_feature_use),
                        ),
                    ),
                    imageResId = R.drawable.img_hardware_wallet,
                    shouldShowScanSecondaryButton = true,
                    onPrimaryButtonClick = ::onBuyClick,
                    primaryButtonText = resourceReference(R.string.details_buy_wallet),
                    otherMethodTitle = resourceReference(R.string.welcome_create_wallet_mobile_title),
                    otherMethodDescription = resourceReference(R.string.welcome_create_wallet_mobile_description),
                    otherMethodClick = ::onStartWithMobileWalletClick,
                    onBackClick = { router.pop() },
                    onScanClick = ::onScanClick,
                    isScanInProgress = false,
                )
                CreateWalletStartComponent.Mode.HotWallet -> CreateWalletStartUM(
                    title = resourceReference(R.string.hw_mobile_wallet),
                    description = resourceReference(R.string.welcome_create_wallet_mobile_description_full),
                    featureItems = persistentListOf(
                        CreateWalletStartUM.FeatureItem(
                            iconResId = R.drawable.ic_shield_check_16,
                            text = resourceReference(R.string.welcome_create_wallet_feature_seamless),
                        ),
                        CreateWalletStartUM.FeatureItem(
                            iconResId = R.drawable.ic_flash_16,
                            text = resourceReference(R.string.welcome_create_wallet_feature_one_tap),
                        ),
                        CreateWalletStartUM.FeatureItem(
                            iconResId = R.drawable.ic_stack_fill_new_16,
                            text = resourceReference(R.string.welcome_create_wallet_feature_assets),
                        ),
                    ),
                    imageResId = R.drawable.img_mobile_wallet,
                    shouldShowScanSecondaryButton = false,
                    onPrimaryButtonClick = ::onStartWithMobileWalletClick,
                    primaryButtonText = resourceReference(R.string.welcome_create_wallet_mobile_title),
                    otherMethodTitle = resourceReference(R.string.welcome_create_wallet_use_hardware_title),
                    otherMethodDescription = resourceReference(R.string.welcome_create_wallet_use_hardware_description),
                    otherMethodClick = ::onBuyClick,
                    onBackClick = { router.pop() },
                    onScanClick = ::onScanClick,
                    isScanInProgress = false,
                )
            },
        )

    init {
        analyticsEventHandler.send(
            event = IntroductionProcess.CreateWalletIntroScreenOpened(),
        )
    }

    private fun onScanClick() {
        analyticsEventHandler.send(
            event = IntroductionProcess.ButtonScanCard(AnalyticsParam.ScreensSources.CreateWalletIntro),
        )
        scanCard()
    }

    private fun onStartWithMobileWalletClick() {
        trackingContextProxy.addHotWalletContext()
        analyticsEventHandler.send(
            event = OnboardingAnalyticsEvent.Onboarding.ButtonMobileWallet(
                source = AnalyticsParam.ScreensSources.CreateWalletIntro.value,
            ),
        )
        if (!isHotWalletCreationSupported()) {
            uiMessageSender.send(
                hotWalletCreationNotSupportedDialog(isHotWalletCreationSupported.getLeastVersionName()),
            )
            return
        }

        router.push(AppRoute.CreateMobileWallet(AnalyticsParam.ScreensSources.CreateWalletIntro.value))
    }

    private fun onBuyClick() {
        analyticsEventHandler.send(Basic.ButtonBuy(source = AnalyticsParam.ScreensSources.CreateWalletIntro))
        modelScope.launch {
            generateBuyTangemCardLinkUseCase
                .invoke(GenerateBuyTangemCardLinkUseCase.Source.Creation).let { urlOpener.openUrl(it) }
        }
    }

    private fun scanCard() {
        modelScope.launch {
            setLoading(true)

            val shouldSaveAccessCodes = settingsRepository.shouldSaveAccessCodes()
            cardSdkConfigRepository.setAccessCodeRequestPolicy(
                isBiometricsRequestPolicy = shouldSaveAccessCodes,
            )

            val analyticsSource = AnalyticsParam.ScreensSources.Intro

            scanCardProcessor.scan(
                analyticsSource = analyticsSource,
                onProgressStateChange = { showProgress ->
                    if (!showProgress) {
                        delay(HIDE_PROGRESS_DELAY)
                        setLoading(false)
                    } else {
                        setLoading(true)
                    }
                },
                onFailure = { error ->
                    handleScanError(error)
                    delay(HIDE_PROGRESS_DELAY)
                    setLoading(false)
                },
                onSuccess = { scanResponse ->
                    proceedWithScanResponse(scanResponse)
                },
            )
        }
    }

    private suspend fun proceedWithScanResponse(scanResponse: ScanResponse) {
        val userWallet = coldUserWalletBuilderFactory.create(scanResponse = scanResponse).build()

        if (userWallet == null) {
            Timber.e("User wallet not created")
            setLoading(false)
            return
        }

        saveWalletUseCase(userWallet = userWallet).fold(
            ifLeft = { error ->
                delay(HIDE_PROGRESS_DELAY)
                setLoading(false)
                when (error) {
                    is SaveWalletError.DataError -> Timber.e(error.toString(), "Unable to save user wallet")
                    is SaveWalletError.WalletAlreadySaved -> {
                        userWalletsListRepository.unlock(
                            userWalletId = userWallet.walletId,
                            unlockMethod = UserWalletsListRepository.UnlockMethod.Scan(scanResponse),
                        ).onRight {
                            appRouter.replaceAll(AppRoute.Wallet)
                        }
                    }
                }
            },
            ifRight = {
                setLoading(false)
                appRouter.replaceAll(AppRoute.Wallet)
            },
        )
    }

    private fun setLoading(isLoading: Boolean) {
        uiState.update { it.copy(isScanInProgress = isLoading) }
    }

    private fun handleScanError(error: TangemError) {
        when (error) {
            is TangemSdkError.NfcFeatureIsUnavailable -> handleNfcFeatureUnavailable()
            is TangemSdkError -> Timber.e(error, "Scan error occurred")
            else -> Timber.e(error, "Error happened")
        }
    }

    private fun handleNfcFeatureUnavailable() {
        uiMessageSender.send(
            message = DialogMessage(
                message = resourceReference(R.string.nfc_error_unavailable),
                title = resourceReference(id = R.string.common_error),
            ),
        )
    }
}