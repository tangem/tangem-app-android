package com.tangem.tap.proxy

import com.tangem.common.card.Card
import com.tangem.domain.common.ScanResponse
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.TangemSdkManager
import com.tangem.tap.domain.tokens.UserTokensRepository
import com.tangem.tap.features.wallet.redux.WalletState
import org.rekotlin.Store

/**
 * Holds objects from old modules, that missing in DI graph
 * Object sets manually to use in new modules and [AppStateHolder] proxies its to DI
 */
class AppStateHolder {

    var scanResponse: ScanResponse? = null
    var walletState: WalletState? = null
    var userTokensRepository: UserTokensRepository? = null
    var mainStore: Store<AppState>? = null
    var tangemSdkManager: TangemSdkManager? = null

    fun getActualCard(): Card? {
        return scanResponse?.card
    }
}
