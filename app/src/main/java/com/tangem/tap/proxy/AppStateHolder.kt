package com.tangem.tap.proxy

import com.tangem.core.navigation.NavigationAction
import com.tangem.core.navigation.NavigationStateHolder
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.legacy.WalletsStateHolder
import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.TangemSdkManager
import com.tangem.tap.domain.tokens.UserTokensRepository
import com.tangem.tap.domain.walletStores.WalletStoresManager
import com.tangem.tap.features.wallet.redux.WalletState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.rekotlin.Store
import javax.inject.Inject

/**
 * Holds objects from old modules, that missing in DI graph.
 * Object sets manually to use in new modules and [AppStateHolder] proxies its to DI.
 */
class AppStateHolder @Inject constructor() : WalletsStateHolder, NavigationStateHolder {

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
    var walletState: WalletState? = null
    var userTokensRepository: UserTokensRepository? = null
    var mainStore: Store<AppState>? = null
    var tangemSdkManager: TangemSdkManager? = null
    var walletStoresManager: WalletStoresManager? = null
    var appFiatCurrency: FiatCurrency = FiatCurrency.Default

    fun getActualCard(): CardDTO? {
        return scanResponse?.card
    }

    override fun navigate(action: NavigationAction) {
        mainStore?.dispatch(action)
    }
}
