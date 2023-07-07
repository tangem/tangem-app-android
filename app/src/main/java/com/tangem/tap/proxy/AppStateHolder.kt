package com.tangem.tap.proxy

import com.tangem.TangemSdk
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
import org.rekotlin.Store
import javax.inject.Inject

/**
 * Holds objects from old modules, that missing in DI graph.
 * Object sets manually to use in new modules and [AppStateHolder] proxies its to DI.
 */
class AppStateHolder @Inject constructor() : WalletsStateHolder {

    override var userWalletsListManager: UserWalletsListManager? = null

    @Deprecated("Use scan response from selected user wallet")
    var scanResponse: ScanResponse? = null
    var walletState: WalletState? = null
    var userTokensRepository: UserTokensRepository? = null
    var mainStore: Store<AppState>? = null
    var tangemSdkManager: TangemSdkManager? = null
    var tangemSdk: TangemSdk? = null
    var walletStoresManager: WalletStoresManager? = null
    var appFiatCurrency: FiatCurrency = FiatCurrency.Default

    fun getActualCard(): CardDTO? {
        return scanResponse?.card
    }
}
