package com.tangem.tap.proxy.redux

import com.tangem.datasource.asset.AssetReader
import com.tangem.datasource.connection.NetworkConnectionManager
import com.tangem.features.tester.api.TesterRouter
import com.tangem.tap.features.customtoken.api.featuretoggles.CustomTokenFeatureToggles
import com.tangem.tap.features.tokens.api.featuretoggles.TokensListFeatureToggles
import org.rekotlin.Action

sealed interface DaggerGraphAction : Action {

    data class SetApplicationDependencies(
        val assetReader: AssetReader,
        val networkConnectionManager: NetworkConnectionManager,
        val tokensListFeatureToggles: TokensListFeatureToggles,
        val customTokenFeatureToggles: CustomTokenFeatureToggles,
    ) : DaggerGraphAction

    data class SetActivityDependencies(val testerRouter: TesterRouter) : DaggerGraphAction
}
