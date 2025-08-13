package com.tangem.tap.common.redux.legacy

import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.redux.LegacyAction
import com.tangem.tap.common.extensions.dispatchWithMain
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.details.redux.AppSettingsState
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import org.rekotlin.Middleware

internal object LegacyMiddleware {
    private val prepareDetailsScreenJobHolder = JobHolder()

    val legacyMiddleware: Middleware<AppState> = { _, _ ->
        { next ->
            { action ->
                when (action) {
                    is LegacyAction.PrepareDetailsScreen -> {
                        val userWalletsListManager = store.inject(DaggerGraphState::generalUserWalletsListManager)
                        val walletsRepository = store.inject(DaggerGraphState::walletsRepository)

                        userWalletsListManager.selectedUserWallet
                            .distinctUntilChanged()
                            .onEach { selectedUserWallet ->
                                val initializedAppSettingsStateContent = initializeAppSettingsState(
                                    shouldSaveUserWallets = walletsRepository.shouldSaveUserWalletsSync(),
                                )
                                store.dispatchWithMain(
                                    DetailsAction.PrepareScreen(
                                        scanResponse = (selectedUserWallet as? UserWallet.Cold)?.scanResponse,
                                        initializedAppSettingsState = initializedAppSettingsStateContent,
                                    ),
                                )
                            }
                            .flowOn(Dispatchers.IO)
                            .launchIn(scope)
                            .saveIn(prepareDetailsScreenJobHolder)
                    }
                }
                next(action)
            }
        }
    }

    /**
     * LEGACY: We need to initialize [AppSettingsState] async to avoid drawing blocking
     * previously it was initialized in runBlocking and blocked details screen
     */
    private suspend fun initializeAppSettingsState(shouldSaveUserWallets: Boolean): AppSettingsState {
        return AppSettingsState(
            isBiometricsAvailable = tangemSdkManager.checkCanUseBiometry(),
            saveWallets = shouldSaveUserWallets,
            saveAccessCodes = store.inject(DaggerGraphState::settingsRepository).shouldSaveAccessCodes(),
            selectedAppCurrency = store.state.globalState.appCurrency,
            selectedThemeMode = store.inject(DaggerGraphState::appThemeModeRepository).getAppThemeMode().firstOrNull()
                ?: AppThemeMode.DEFAULT,
            isHidingEnabled = store.inject(DaggerGraphState::balanceHidingRepository)
                .getBalanceHidingSettings().isHidingEnabledInSettings,
            needEnrollBiometrics = runCatching(tangemSdkManager::needEnrollBiometrics).getOrNull() == true,
        )
    }
}