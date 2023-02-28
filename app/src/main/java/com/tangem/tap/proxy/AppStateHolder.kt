package com.tangem.tap.proxy

import com.tangem.TangemSdk
import com.tangem.domain.common.CardDTO
import com.tangem.domain.common.ScanResponse
import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.TangemSdkManager
import com.tangem.tap.domain.tokens.UserTokensRepository
import com.tangem.tap.domain.userWalletList.UserWalletsListManager
import com.tangem.tap.domain.walletStores.WalletStoresManager
import com.tangem.tap.features.wallet.redux.WalletState
import org.rekotlin.Store
import javax.inject.Inject

/**
 * Holds objects from old modules, that missing in DI graph
 * Object sets manually to use in new modules and [AppStateHolder] proxies its to DI
 */
class AppStateHolder @Inject constructor() {

    var scanResponse: ScanResponse? = null
    var walletState: WalletState? = null
    var userTokensRepository: UserTokensRepository? = null
    var mainStore: Store<AppState>? = null
    var tangemSdkManager: TangemSdkManager? = null
    var tangemSdk: TangemSdk? = null
    var walletStoresManager: WalletStoresManager? = null
    var userWalletsListManager: UserWalletsListManager? = null
    var appFiatCurrency: FiatCurrency = FiatCurrency.Default

    fun getActualCard(): CardDTO? {
        return scanResponse?.card
    }
}
