package com.tangem.tap.features.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.common.keyboard.KeyboardValidator
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.event.TechAnalyticsEvent
import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.coil.ImagePreloader
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.BottomSheetMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.datasource.local.config.environment.EnvironmentConfigStorage
import com.tangem.domain.appcurrency.FetchAppCurrenciesUseCase
import com.tangem.domain.balancehiding.BalanceHidingSettings
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.balancehiding.ListenToFlipsUseCase
import com.tangem.domain.balancehiding.UpdateBalanceHidingSettingsUseCase
import com.tangem.domain.common.LogConfig
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.notifications.GetApplicationIdUseCase
import com.tangem.domain.notifications.SendPushTokenUseCase
import com.tangem.domain.notifications.models.ApplicationId
import com.tangem.domain.onramp.FetchHotCryptoUseCase
import com.tangem.domain.promo.GetStoryContentUseCase
import com.tangem.domain.promo.models.StoryContentIds
import com.tangem.domain.quotes.multi.MultiQuoteUpdater
import com.tangem.domain.settings.DeleteDeprecatedLogsUseCase
import com.tangem.domain.settings.IncrementAppLaunchCounterUseCase
import com.tangem.domain.settings.usercountry.FetchUserCountryUseCase
import com.tangem.domain.staking.FetchStakingTokensUseCase
import com.tangem.domain.wallets.usecase.AssociateWalletsWithApplicationIdUseCase
import com.tangem.domain.wallets.usecase.GetSavedWalletsCountUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletUseCase
import com.tangem.domain.wallets.usecase.UpdateRemoteWalletsInfoUseCase
import com.tangem.feature.swap.analytics.StoriesEvents
import com.tangem.tap.common.extensions.setContext
import com.tangem.tap.network.exchangeServices.ExchangeService
import com.tangem.tap.network.exchangeServices.moonpay.MoonPayService
import com.tangem.tap.proxy.AppStateHolder
import com.tangem.tap.routing.configurator.AppRouterConfig
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.wallet.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@Suppress("LongParameterList", "LargeClass")
@HiltViewModel
internal class MainViewModel @Inject constructor(
    private val updateBalanceHidingSettingsUseCase: UpdateBalanceHidingSettingsUseCase,
    private val listenToFlipsUseCase: ListenToFlipsUseCase,
    private val fetchAppCurrenciesUseCase: FetchAppCurrenciesUseCase,
    deleteDeprecatedLogsUseCase: DeleteDeprecatedLogsUseCase,
    private val incrementAppLaunchCounterUseCase: IncrementAppLaunchCounterUseCase,
    private val blockchainSDKFactory: BlockchainSDKFactory,
    private val dispatchers: CoroutineDispatcherProvider,
    private val fetchStakingTokensUseCase: FetchStakingTokensUseCase,
    private val fetchUserCountryUseCase: FetchUserCountryUseCase,
    @GlobalUiMessageSender private val messageSender: UiMessageSender,
    private val keyboardValidator: KeyboardValidator,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val getStoryContentUseCase: GetStoryContentUseCase,
    private val imagePreloader: ImagePreloader,
    private val fetchHotCryptoUseCase: FetchHotCryptoUseCase,
    private val getApplicationIdUseCase: GetApplicationIdUseCase,
    private val subscribeOnWalletsUseCase: GetSavedWalletsCountUseCase,
    private val associateWalletsWithApplicationIdUseCase: AssociateWalletsWithApplicationIdUseCase,
    private val updateRemoteWalletsInfoUseCase: UpdateRemoteWalletsInfoUseCase,
    private val sendPushTokenUseCase: SendPushTokenUseCase,
    private val apiConfigsManager: ApiConfigsManager,
    private val multiQuoteUpdater: MultiQuoteUpdater,
    private val appStateHolder: AppStateHolder,
    private val environmentConfigStorage: EnvironmentConfigStorage,
    private val getSelectedWalletUseCase: GetSelectedWalletUseCase,
    private val appRouterConfig: AppRouterConfig,
    getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
) : ViewModel() {

    private val balanceHidingSettingsFlow: SharedFlow<BalanceHidingSettings> = getBalanceHidingSettingsUseCase()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    var isSplashScreenShown: Boolean = true
        private set

    init {
        /**
         * Run any data initialization here that needs to happen before the app starts
         * and is hidden behind the SplashScreen
         */
        loadApplicationResources()

        /** Run any API data load here that runs in parallel and does not block the app from starting */
        launchAPIRequests {
            launch { fetchHotCryptoUseCase() }

            launch { fetchAppCurrenciesUseCase() }

            launch { fetchStakingTokens() }

            launch { initPushNotifications() }
        }

        viewModelScope.launch { incrementAppLaunchCounterUseCase() }

        multiQuoteUpdater.subscribe()

        initializeOffRamp()

        observeFlips()
        displayBalancesHidingStatusToast()
        displayHiddenBalancesModalNotification()

        deleteDeprecatedLogsUseCase()

        sendKeyboardIdentifierEvent()

        preloadImages()
    }

    override fun onCleared() {
        super.onCleared()

        multiQuoteUpdater.unsubscribe()
    }

    /** Loading the resources needed to run the application */
    private fun loadApplicationResources() {
        viewModelScope.launch {
            launchAPIRequests {
                launch { blockchainSDKFactory.init() }

                launch {
                    withTimeout(timeMillis = 1.seconds.inWholeMilliseconds) { fetchUserCountry() }
                }
            }

            prepareSelectedWalletFeedback()

            // await while initial route stack is initialized
            appRouterConfig.isInitialized.first { it }

            isSplashScreenShown = false
        }
    }

    private suspend fun fetchUserCountry() {
        fetchUserCountryUseCase().onLeft {
            Timber.e("Unable to fetch the user country code $it")
        }
    }

    private fun launchAPIRequests(function: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch {
            if (BuildConfig.TESTER_MENU_ENABLED) {
                apiConfigsManager.isInitialized
                    .filter { it }
                    .first() // wait until isInitialized becomes true

                function()
            } else {
                function()
            }
        }
    }

    private fun prepareSelectedWalletFeedback() {
        getSelectedWalletUseCase.invoke()
            .mapLeft { emptyFlow<UserWallet>() }
            .onRight {
                it.distinctUntilChanged()
                    .onEach { userWallet ->
                        Analytics.setContext(userWallet)
                    }
                    .flowOn(dispatchers.io)
                    .launchIn(viewModelScope)
            }
    }

    private suspend fun fetchStakingTokens() {
        fetchStakingTokensUseCase()
            .onLeft { Timber.e(it.toString(), "Unable to fetch the staking tokens list") }
            .onRight { Timber.d("Staking token list was fetched successfully") }
    }

    private fun initializeOffRamp() {
        viewModelScope.launch {
            val sellService = makeSellExchangeService(environmentConfig = environmentConfigStorage.getConfigSync())
            appStateHolder.sellService = sellService

            sellService.update()
        }
    }

    private fun makeSellExchangeService(environmentConfig: EnvironmentConfig): ExchangeService {
        return MoonPayService(
            apiKey = environmentConfig.moonPayApiKey,
            secretKey = environmentConfig.moonPayApiSecretKey,
            logEnabled = LogConfig.network.moonPayService,
            userWalletProvider = { getSelectedWalletUseCase.sync().getOrNull() },
        )
    }

    private fun observeFlips() {
        listenToFlipsUseCase().launchIn(viewModelScope)
    }

    private fun displayHiddenBalancesModalNotification() {
        balanceHidingSettingsFlow
            .drop(count = 1) // Skip initial emit
            .distinctUntilChanged()
            .filter {
                it.isBalanceHidingNotificationEnabled && it.isBalanceHidden
            }
            .onEach {
                if (!it.isUpdateFromToast) {
                    listenToFlipsUseCase.changeUpdateEnabled(false)

                    val message = BottomSheetMessage.invoke(
                        iconResId = R.drawable.ic_eye_off_outline_24,
                        title = resourceReference(R.string.balance_hidden_title),
                        message = resourceReference(R.string.balance_hidden_description),
                        onDismissRequest = ::onBottomSheetDismissed,
                        firstActionBuilder = {
                            EventMessageAction(
                                title = resourceReference(R.string.balance_hidden_got_it_button),
                                onClick = {
                                    onHiddenBalanceNotificationAction(isPermanent = false)
                                    onDismissRequest()
                                },
                            )
                        },
                        secondActionBuilder = {
                            EventMessageAction(
                                title = resourceReference(R.string.balance_hidden_do_not_show_button),
                                onClick = {
                                    onHiddenBalanceNotificationAction(isPermanent = true)
                                    onDismissRequest()
                                },
                            )
                        },
                    )
                    messageSender.send(message)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun displayBalancesHidingStatusToast() {
        var previousSettings: BalanceHidingSettings? = null

        balanceHidingSettingsFlow
            .onEach { settings ->
                if (previousSettings == null) {
                    previousSettings = settings
                    return@onEach
                }

                /*
                 * Skip if the feature to hide balances has just been
                 * enabled or disabled in the settings, or if the hide balances status has not been changed
                 * */
                if (settings.isHidingEnabledInSettings != previousSettings?.isHidingEnabledInSettings ||
                    settings.isBalanceHidden == previousSettings?.isBalanceHidden
                ) {
                    previousSettings = settings
                    return@onEach
                }

                if (!settings.isUpdateFromToast) {
                    displayBalancesHiddenStatusToast(settings)
                }

                previousSettings = settings
            }
            .launchIn(viewModelScope)
    }

    private fun displayBalancesHiddenStatusToast(settings: BalanceHidingSettings) {
        // If modal notification is enabled and balances are hidden, the toast will not show
        if (!settings.isBalanceHidingNotificationEnabled || !settings.isBalanceHidden) {
            val message = SnackbarMessage(
                message = if (settings.isBalanceHidden) {
                    resourceReference(R.string.toast_balances_hidden)
                } else {
                    resourceReference(R.string.toast_balances_shown)
                },
                actionLabel = resourceReference(R.string.toast_undo),
                action = {
                    if (settings.isBalanceHidden) {
                        onHiddenBalanceToastAction()
                    } else {
                        onShownBalanceToastAction()
                    }
                },
            )

            messageSender.send(message)
        }
    }

    private fun onHiddenBalanceToastAction() {
        viewModelScope.launch {
            updateBalanceHidingSettingsUseCase.invoke {
                copy(
                    isBalanceHidden = false,
                    isUpdateFromToast = true,
                )
            }
        }
    }

    private fun onShownBalanceToastAction() {
        viewModelScope.launch {
            updateBalanceHidingSettingsUseCase.invoke {
                copy(
                    isBalanceHidden = true,
                    isUpdateFromToast = true,
                )
            }
        }
    }

    private fun onHiddenBalanceNotificationAction(isPermanent: Boolean) {
        val message = SnackbarMessage(
            message = resourceReference(R.string.toast_balances_hidden),
            actionLabel = resourceReference(R.string.toast_undo),
            action = ::onHiddenBalanceToastAction,
        )
        messageSender.send(message)

        if (isPermanent) {
            viewModelScope.launch {
                updateBalanceHidingSettingsUseCase.invoke {
                    copy(isBalanceHidingNotificationEnabled = false)
                }
            }
        }
    }

    private fun onBottomSheetDismissed() {
        listenToFlipsUseCase.changeUpdateEnabled(isUpdateEnabled = true)
    }

    private fun sendKeyboardIdentifierEvent() {
        viewModelScope.launch {
            val keyboardId = keyboardValidator.getKeyboardId()

            if (keyboardId != null) {
                Timber.d("Keyboard ID: https://play.google.com/store/apps/details?id=${keyboardId.getPackageName()}")

                analyticsEventHandler.send(
                    event = TechAnalyticsEvent.KeyboardIdentifier(
                        id = keyboardId.value,
                        packageName = keyboardId.getPackageName(),
                        isTrusted = keyboardValidator.validate(id = keyboardId),
                    ),
                )
            } else {
                Timber.e("Unable to get keyboard identifier")
            }
        }
    }

    // Preload stories to display on startup before navigation to targeted screen
    private fun preloadImages() {
        try {
            viewModelScope.launch {
                val swapStories = getStoryContentUseCase.invokeSync(
                    id = StoryContentIds.STORY_FIRST_TIME_SWAP.id,
                    refresh = true,
                ).getOrNull()

                if (swapStories == null) {
                    analyticsEventHandler.send(
                        StoriesEvents.Error(
                            type = StoryContentIds.STORY_FIRST_TIME_SWAP.analyticType,
                        ),
                    )
                } else {
                    val storiesImages = swapStories.getImageUrls()
                    // try to preload images for stories
                    storiesImages.forEach(imagePreloader::preload)
                }
            }
        } catch (ex: Exception) {
            Timber.e(ex)
            analyticsEventHandler.send(
                StoriesEvents.Error(
                    type = StoryContentIds.STORY_FIRST_TIME_SWAP.analyticType,
                ),
            )
        }
    }

    private suspend fun initPushNotifications() {
        getApplicationIdUseCase()
            .onRight { applicationId ->
                sendPushTokenUseCase(applicationId = applicationId)
                associateWalletsWithApplicationId(applicationId = applicationId)
                updateRemoteWalletsInfoUseCase(applicationId = applicationId)
            }
            .onLeft(Timber::e)
    }

    private fun associateWalletsWithApplicationId(applicationId: ApplicationId) {
        subscribeOnWalletsUseCase()
            .onEach { wallets ->
                associateWalletsWithApplicationIdUseCase(applicationId, wallets)
            }
            .launchIn(viewModelScope)
    }
}