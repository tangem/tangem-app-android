package com.tangem.features.home.impl.model

import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRoute.ManageTokens.Source
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.entity.InitScreenLaunchMode
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic.SignedIn
import com.tangem.core.analytics.models.Basic.SignedIn.SignInType
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
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.card.analytics.IntroductionProcess
import com.tangem.domain.card.analytics.ParamCardCurrencyConverter
import com.tangem.domain.card.analytics.Shop
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.core.wallets.error.SaveWalletError
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.settings.usercountry.GetUserCountryUseCase
import com.tangem.domain.settings.usercountry.models.UserCountry
import com.tangem.domain.settings.usercountry.models.needApplyFCARestrictions
import com.tangem.domain.wallets.builder.ColdUserWalletBuilder
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.usecase.GenerateBuyTangemCardLinkUseCase
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.features.home.api.HomeComponent
import com.tangem.features.home.impl.ui.state.HomeUM
import com.tangem.features.home.impl.ui.state.Stories
import com.tangem.features.home.impl.ui.state.getRestrictedStories
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
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
    private val appRouter: AppRouter,
    private val getUserCountryUseCase: GetUserCountryUseCase,
    private val coldUserWalletBuilderFactory: ColdUserWalletBuilder.Factory,
    private val saveWalletUseCase: SaveWalletUseCase,
    private val generateBuyTangemCardLinkUseCase: GenerateBuyTangemCardLinkUseCase,
    private val urlOpener: UrlOpener,
    private val userWalletsListManager: UserWalletsListManager,
    @GlobalUiMessageSender private val uiMessageSender: UiMessageSender,
) : Model() {

    val params = paramsContainer.require<HomeComponent.Params>()

    private val _uiState = MutableStateFlow(
        HomeUM(
            scanInProgress = false,
            stories = getRestrictedStories().toImmutableList(),
            onScanClick = ::onScanClick,
            onShopClick = ::onShopClick,
            onSearchTokensClick = ::onSearchTokensClick,
            onCreateNewWalletClick = ::onCreateNewWalletClick,
            onAddExistingWalletClick = ::onAddExistingWalletClick,
        ),
    )

    val uiState = _uiState.asStateFlow()

    init {
        analyticsEventHandler.send(IntroductionProcess.ScreenOpened)
        observeUserCountryChanges()

        when (params.launchMode) {
            InitScreenLaunchMode.Standard -> Unit
            InitScreenLaunchMode.WithCardScan -> scanCard()
        }
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

        _uiState.update {
            it.copy(stories = stories.toImmutableList())
        }
    }

    private fun onScanClick() {
        analyticsEventHandler.send(IntroductionProcess.ButtonScanCard)
        scanCard()
    }

    private fun onShopClick() {
        analyticsEventHandler.send(IntroductionProcess.ButtonBuyCards)
        analyticsEventHandler.send(Shop.ScreenOpened)
        modelScope.launch {
            generateBuyTangemCardLinkUseCase.invoke().let { urlOpener.openUrl(it) }
        }
    }

    private fun onSearchTokensClick() {
        analyticsEventHandler.send(IntroductionProcess.ButtonTokensList)
        router.push(AppRoute.ManageTokens(Source.STORIES))
    }

    private fun onCreateNewWalletClick() {
        router.push(AppRoute.CreateWalletSelection)
    }

    private fun onAddExistingWalletClick() {
        router.push(AppRoute.AddExistingWallet)
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

        saveWalletUseCase(userWallet).fold(
            ifLeft = {
                delay(HIDE_PROGRESS_DELAY)
                setLoading(false)
                when (it) {
                    is SaveWalletError.DataError -> Timber.e(it.toString(), "Unable to save user wallet")
                    is SaveWalletError.WalletAlreadySaved -> appRouter.replaceAll(AppRoute.Wallet)
                }
            },
            ifRight = {
                setLoading(false)
                sendSignedInCardAnalyticsEvent(scanResponse)
                appRouter.replaceAll(AppRoute.Wallet)
            },
        )
    }

    private fun sendSignedInCardAnalyticsEvent(scanResponse: ScanResponse) {
        val currency = ParamCardCurrencyConverter().convert(value = scanResponse.cardTypesResolver)
        if (currency != null) {
            analyticsEventHandler.send(
                SignedIn(
                    currency = currency,
                    batch = scanResponse.card.batchId,
                    signInType = SignInType.Card,
                    walletsCount = userWalletsListManager.walletsCount.toString(),
                    hasBackup = scanResponse.card.backupStatus?.isActive,
                ),
            )
        }
    }

    private fun setLoading(isLoading: Boolean) {
        _uiState.update { it.copy(scanInProgress = isLoading) }
    }

    fun handleScanError(error: TangemError) {
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