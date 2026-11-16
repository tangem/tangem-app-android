package com.tangem.features.polymarket.impl.main.addfunds.network

import androidx.compose.runtime.Stable
import com.tangem.common.ui.extensions.networkIconResId
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toPersistentList
import javax.inject.Inject

@Stable
@ModelScoped
internal class PolymarketChooseNetworkModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<PolymarketChooseNetworkComponent.Params>()

    val uiState: PolymarketChooseNetworkUM = PolymarketChooseNetworkUM(
        networks = params.receiveConfigs.map(::toNetworkUM).toPersistentList(),
        onDismiss = ::onDismiss,
    )

    fun onDismiss() {
        params.onDismiss()
    }

    private fun toNetworkUM(config: TokenReceiveConfig): PolymarketChooseNetworkUM.Network {
        val currency = config.cryptoCurrency
        return PolymarketChooseNetworkUM.Network(
            id = currency.network.rawId,
            icon = TangemTokenIcon.State(url = currency.iconUrl, topIcon = currency.networkIconResId),
            name = stringReference(currency.network.name),
            symbol = stringReference(currency.symbol),
            onClick = { params.onNetworkClick(config) },
        )
    }
}