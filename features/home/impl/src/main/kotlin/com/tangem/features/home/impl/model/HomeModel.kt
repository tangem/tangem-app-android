package com.tangem.features.home.impl.model

import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.entity.InitScreenLaunchMode
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.SignInType
import com.tangem.core.analytics.models.Basic.SignedIn
import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.dialog.Dialogs
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.card.analytics.IntroductionProcess
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.error.SaveWalletError
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.settings.usercountry.GetUserCountryUseCase
import com.tangem.domain.settings.usercountry.models.UserCountry
import com.tangem.domain.settings.usercountry.models.needApplyFCARestrictions
import com.tangem.domain.wallets.builder.ColdUserWalletBuilder
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.feature.referral.domain.ShouldShowMobileWalletPromoUseCase
import com.tangem.features.home.api.HomeComponent
import com.tangem.features.home.api.HomeFeatureToggles
import com.tangem.features.home.impl.ui.state.HomeStoriesConfig
import com.tangem.features.home.impl.ui.state.HomeUM
import com.tangem.features.home.impl.ui.state.Stories
import com.tangem.features.home.impl.ui.state.getRestrictedStories
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.Debouncer
import com.tangem.utils.logging.TangemLogger
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

private const val HIDE_PROGRESS_DELAY = 400L

@Suppress("LongParameterList")
@ModelScoped
internal class HomeModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val scanCardProcessor: ScanCardProcessor,
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val settingsRepository: SettingsRepository,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val router: Router,
    private val getUserCountryUseCase: GetUserCountryUseCase,
    private val coldUserWalletBuilderFactory: ColdUserWalletBuilder.Factory,
    private val saveWalletUseCase: SaveWalletUseCase,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val shouldShowMobileWalletPromoUseCase: ShouldShowMobileWalletPromoUseCase,
    private val homeFeatureToggles: HomeFeatureToggles,
    @GlobalUiMessageSender private val uiMessageSender: UiMessageSender,
) : Model() {

    private val debouncer = Debouncer()

    val params = paramsContainer.require<HomeComponent.Params>()

    val uiState: StateFlow<HomeUM>
        field = MutableStateFlow(createInitialState())

    init {
        analyticsEventHandler.send(IntroductionProcess.ScreenOpened())
        observeUserCountryChanges()

        when (params.launchMode) {
            InitScreenLaunchMode.Standard -> Unit
            InitScreenLaunchMode.WithCardScan -> scanCard()
        }
    }

    private fun createInitialState(): HomeUM {
        val initialStories = getRestrictedStories().toImmutableList()
        return HomeUM(
            scanInProgress = false,
            isStoriesContainerEnabled = homeFeatureToggles.isStoriesContainerEnabled,
            stories = initialStories,
            storiesConfig = HomeStoriesConfig(stories = initialStories),
            onGetStartedClick = ::onGetStartedClick,
        )
    }

    private fun observeUserCountryChanges() {
        getUserCountryUseCase.invoke()
            .distinctUntilChanged()
            .filterNotNull()
            .onEach { result ->
                val userCountry = result.getOrNull() ?: UserCountry.Other(Locale.getDefault().country)
                updateStoriesForCountry(userCountry)
            }
            .flowOn(dispatchers.io)
            .launchIn(modelScope)
    }

    private fun updateStoriesForCountry(userCountry: UserCountry) {
        val stories = if (userCountry.needApplyFCARestrictions()) {
            getRestrictedStories()
        } else {
            Stories.entries
        }
            .toImmutableList()

        uiState.update {
            it.copy(stories = stories, storiesConfig = HomeStoriesConfig(stories = stories))
        }
    }

    private fun onGetStartedClick() {
        debouncer.debounce(modelScope) {
            val mode = if (shouldShowMobileWalletPromoUseCase()) {
                AppRoute.CreateWalletStart.Mode.HotWallet
            } else {
                AppRoute.CreateWalletStart.Mode.ColdWallet
            }
            router.push(AppRoute.CreateWalletStart(mode = mode))
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
                shouldCheckIsAlreadyActivated = true,
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
            TangemLogger.e("User wallet not created")
            setLoading(false)
            return
        }

        saveWalletUseCase(
            userWallet = userWallet,
            analyticsSource = AnalyticsParam.ScreensSources.Intro,
        ).fold(
            ifLeft = { error ->
                delay(HIDE_PROGRESS_DELAY)
                setLoading(false)
                when (error) {
                    is SaveWalletError.DataError -> TangemLogger.e("Unable to save user wallet: $error")
                    is SaveWalletError.WalletAlreadySaved -> router.replaceAll(AppRoute.Wallet)
                }
            },
            ifRight = {
                setLoading(false)
                sendSignedInCardAnalyticsEvent(scanResponse, userWallet.isImported)
                router.replaceAll(AppRoute.Wallet)
            },
        )
    }

    private suspend fun sendSignedInCardAnalyticsEvent(scanResponse: ScanResponse, isImported: Boolean) {
        analyticsEventHandler.send(
            SignedIn(
                signInType = SignInType.Card,
                walletsCount = userWalletsListRepository.userWalletsSync().size,
                isImported = isImported,
                isBackedUp = scanResponse.card.backupStatus?.isActive == true,
            ),
        )
    }

    private fun setLoading(isLoading: Boolean) {
        uiState.update { it.copy(scanInProgress = isLoading) }
    }

    private fun handleScanError(error: TangemError) {
        when (error) {
            is TangemSdkError.NfcFeatureIsUnavailable -> handleNfcFeatureUnavailable()
            is TangemSdkError -> TangemLogger.e("Scan error occurred", error)
            else -> TangemLogger.e("Error happened", error)
        }
    }

    private fun handleNfcFeatureUnavailable() {
        uiMessageSender.send(Dialogs.nfcFeatureUnavailable())
    }
}