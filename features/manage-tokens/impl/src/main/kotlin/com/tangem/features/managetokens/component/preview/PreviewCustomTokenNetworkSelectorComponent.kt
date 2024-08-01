package com.tangem.features.managetokens.component.preview

import androidx.compose.foundation.lazy.LazyListScope
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.managetokens.component.CustomTokenNetworkSelectorComponent
import com.tangem.features.managetokens.entity.CurrencyNetworkUM
import com.tangem.features.managetokens.entity.CustomTokenNetworkSelectorUM
import com.tangem.features.managetokens.entity.SelectedNetworkUM
import com.tangem.features.managetokens.impl.R
import com.tangem.features.managetokens.ui.customTokenNetworkSelectorContent
import kotlinx.collections.immutable.toImmutableList

internal class PreviewCustomTokenNetworkSelectorComponent(
    private val params: CustomTokenNetworkSelectorComponent.Params = CustomTokenNetworkSelectorComponent.Params(
        userWalletId = UserWalletId(stringValue = "321"),
        selectedNetwork = null,
        onNetworkSelected = {},
    ),
    networksSize: Int = 5,
) : CustomTokenNetworkSelectorComponent {

    private val previewNetworks = List(size = networksSize) { networkIndex ->
        val n = SelectedNetworkUM(
            id = Network.ID(networkIndex.toString()),
            name = "Network $networkIndex",
        )

        CurrencyNetworkUM(
            id = n.id,
            name = n.name,
            type = "N$networkIndex",
            iconResId = R.drawable.ic_eth_16,
            isMainNetwork = false,
            isSelected = n.id == params.selectedNetwork?.id,
            onSelectedStateChange = { params.onNetworkSelected(n) },
        )
    }.toImmutableList()

    private val previewState = CustomTokenNetworkSelectorUM(
        networks = previewNetworks,
    )

    override fun content(scope: LazyListScope) {
        scope.customTokenNetworkSelectorContent(
            model = previewState,
        )
    }
}
