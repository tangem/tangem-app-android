package com.tangem.features.managetokens.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.components.rows.model.BlockchainRowUM
import com.tangem.core.ui.components.rows.model.ChainRowUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.managetokens.entity.CurrencyItemUM
import com.tangem.features.managetokens.entity.CurrencyNetworkUM
import com.tangem.features.managetokens.entity.ManageTokensUM
import com.tangem.features.managetokens.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ComponentScoped
internal class ManageTokensModel @Inject constructor(
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val changedItemsIds: MutableSet<String> = mutableSetOf()
    private var items = initItems()

    val state: MutableStateFlow<ManageTokensUM> = MutableStateFlow(
        value = ManageTokensUM(
            popBack = router::pop,
            items = items,
            search = SearchBarUM(
                placeholderText = resourceReference(R.string.manage_tokens_search_placeholder),
                query = "",
                onQueryChange = ::searchCurrencies,
                isActive = false,
                onActiveChange = ::toggleSearchBar,
            ),
            hasChanges = false,
            onSaveClick = {},
            onAddCustomToken = {},
        ),
    )

    @Suppress("UnusedPrivateMember")
    private fun searchCurrencies(query: String) {
        // TODO: [REDACTED_JIRA]
    }

    private fun toggleSearchBar(isActive: Boolean) {
        state.update { state ->
            state.copy(
                search = state.search.copy(isActive = isActive),
            )
        }
    }

    private fun initItems() = List(size = 30) { index ->
        if (index < 2) {
            getCustomItem(index)
        } else {
            getBasicItem(index)
        }
    }.toPersistentList()

    private fun getCustomItem(index: Int) = CurrencyItemUM.Custom(
        id = index.toString(),
        model = ChainRowUM(
            name = "Custom token $index",
            type = "CT$index",
            icon = CurrencyIconState.CustomTokenIcon(
                tint = Color.White,
                background = Color.Black,
                topBadgeIconResId = R.drawable.img_eth_22,
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
            name = "Currency $index",
            type = "C$index",
            icon = CurrencyIconState.CoinIcon(
                url = null,
                fallbackResId = R.drawable.img_btc_22,
                isGrayscale = false,
                showCustomBadge = false,
            ),
            showCustom = false,
        ),
        networks = if (index == 2) {
            CurrencyItemUM.Basic.NetworksUM.Expanded(getCurrencyNetworks(index))
        } else {
            CurrencyItemUM.Basic.NetworksUM.Collapsed
        },
        onExpandClick = { toggleCurrency(index) },
    )

    private fun getCurrencyNetworks(currencyIndex: Int) = List(size = 3) { networkIndex ->
        CurrencyNetworkUM(
            id = networkIndex.toString(),
            model = BlockchainRowUM(
                name = "NETWORK$networkIndex",
                type = "N$networkIndex",
                iconResId = R.drawable.ic_eth_16,
                isMainNetwork = networkIndex == 0,
                isSelected = false,
            ),
            isSelected = false,
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

        state.update { state ->
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
                                            iconResId = if (isSelected) {
                                                R.drawable.img_eth_22
                                            } else {
                                                R.drawable.ic_eth_16
                                            },
                                            isSelected = isSelected,
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

        val id = "${currencyIndex}_$networkIndex"
        if (changedItemsIds.contains(id)) {
            changedItemsIds.remove(id)
        } else {
            changedItemsIds.add(id)
        }

        state.update { state ->
            items = items.mutate {
                it[currencyIndex] = updatedItem
            }
            state.copy(
                items = items,
                hasChanges = changedItemsIds.isNotEmpty(),
            )
        }
    }
}