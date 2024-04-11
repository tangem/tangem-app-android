package com.tangem.tap.proxy

import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.core.navigation.ReduxNavController
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.legacy.WalletsStateHolder
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.dispatchWithMain
import com.tangem.tap.common.extensions.onUserWalletSelected
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.sdk.TangemSdkManager
import com.tangem.tap.network.exchangeServices.ExchangeService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.rekotlin.Action
import org.rekotlin.Store
import javax.inject.Inject

/**
 * Holds objects from old modules, that missing in DI graph.
 * Object sets manually to use in new modules and [AppStateHolder] proxies its to DI.
 */
class AppStateHolder @Inject constructor() : WalletsStateHolder, ReduxNavController, ReduxStateHolder {

    override var userWalletsListManager: UserWalletsListManager? = null
        set(value) {
            field = value
            _userWalletsListManagerFlow.value = value
        }

    override val userWalletListManagerFlow: Flow<UserWalletsListManager?>
        get() = _userWalletsListManagerFlow

    private val _userWalletsListManagerFlow = MutableStateFlow<UserWalletsListManager?>(null)

    @Deprecated("Use scan response from selected user wallet")
    var scanResponse: ScanResponse? = null
    var mainStore: Store<AppState>? = null
    var tangemSdkManager: TangemSdkManager? = null
    var exchangeService: ExchangeService? = null

    fun getActualCard(): CardDTO? {
        return scanResponse?.card
    }

    override fun navigate(action: NavigationAction) {
        mainStore?.dispatchOnMain(action)
    }

    override fun getBackStack(): List<AppScreen> = mainStore?.state?.navigationState?.backStack.orEmpty()

    override fun popBackStack(screen: AppScreen?) {
        mainStore?.dispatchOnMain(NavigationAction.PopBackTo(screen))
    }

    override fun dispatch(action: Action) {
        mainStore?.dispatch(action)
    }

    override suspend fun dispatchWithMain(action: Action) {
        mainStore?.dispatchWithMain(action)
    }

    override suspend fun onUserWalletSelected(userWallet: UserWallet) {
        mainStore?.onUserWalletSelected(userWallet)
    }
}
