package com.tangem.tap.proxy.redux

import com.tangem.datasource.asset.AssetReader
import com.tangem.datasource.connection.NetworkConnectionManager
import com.tangem.features.tester.api.TesterRouter
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectInteractor
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectRepository
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectSessionsRepository
import org.rekotlin.Action

sealed interface DaggerGraphAction : Action {

    data class SetApplicationDependencies(
        val assetReader: AssetReader,
        val networkConnectionManager: NetworkConnectionManager,
        val walletConnectRepository: WalletConnectRepository,
        val walletConnectSessionsRepository: WalletConnectSessionsRepository,
    ) : DaggerGraphAction

    data class SetActivityDependencies(
        val testerRouter: TesterRouter,
        val walletConnectInteractor: WalletConnectInteractor,
    ) : DaggerGraphAction
}