package com.tangem.features.managetokens.component.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.managetokens.component.CustomTokenSelectorComponent
import com.tangem.features.managetokens.component.CustomTokenSelectorComponent.Params
import com.tangem.features.managetokens.entity.customtoken.CustomTokenSelectorUM
import com.tangem.features.managetokens.entity.customtoken.SelectedDerivationPath
import com.tangem.features.managetokens.entity.customtoken.SelectedNetwork
import com.tangem.features.managetokens.entity.item.CurrencyNetworkUM
import com.tangem.features.managetokens.entity.item.DerivationPathUM
import com.tangem.features.managetokens.impl.R
import com.tangem.features.managetokens.ui.CustomTokenSelectorContent
import kotlinx.collections.immutable.toImmutableList

internal class PreviewCustomTokenSelectorComponent(
    private val params: Params = Params.NetworkSelector(
        userWalletId = UserWalletId(stringValue = "321"),
        selectedNetwork = null,
        onNetworkSelected = {},
    ),
    itemsSize: Int = 5,
) : CustomTokenSelectorComponent {

    private val previewItems = List(size = itemsSize) { index ->
        val derivationPath = Network.DerivationPath.Card("m/44'/0'/0'/0/$index")

        when (params) {
            is Params.DerivationPathSelector -> {
                val d = SelectedDerivationPath(
                    id = Network.ID(value = index.toString(), derivationPath = derivationPath),
                    value = derivationPath,
                    name = "Network $index",
                    isDefault = false,
                )

                DerivationPathUM(
                    id = d.id?.rawId?.value ?: "",
                    value = d.value.value.orEmpty(),
                    networkName = stringReference(d.name),
                    isSelected = d.value == params.selectedDerivationPath?.value,
                    onSelectedStateChange = { params.onDerivationPathSelected(d) },
                )
            }
            is Params.NetworkSelector -> {
                val n = SelectedNetwork(
                    id = Network.ID(value = index.toString(), derivationPath = derivationPath),
                    name = "Network $index",
                    derivationPath = derivationPath,
                    canHandleTokens = false,
                )

                CurrencyNetworkUM(
                    network = Network(
                        id = n.id,
                        backendId = n.id.rawId.value,
                        name = "Network $index",
                        currencySymbol = "N$index",
                        derivationPath = Network.DerivationPath.Card(""),
                        isTestnet = false,
                        standardType = Network.StandardType.ERC20,
                        hasFiatFeeRate = false,
                        canHandleTokens = false,
                        transactionExtrasType = Network.TransactionExtrasType.NONE,
                        nameResolvingType = Network.NameResolvingType.NONE,
                    ),
                    name = "Network $index",
                    type = "N$index",
                    iconResId = R.drawable.ic_eth_16,
                    isMainNetwork = false,
                    isSelected = n.id == params.selectedNetwork?.id,
                    onSelectedStateChange = { params.onNetworkSelected(n) },
                    onLongClick = {},
                )
            }
        }
    }.toImmutableList()

    private val previewState = CustomTokenSelectorUM(
        header = when (params) {
            is Params.DerivationPathSelector -> CustomTokenSelectorUM.HeaderUM.CustomDerivationButton(
                value = null,
                onClick = {},
            )
            is Params.NetworkSelector -> if (params.selectedNetwork == null) {
                CustomTokenSelectorUM.HeaderUM.Description
            } else {
                CustomTokenSelectorUM.HeaderUM.None
            }
        },
        items = previewItems,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        CustomTokenSelectorContent(modifier = modifier, model = previewState)
    }
}