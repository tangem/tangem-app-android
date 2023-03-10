package com.tangem.tap.proxy.redux

import com.tangem.datasource.asset.AssetReader
import com.tangem.datasource.connection.NetworkConnectionManager
import com.tangem.features.tester.api.TesterRouter
import org.rekotlin.Action

sealed interface DaggerGraphAction : Action {

    data class SetApplicationDependencies(
        val assetReader: AssetReader,
        val networkConnectionManager: NetworkConnectionManager,
    ) : DaggerGraphAction

    data class SetActivityDependencies(val testerRouter: TesterRouter) : DaggerGraphAction
}