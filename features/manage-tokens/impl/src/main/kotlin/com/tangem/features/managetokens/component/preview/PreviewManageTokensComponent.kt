package com.tangem.features.managetokens.component.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.rows.model.BlockchainRowUM
import com.tangem.core.ui.components.rows.model.ChainRowUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.managetokens.component.ManageTokensComponent
import com.tangem.features.managetokens.entity.CurrencyItemUM
import com.tangem.features.managetokens.entity.CurrencyNetworkUM
import com.tangem.features.managetokens.entity.ManageTokensUM
import com.tangem.features.managetokens.impl.R
import com.tangem.features.managetokens.ui.ManageTokensScreen
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

internal class PreviewManageTokensComponent : ManageTokensComponent {

    private var items = List(size = 30) { index ->
        if (index < 2) {
            getCustomItem(index)
        } else {
            getBasicItem(index)
        }
    }.toPersistentList()

    private val previewState = MutableStateFlow(
        value = ManageTokensUM(
            popBack = {},
            items = items,
        ),
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by previewState.collectAsState()

        ManageTokensScreen(
            modifier = modifier,
            state = state,
        )
    }

    private fun getCustomItem(index: Int) = CurrencyItemUM.Custom(
        id = index.toString(),
        model = ChainRowUM(
            name = stringReference("Custom token $index"),
            type = stringReference("CT$index"),
            icon = CurrencyIconState.CustomTokenIcon(
                tint = Color.White,
                background = Color.Black,
                networkBadgeIconResId = R.drawable.img_eth_22,
                isGrayscale = false,
                showCustomBadge = true,
            ),
            showCustom = true,
        ),
        onRemoveClick = {},
    )

    private fun getBasicItem(index: Int) = CurrencyItemUM.Basic(
        id = index.toString(),
        model = ChainRowUM(
            name = stringReference("Currency $index"),
            type = stringReference("C$index"),
            icon = CurrencyIconState.CoinIcon(
                url = null,
                fallbackResId = R.drawable.img_btc_22,
                isGrayscale = false,
                showCustomBadge = false,
            ),
            showCustom = false,
        ),
        networks = CurrencyItemUM.Basic.NetworksUM.Collapsed,
        onExpandClick = { toggleCurrency(index) },
    )

    private fun getCurrencyNetworks(currencyIndex: Int) = List(size = 5) { networkIndex ->
        val selectedIndex = Random.nextInt(from = 0, until = 5)

        CurrencyNetworkUM(
            id = networkIndex.toString(),
            model = BlockchainRowUM(
                name = stringReference("NETWORK$networkIndex"),
                type = stringReference("N$networkIndex"),
                icon = CurrencyIconState.Locked,
                isAccented = networkIndex == 0,
                usePrimaryTextColor = networkIndex == selectedIndex,
            ),
            isSelected = networkIndex == selectedIndex,
            onSelectedStateChange = { toggleNetwork(currencyIndex, networkIndex, isSelected = it) },
        )
    }.toImmutableList()

    private fun toggleCurrency(index: Int) {
        val updatedItem = when (val item = items[index]) {
            is CurrencyItemUM.Basic -> item.copy(
                networks = if (item.networks is CurrencyItemUM.Basic.NetworksUM.Collapsed) {
                    CurrencyItemUM.Basic.NetworksUM.Expanded(getCurrencyNetworks(index))
                } else {
                    CurrencyItemUM.Basic.NetworksUM.Collapsed
                },
            )
            is CurrencyItemUM.Custom -> return
        }

        previewState.update { state ->
            items = items.mutate {
                it[index] = updatedItem
            }
            state.copy(items = items)
        }
    }

    private fun toggleNetwork(currencyIndex: Int, networkIndex: Int, isSelected: Boolean) {
        val updatedItem = when (val item = items[currencyIndex]) {
            is CurrencyItemUM.Basic -> {
                val updatedNetworks = (item.networks as? CurrencyItemUM.Basic.NetworksUM.Expanded)
                    ?.copy(
                        networks = item.networks.networks.toPersistentList().mutate {
                            it.fastForEachIndexed { index, network ->
                                if (index == networkIndex) {
                                    it[index] = network.copy(
                                        model = network.model.copy(
                                            usePrimaryTextColor = isSelected,
                                        ),
                                        isSelected = isSelected,
                                    )
                                }
                            }
                        },
                    )
                    ?: return

                item.copy(networks = updatedNetworks)
            }
            is CurrencyItemUM.Custom -> return
        }

        previewState.update { state ->
            items = items.mutate {
                it[currencyIndex] = updatedItem
            }
            state.copy(items = items)
        }
    }
}