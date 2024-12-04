package com.tangem.features.managetokens.component.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId
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
        when (params) {
            is Params.DerivationPathSelector -> {
                val d = SelectedDerivationPath(
                    id = Network.ID(index.toString()),
                    value = Network.DerivationPath.Card("m/44'/0'/0'/0/$index"),
                    name = "Network $index",
                    isDefault = false,
                )

                DerivationPathUM(
                    id = d.id?.value ?: "",
                    value = d.value.value.orEmpty(),
                    networkName = stringReference(d.name),
                    isSelected = d.value == params.selectedDerivationPath?.value,
                    onSelectedStateChange = { params.onDerivationPathSelected(d) },
                )
            }
            is Params.NetworkSelector -> {
                val n = SelectedNetwork(
                    id = Network.ID(index.toString()),
                    name = "Network $index",
                    derivationPath = Network.DerivationPath.Card("m/44'/0'/0'/0/$index"),
                    canHandleTokens = false,
                )

                CurrencyNetworkUM(
                    network = Network(
                        id = n.id,
                        backendId = n.id.value,
                        name = "Network $index",
                        currencySymbol = "N$index",
                        derivationPath = Network.DerivationPath.Card(""),
                        isTestnet = false,
                        standardType = Network.StandardType.ERC20,
                        hasFiatFeeRate = false,
                        canHandleTokens = false,
                        transactionExtrasType = Network.TransactionExtrasType.NONE,
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

    val previewState = CustomTokenSelectorUM(
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