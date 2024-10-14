package com.tangem.tap.common.redux.legacy

import com.tangem.domain.redux.LegacyAction
import com.tangem.tap.common.extensions.dispatchWithMain
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.onboarding.products.wallet.redux.BackupStartedSource
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.rekotlin.Middleware

internal object LegacyMiddleware {
    private val prepareDetailsScreenJobHolder = JobHolder()

    val legacyMiddleware: Middleware<AppState> = { _, _ ->
        { next ->
            { action ->
                when (action) {
                    is LegacyAction.StartOnboardingProcess -> {
                        store.dispatch(
                            GlobalAction.Onboarding.Start(
                                scanResponse = action.scanResponse,
                                source = BackupStartedSource.CreateBackup,
                                canSkipBackup = action.canSkipBackup,
                            ),
                        )
                    }
                    is LegacyAction.PrepareDetailsScreen -> {
                        val userWalletsListManager = store.inject(DaggerGraphState::generalUserWalletsListManager)
                        val walletsRepository = store.inject(DaggerGraphState::walletsRepository)

                        userWalletsListManager.selectedUserWallet
                            .distinctUntilChanged()
                            .onEach { selectedUserWallet ->
                                store.dispatchWithMain(
                                    DetailsAction.PrepareScreen(
                                        scanResponse = selectedUserWallet.scanResponse,
                                        shouldSaveUserWallets = walletsRepository.shouldSaveUserWalletsSync(),
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
}