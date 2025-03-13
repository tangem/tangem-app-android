package com.tangem.tap.features.details.ui.appsettings.model

import androidx.compose.runtime.Stable
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.appcurrency.repository.AppCurrencyRepository
import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.domain.apptheme.repository.AppThemeModeRepository
import com.tangem.domain.balancehiding.repositories.BalanceHidingRepository
import com.tangem.domain.settings.CanUseBiometryUseCase
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Settings
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.dispatchWithMain
import com.tangem.tap.features.details.redux.AppSetting
import com.tangem.tap.features.details.redux.AppSettingsState
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.details.redux.DetailsState
import com.tangem.tap.features.details.ui.appsettings.AppSettingsDialogsFactory
import com.tangem.tap.features.details.ui.appsettings.AppSettingsItemsFactory
import com.tangem.tap.features.details.ui.appsettings.AppSettingsScreenState
import com.tangem.tap.features.details.ui.appsettings.analytics.AppSettingsItemsAnalyticsSender
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.rekotlin.StoreSubscriber
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class AppSettingsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val appCurrencyRepository: AppCurrencyRepository,
    private val walletsRepository: WalletsRepository,
    private val canUseBiometryUseCase: CanUseBiometryUseCase,
    private val balanceHidingRepository: BalanceHidingRepository,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val appThemeModeRepository: AppThemeModeRepository,
    private val settingsRepository: SettingsRepository,
    private val appSettingsItemsAnalyticsSender: AppSettingsItemsAnalyticsSender,
) : Model(), StoreSubscriber<DetailsState> {

    private val itemsFactory = AppSettingsItemsFactory()
    private val dialogsFactory = AppSettingsDialogsFactory()

    private val appCurrencyUpdatesJobHolder = JobHolder()

    private val _uiState: MutableStateFlow<AppSettingsScreenState> = MutableStateFlow(
        value = AppSettingsScreenState.Loading,
    )
    val uiState: StateFlow<AppSettingsScreenState> = _uiState

    init {
        bootstrapAppCurrencyUpdates()
        bootstrapBiometricsUpdates()

        subscribeToStoreChanges()
        sendItemsAnalytics()
    }

    override fun newState(state: DetailsState) {
        val items = buildItems(state.appSettingsState)

        _uiState.update { prevState ->
            when (prevState) {
                is AppSettingsScreenState.Content -> prevState.copy(
                    items = items,
                )
                is AppSettingsScreenState.Loading -> AppSettingsScreenState.Content(
                    items = items,
                    dialog = null,
                )
            }
        }
    }

    fun onResume() {
        store.dispatch(DetailsAction.AppSettings.CheckBiometricsStatus(modelScope))
    }

    override fun onDestroy() {
        super.onDestroy()
        store.unsubscribe(subscriber = this)
    }

    private fun buildItems(state: AppSettingsState): ImmutableList<AppSettingsScreenState.Item> {
        val items = buildList {
            if (state.needEnrollBiometrics) {
                itemsFactory.createEnrollBiometricsCard(
                    onClick = ::enrollBiometrics,
                ).let(::add)
            }

            itemsFactory.createSelectAppCurrencyButton(
                currentAppCurrencyName = state.selectedAppCurrency.name,
                onClick = ::showAppCurrencySelector,
            ).let(::add)

            if (state.isBiometricsAvailable) {
                val canUseBiometrics = !state.needEnrollBiometrics && !state.isInProgress

                itemsFactory.createSaveWalletsSwitch(
                    isChecked = state.saveWallets,
                    isEnabled = canUseBiometrics,
                    onCheckedChange = ::onSaveWalletsToggled,
                ).let(::add)

                itemsFactory.createSaveAccessCodeSwitch(
                    isChecked = state.saveAccessCodes,
                    isEnabled = canUseBiometrics,
                    onCheckedChange = ::onSaveAccessCodesToggled,
                ).let(::add)
            }

            itemsFactory.createFlipToHideBalanceSwitch(
                isChecked = state.isHidingEnabled,
                isEnabled = true,
                onCheckedChange = ::onFlipToHideBalanceToggled,
            ).let(::add)

            itemsFactory.createSelectThemeModeButton(
                currentThemeMode = state.selectedThemeMode,
                onClick = { showThemeModeSelector(state.selectedThemeMode) },
            ).let(::add)
        }

        return items.toImmutableList()
    }

    private fun enrollBiometrics() {
        store.dispatchOnMain(DetailsAction.AppSettings.EnrollBiometrics)
    }

    private fun showAppCurrencySelector() {
        store.dispatchNavigationAction { push(AppRoute.AppCurrencySelector) }
    }

    private fun showThemeModeSelector(selectedMode: AppThemeMode) {
        updateContentState {
            copy(
                dialog = dialogsFactory.createThemeModeSelectorDialog(
                    selectedModeIndex = selectedMode.ordinal,
                    onSelect = { mode ->
                        analyticsEventHandler.send(
                            event = Settings.AppSettings.ThemeSwitched(
                                theme = AnalyticsParam.AppTheme.fromAppThemeMode(mode),
                            ),
                        )
                        store.dispatchOnMain(DetailsAction.AppSettings.ChangeAppThemeMode(mode))
                        dismissDialog()
                    },
                    onDismiss = ::dismissDialog,
                ),
            )
        }
    }

    private fun onSaveWalletsToggled(isChecked: Boolean) {
        if (isChecked) {
            onSettingsToggled(AppSetting.SaveWallets, enable = true)
        } else {
            updateContentState {
                copy(
                    dialog = dialogsFactory.createDeleteSavedWalletsAlert(
                        onDelete = {
                            onSettingsToggled(AppSetting.SaveWallets, enable = false)
                            dismissDialog()
                        },
                        onDismiss = ::dismissDialog,
                    ),
                )
            }
        }
    }

    private fun onSaveAccessCodesToggled(isChecked: Boolean) {
        if (isChecked) {
            onSettingsToggled(AppSetting.SaveAccessCode, enable = true)
        } else {
            updateContentState {
                copy(
                    dialog = dialogsFactory.createDeleteSavedAccessCodesAlert(
                        onDelete = {
                            onSettingsToggled(AppSetting.SaveAccessCode, enable = false)
                            dismissDialog()
                        },
                        onDismiss = ::dismissDialog,
                    ),
                )
            }
        }
    }

    private fun onSettingsToggled(setting: AppSetting, enable: Boolean) {
        store.dispatch(DetailsAction.AppSettings.SwitchPrivacySetting(enable = enable, setting = setting))
    }

    private fun onFlipToHideBalanceToggled(enable: Boolean) {
        val param = AnalyticsParam.OnOffState(enable)
        analyticsEventHandler.send(Settings.AppSettings.HideBalanceChanged(param))

        store.dispatch(DetailsAction.AppSettings.ChangeBalanceHiding(hideBalance = enable))
    }

    private fun dismissDialog() {
        updateContentState { copy(dialog = null) }
    }

    private fun bootstrapAppCurrencyUpdates() {
        appCurrencyRepository
            .getSelectedAppCurrency()
            .onEach {
                if (it.code == store.state.globalState.appCurrency.code) return@onEach

                store.dispatchWithMain(DetailsAction.AppSettings.ChangeAppCurrency(it))
            }
            .launchIn(scope)
            .saveIn(appCurrencyUpdatesJobHolder)
    }

    private fun bootstrapBiometricsUpdates() = modelScope.launch {
        val state = AppSettingsState(
            saveWallets = walletsRepository.shouldSaveUserWalletsSync(),
            saveAccessCodes = settingsRepository.shouldSaveAccessCodes(),
            isBiometricsAvailable = canUseBiometryUseCase(),
            isHidingEnabled = balanceHidingRepository.getBalanceHidingSettings().isHidingEnabledInSettings,
            selectedAppCurrency = appCurrencyRepository.getSelectedAppCurrency().firstOrNull() ?: AppCurrency.Default,
            selectedThemeMode = appThemeModeRepository.getAppThemeMode().firstOrNull() ?: AppThemeMode.DEFAULT,
        )

        store.dispatchWithMain(DetailsAction.AppSettings.Prepare(state))
    }

    private fun subscribeToStoreChanges() {
        store.subscribe(subscriber = this) { state ->
            state.skipRepeats { oldState, newState ->
                oldState.detailsState == newState.detailsState
            }.select { it.detailsState }
        }
    }

    private fun sendItemsAnalytics() {
        uiState
            .filterIsInstance<AppSettingsScreenState.Content>()
            .distinctUntilChangedBy(AppSettingsScreenState.Content::items)
            .onEach { appSettingsItemsAnalyticsSender.send(it.items) }
            .launchIn(scope)
    }

    private fun updateContentState(block: AppSettingsScreenState.Content.() -> AppSettingsScreenState.Content) {
        _uiState.update { prevState ->
            when (prevState) {
                is AppSettingsScreenState.Content -> block(prevState)
                is AppSettingsScreenState.Loading -> prevState
            }
        }
    }
}