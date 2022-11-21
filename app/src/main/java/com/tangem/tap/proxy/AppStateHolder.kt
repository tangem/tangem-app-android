package com.tangem.tap.proxy

import com.tangem.common.card.Card
import com.tangem.domain.common.ScanResponse
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.tokens.UserTokensRepository
import com.tangem.tap.features.tokens.redux.TokensMiddleware
import com.tangem.tap.features.wallet.redux.WalletState
import org.rekotlin.Store

/**
 * Holds objects from old modules, that missing in DI graph
 * Object sets manually to use in new modules and [AppStateHolder] proxies its to DI
 */
class AppStateHolder {

    val scanResponse: ScanResponse? = null
    val walletState: WalletState? = null
    val userTokensRepository: UserTokensRepository? = null
    val mainStore: Store<AppState>? = null
    val tokesMiddleware: TokensMiddleware? = null

    fun getActualCard(): Card? {
        return scanResponse?.card
    }
}