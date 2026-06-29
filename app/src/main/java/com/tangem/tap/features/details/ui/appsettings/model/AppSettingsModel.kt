package com.tangem.tap.features.details.ui.appsettings.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.settings.SettingsManager
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.appcurrency.repository.AppCurrencyRepository
import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.domain.apptheme.repository.AppThemeModeRepository
import com.tangem.domain.balancehiding.repositories.BalanceHidingRepository
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Settings
import com.tangem.tap.features.details.ui.appsettings.AppSettingsDialogsFactory
import com.tangem.tap.features.details.ui.appsettings.AppSettingsItemsFactory
import com.tangem.tap.features.details.ui.appsettings.AppSettingsScreenState
import com.tangem.tap.features.details.ui.appsettings.analytics.AppSettingsItemsAnalyticsSender
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.extensions.addIf
import com.tangem.utils.logging.TangemLogger
import com.tangem.wallet.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList", "LargeClass")
@Stable
@ModelScoped
internal class AppSettingsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    appCurrencyRepository: AppCurrencyRepository,
    private val walletsRepository: WalletsRepository,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val balanceHidingRepository: BalanceHidingRepository,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val appThemeModeRepository: AppThemeModeRepository,
    private val appSettingsItemsAnalyticsSender: AppSettingsItemsAnalyticsSender,
    private val tangemSdkManager: TangemSdkManager,
    private val settingsManager: SettingsManager,
    private val settingsRepository: SettingsRepository,
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val tangemHotSdk: TangemHotSdk,
    private val router: Router,
    private val uiMessageSender: UiMessageSender,
) : Model() {

    private val itemsFactory = AppSettingsItemsFactory()
    private val dialogsFactory = AppSettingsDialogsFactory()

    val dialogNavigation: SlotNavigation<AppSettingsDialogConfig> = SlotNavigation()

    private val localState = MutableStateFlow(LocalState())
    private val biometricsStatusJobHolder = JobHolder()

    val uiState: StateFlow<AppSettingsScreenState>
        field = MutableStateFlow<AppSettingsScreenState>(value = AppSettingsScreenState.Loading)

    init {
        bootstrapLocalState()

        combine(
            flow = appCurrencyRepository.getSelectedAppCurrency().distinctUntilChanged(),
            flow2 = appThemeModeRepository.getAppThemeMode(),
            flow3 = balanceHidingRepository.getBalanceHidingSettingsFlow(),
            flow4 = localState,
        ) { currency, themeMode, hidingSettings, local ->
            AppSettingsState(
                appCurrency = currency,
                themeMode = themeMode,
                isHidingEnabled = hidingSettings.isHidingEnabledInSettings,
                local = local,
            )
        }
            .onEach { state ->
                val items = buildItems(state)
                uiState.update { prevState ->
                    when (prevState) {
                        is AppSettingsScreenState.Content -> prevState.copy(items = items)
                        is AppSettingsScreenState.Loading -> AppSettingsScreenState.Content(items = items)
                    }
                }
            }
            .launchIn(modelScope)

        sendItemsAnalytics()
    }

    fun onResume() {
        observeBiometricsStatusChanges()
    }

    private fun observeBiometricsStatusChanges() {
        flow {
            do {
                val isEnrollBiometricsNeeded = runCatching(tangemSdkManager::isEnrollBiometricsNeeded).getOrNull()
                if (isEnrollBiometricsNeeded != null) {
                    emit(isEnrollBiometricsNeeded)
                }
                delay(timeMillis = 200)
            } while (true)
        }
            .flowOn(dispatchers.default)
            .distinctUntilChanged()
            .onEach { isEnrollBiometricsNeeded ->
                localState.update { it.copy(isEnrollBiometricsNeeded = isEnrollBiometricsNeeded) }
            }
            .launchIn(modelScope)
            .saveIn(biometricsStatusJobHolder)
    }

    private fun buildItems(state: AppSettingsState): ImmutableList<AppSettingsScreenState.Item> {
        val items = buildList {
            addIf(
                condition = state.local.isEnrollBiometricsNeeded,
                element = itemsFactory.createEnrollBiometricsCard(onClick = ::enrollBiometrics),
            )

            add(
                itemsFactory.createSelectAppCurrencyButton(
                    currentAppCurrencyName = state.appCurrency.name,
                    onClick = ::showAppCurrencySelector,
                ),
            )

            val canUseBiometrics = with(state.local) {
                !isEnrollBiometricsNeeded && !isInProgress && hasSecuredWallets
            }

            add(
                itemsFactory.createUseBiometricsSwitch(
                    isChecked = state.local.isBiometricAuthenticationUsed,
                    isEnabled = canUseBiometrics,
                    onCheckedChange = ::onBiometricAuthenticationToggled,
                    onDisabledClick = ::onBiometricAuthenticationDisabledClicked,
                ),
            )

            add(
                itemsFactory.createRequireAccessCodeSwitch(
                    isChecked = state.local.isAccessCodeRequired || !state.local.isBiometricAuthenticationUsed,
                    isEnabled = canUseBiometrics && state.local.isBiometricAuthenticationUsed,
                    onCheckedChange = ::onRequireAccessCodeToggled,
                ),
            )

            add(
                itemsFactory.createFlipToHideBalanceSwitch(
                    isChecked = state.isHidingEnabled,
                    isEnabled = true,
                    onCheckedChange = ::onFlipToHideBalanceToggled,
                ),
            )

            add(
                itemsFactory.createSelectThemeModeButton(
                    currentThemeMode = state.themeMode,
                    onClick = { showThemeModeSelector(state.themeMode) },
                ),
            )
        }

        return items.toImmutableList()
    }

    private fun enrollBiometrics() {
        analyticsEventHandler.send(Settings.AppSettings.ButtonEnableBiometricAuthentication())
        settingsManager.openBiometricSettings()
    }

    fun onBackClick() {
        router.pop()
    }

    private fun showAppCurrencySelector() {
        router.push(AppRoute.AppCurrencySelector)
    }

    private fun showThemeModeSelector(selectedMode: AppThemeMode) {
        dialogNavigation.activate(AppSettingsDialogConfig.ThemeModeSelector(selectedMode.ordinal))
    }

    fun onThemeModeSelected(index: Int) {
        val mode = AppThemeMode.available[index]
        analyticsEventHandler.send(
            event = Settings.AppSettings.ThemeSwitched(
                theme = AnalyticsParam.AppTheme.fromAppThemeMode(mode),
            ),
        )
        changeAppThemeMode(mode)
        dialogNavigation.dismiss()
    }

    fun dismissDialog() {
        dialogNavigation.dismiss()
    }

    private fun onBiometricAuthenticationToggled(isChecked: Boolean) {
        // TODO : Uncomment and implement analytics event when ready
        // val param = AnalyticsParam.OnOffState(isChecked)
        // analyticsEventHandler.send(Settings.AppSettings.BiometricAuthenticationChanged(param))
        if (isChecked) {
            toggleBiometricsAuthentication(enable = true)
        } else {
            uiMessageSender.send(
                dialogsFactory.createDisableBiometricAuthenticationAlert(
                    onDisable = { toggleBiometricsAuthentication(enable = false) },
                ),
            )
        }
    }

    private fun onBiometricAuthenticationDisabledClicked() {
        uiMessageSender.send(DialogMessage(message = resourceReference(R.string.app_settings_access_code_warning)))
    }

    private fun onRequireAccessCodeToggled(isChecked: Boolean) {
        // TODO : Uncomment and implement analytics event when ready
        // val param = AnalyticsParam.OnOffState(isChecked)
        // analyticsEventHandler.send(Settings.AppSettings.RequireAccessCodeChanged(param))
        if (isChecked) {
            uiMessageSender.send(
                dialogsFactory.createEnableRequireAccessCodeAlert(
                    onEnable = { toggleRequireAccessCode(enable = true) },
                ),
            )
        } else {
            uiMessageSender.send(
                dialogsFactory.createDisableRequireAccessCodeAlert(
                    onDisable = { toggleRequireAccessCode(enable = false) },
                ),
            )
        }
    }

    private fun toggleBiometricsAuthentication(enable: Boolean) {
        localState.update { it.copy(isBiometricAuthenticationUsed = enable, isInProgress = true) }

        modelScope.launch {
            // Nothing to change
            if (walletsRepository.useBiometricAuthentication() == enable) {
                localState.update { it.copy(isInProgress = false) }
                return@launch
            }

            if (enable) {
                setBiometricLockForAllWallets()
            } else {
                removeAllBiometricData()
                walletsRepository.setRequireAccessCode(value = true)
                localState.update { it.copy(isAccessCodeRequired = true) }
            }

            walletsRepository.setUseBiometricAuthentication(value = enable)
            localState.update { it.copy(isInProgress = false) }
        }
    }

    private fun toggleRequireAccessCode(enable: Boolean) {
        localState.update { it.copy(isAccessCodeRequired = enable, isInProgress = true) }

        modelScope.launch {
            // Nothing to change
            if (walletsRepository.requireAccessCode() == enable) {
                localState.update { it.copy(isInProgress = false) }
                return@launch
            }

            if (enable) {
                removeAllBiometricSingData(userWalletsListRepository.userWalletsSync())
            }

            walletsRepository.setRequireAccessCode(value = enable)
            localState.update { it.copy(isInProgress = false) }
        }
    }

    private suspend fun setBiometricLockForAllWallets() {
        val userWallets = userWalletsListRepository.userWalletsSync()
        userWallets.forEach { wallet ->
            userWalletsListRepository.setLock(
                userWalletId = wallet.walletId,
                lockMethod = UserWalletsListRepository.LockMethod.Biometric,
                changeUnsecured = false,
            )
        }
    }

    private suspend fun removeAllBiometricData() {
        val userWallets = userWalletsListRepository.userWalletsSync()
        userWallets.forEach {
            userWalletsListRepository.removeBiometricLock(it.walletId)
        }
        removeAllBiometricSingData(userWallets)
    }

    private suspend fun removeAllBiometricSingData(userWallets: List<UserWallet>) {
        deleteSavedAccessCodes()
        userWallets.forEach { wallet ->
            if (wallet is UserWallet.Hot) {
                userWalletsListRepository.saveWithoutLock(
                    userWallet = wallet.copy(
                        hotWalletId = tangemHotSdk.removeBiometryAuthIfPresented(wallet.hotWalletId),
                    ),
                )
            }
        }
    }

    private suspend fun deleteSavedAccessCodes() {
        tangemSdkManager.clearSavedUserCodes()
            .doOnSuccess {
                analyticsEventHandler.send(
                    Settings.AppSettings.SaveAccessCodeSwitcherChanged(AnalyticsParam.OnOffState.Off),
                )
                settingsRepository.setShouldSaveAccessCodes(value = false)
                cardSdkConfigRepository.setAccessCodeRequestPolicy(isBiometricsRequestPolicy = false)
            }
            .doOnFailure { error ->
                TangemLogger.e("Unable to delete saved access codes", error)
            }
    }

    private fun onFlipToHideBalanceToggled(enable: Boolean) {
        val param = AnalyticsParam.OnOffState(enable)
        analyticsEventHandler.send(Settings.AppSettings.HideBalanceChanged(param))

        modelScope.launch {
            val settings = balanceHidingRepository.getBalanceHidingSettings().copy(
                isHidingEnabledInSettings = enable,
                isBalanceHidden = false,
            )
            balanceHidingRepository.storeBalanceHidingSettings(settings)
        }
    }

    private fun changeAppThemeMode(mode: AppThemeMode) {
        modelScope.launch {
            appThemeModeRepository.changeAppThemeMode(mode)
        }
    }

    private fun bootstrapLocalState() = modelScope.launch {
        localState.update { state ->
            state.copy(
                hasSecuredWallets = userWalletsListRepository.hasSecuredWallets(),
                isEnrollBiometricsNeeded = runCatching(tangemSdkManager::isEnrollBiometricsNeeded).getOrNull() == true,
                isBiometricAuthenticationUsed = walletsRepository.useBiometricAuthentication(),
                isAccessCodeRequired = walletsRepository.requireAccessCode(),
            )
        }
    }

    private fun sendItemsAnalytics() {
        uiState
            .filterIsInstance<AppSettingsScreenState.Content>()
            .distinctUntilChangedBy(AppSettingsScreenState.Content::items)
            .onEach { appSettingsItemsAnalyticsSender.send(it.items) }
            .launchIn(modelScope)
    }

    private data class LocalState(
        val hasSecuredWallets: Boolean = false,
        val isEnrollBiometricsNeeded: Boolean = false,
        val isBiometricAuthenticationUsed: Boolean = false,
        val isAccessCodeRequired: Boolean = false,
        val isInProgress: Boolean = false,
    )

    private data class AppSettingsState(
        val themeMode: AppThemeMode = AppThemeMode.DEFAULT,
        val isHidingEnabled: Boolean = false,
        val appCurrency: AppCurrency = AppCurrency.Default,
        val local: LocalState = LocalState(),
    )
}