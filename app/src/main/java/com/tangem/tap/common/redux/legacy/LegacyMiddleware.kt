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

@Suppress("MemberNameEqualsClassName")
internal object LegacyMiddleware {
    private val prepareDetailsScreenJobHolder = JobHolder()

    val legacyMiddleware: Middleware<AppState> = { _, _ ->
        { next ->
            { action ->
                when (action) {
                    is LegacyAction.PrepareDetailsScreen -> {
                        selectedUserWallet()
                            .distinctUntilChanged { old, new ->
                                if (old is UserWallet.Cold && new is UserWallet.Cold) {
                                    old.walletId == new.walletId &&
                                        old.scanResponse == new.scanResponse
                                } else {
                                    old.walletId == new.walletId
                                }
                            }
                            .onEach { selectedUserWallet ->
                                val initializedAppSettingsStateContent = initializeAppSettingsState()
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

    private fun selectedUserWallet(): Flow<UserWallet> {
        return store.inject(DaggerGraphState::userWalletsListRepository).selectedUserWallet.filterNotNull()
    }

    /**
     * LEGACY: We need to initialize [AppSettingsState] async to avoid drawing blocking
     * previously it was initialized in runBlocking and blocked details screen
     */
    private suspend fun initializeAppSettingsState(): AppSettingsState {
        return AppSettingsState(
            selectedAppCurrency = store.state.globalState.appCurrency,
            selectedThemeMode = store.inject(DaggerGraphState::appThemeModeRepository).getAppThemeMode().firstOrNull()
                ?: AppThemeMode.DEFAULT,
            requireAccessCode = store.inject(DaggerGraphState::walletsRepository).requireAccessCode(),
            useBiometricAuthentication = store.inject(DaggerGraphState::walletsRepository).useBiometricAuthentication(),
            isHidingEnabled = store.inject(DaggerGraphState::balanceHidingRepository)
                .getBalanceHidingSettings().isHidingEnabledInSettings,
            needEnrollBiometrics = runCatching(tangemSdkManager::needEnrollBiometrics).getOrNull() == true,
            hasSecuredWallets = store.inject(DaggerGraphState::userWalletsListRepository).hasSecuredWallets(),
        )
    }
}