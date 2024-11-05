package com.tangem.tap.proxy

import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.redux.StateDialog
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.extensions.dispatchWithMain
import com.tangem.tap.common.extensions.onUserWalletSelected
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.network.exchangeServices.ExchangeService
import org.rekotlin.Action
import org.rekotlin.Store
import javax.inject.Inject

/**
 * Holds objects from old modules, that missing in DI graph.
 * Object sets manually to use in new modules and [AppStateHolder] proxies its to DI.
 */
class AppStateHolder @Inject constructor() : ReduxStateHolder {

    @Deprecated("Use scan response from selected user wallet")
    var scanResponse: ScanResponse? = null
    var mainStore: Store<AppState>? = null
    var tangemSdkManager: TangemSdkManager? = null
    var exchangeService: ExchangeService? = null
    var buyService: ExchangeService? = null
    var sellService: ExchangeService? = null

    override fun dispatch(action: Action) {
        mainStore?.dispatch(action)
    }

    override suspend fun dispatchWithMain(action: Action) {
        mainStore?.dispatchWithMain(action)
    }

    override suspend fun onUserWalletSelected(userWallet: UserWallet) {
        mainStore?.onUserWalletSelected(userWallet)
    }

    override fun dispatchDialogShow(dialog: StateDialog) {
        mainStore?.dispatchDialogShow(dialog)
    }
}