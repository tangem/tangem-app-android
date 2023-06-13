package com.tangem.tap.proxy.redux

import com.tangem.datasource.asset.AssetReader
import com.tangem.datasource.connection.NetworkConnectionManager
import com.tangem.domain.card.ScanCardUseCase
import com.tangem.features.tester.api.TesterRouter
import com.tangem.features.wallet.featuretoggles.WalletFeatureToggles
import com.tangem.features.wallet.navigation.WalletRouter
import com.tangem.tap.features.customtoken.api.featuretoggles.CustomTokenFeatureToggles
import org.rekotlin.StateType

data class DaggerGraphState(
    val assetReader: AssetReader? = null,
    val testerRouter: TesterRouter? = null,
    val networkConnectionManager: NetworkConnectionManager? = null,
    val customTokenFeatureToggles: CustomTokenFeatureToggles? = null,
    val scanCardUseCase: ScanCardUseCase? = null,
    val walletFeatureToggles: WalletFeatureToggles? = null,
    val walletRouter: WalletRouter? = null,
) : StateType {

    inline fun <reified T> get(getDependency: DaggerGraphState.() -> T?): T {
        return requireNotNull(getDependency()) {
            "${T::class.simpleName} isn't initialized "
        }
    }
}