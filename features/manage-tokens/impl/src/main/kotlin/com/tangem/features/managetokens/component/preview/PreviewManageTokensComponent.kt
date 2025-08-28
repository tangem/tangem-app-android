package com.tangem.features.managetokens.component.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.features.managetokens.component.ManageTokensComponent
import com.tangem.features.managetokens.entity.item.CurrencyItemUM
import com.tangem.features.managetokens.entity.item.CurrencyNetworkUM
import com.tangem.features.managetokens.entity.managetokens.ManageTokensTopBarUM
import com.tangem.features.managetokens.entity.managetokens.ManageTokensUM
import com.tangem.features.managetokens.impl.R
import com.tangem.features.managetokens.ui.ManageTokensScreen
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class PreviewManageTokensComponent(
    private val isLoading: Boolean,
    showTangemIcon: Boolean,
    params: ManageTokensComponent.Params,
) : ManageTokensComponent {

    private val changedItemsIds: MutableSet<String> = mutableSetOf()

    private var items = initItems()
    private val previewState = MutableStateFlow(
        value = ManageTokensUM.ManageContent(
            popBack = {},
            items = items,
            topBar = if (params.userWalletId != null) {
                ManageTokensTopBarUM.ManageContent(
                    title = resourceReference(id = R.string.main_manage_tokens),
                    onBackButtonClick = {},
                    endButton = TopAppBarButtonUM.Icon(
                        iconRes = R.drawable.ic_plus_24,
                        onClicked = {},
                    ),
                )
            } else {
                ManageTokensTopBarUM.ReadContent(
                    title = resourceReference(R.string.common_search_tokens),
                    onBackButtonClick = {},
                )
            },
            search = SearchBarUM(
                placeholderText = resourceReference(R.string.manage_tokens_search_placeholder),
                query = "",
                onQueryChange = ::searchCurrencies,
                isActive = false,
                onActiveChange = ::toggleSearchBar,
            ),
            hasChanges = false,
            isInitialBatchLoading = false,
            isNextBatchLoading = true,
            loadMore = { false },
            saveChanges = {},
            isSavingInProgress = false,
            needToAddDerivations = showTangemIcon,
        ),
    )

    private fun searchCurrencies(query: String) {
        previewState.update { state ->
            items = if (query.isBlank()) {
                initItems()
            } else {
                items.filter { currency ->
                    currency.name.contains(query, ignoreCase = true)
                }.toPersistentList()
            }

            state.copy(
                search = state.search.copy(query = query),
                items = items,
            )
        }
    }

    private fun toggleSearchBar(isActive: Boolean) {
        previewState.update { state ->
            state.copy(
                search = state.search.copy(isActive = isActive),
            )
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by previewState.collectAsState()

        ManageTokensScreen(
            modifier = modifier,
            state = state,
        )
    }

    private fun initItems() = List(size = 30) { index ->
        if (isLoading) {
            CurrencyItemUM.Loading(index)
        } else {
            if (index < 2) {
                getCustomItem(index)
            } else {
                getBasicItem(index)
            }
        }
    }.toPersistentList()

    private fun getCustomItem(index: Int) = CurrencyItemUM.Custom(
        id = ManagedCryptoCurrency.ID(index.toString()),
        name = "Custom token $index",
        symbol = "CT$index",
        icon = CurrencyIconState.CustomTokenIcon(
            tint = Color.White,
            background = Color.Black,
            topBadgeIconResId = R.drawable.img_eth_22,
            isGrayscale = false,
            showCustomBadge = true,
        ),
        onRemoveClick = {},
    )

    private fun getBasicItem(index: Int) = CurrencyItemUM.Basic(
        id = ManagedCryptoCurrency.ID(index.toString()),
        name = "Currency $index",
        symbol = "C$index",
        icon = CurrencyIconState.CoinIcon(
            url = null,
            fallbackResId = R.drawable.img_btc_22,
            isGrayscale = false,
            showCustomBadge = false,
        ),
        networks = if (index == 2) {
            CurrencyItemUM.Basic.NetworksUM.Expanded(getCurrencyNetworks(index))
        } else {
            CurrencyItemUM.Basic.NetworksUM.Collapsed
        },
        onExpandClick = { toggleCurrency(index) },
    )

    private fun getCurrencyNetworks(currencyIndex: Int) = List(size = 3) { networkIndex ->
        val derivationPath = Network.DerivationPath.Card("")
        CurrencyNetworkUM(
            network = Network(
                id = Network.ID(value = networkIndex.toString(), derivationPath = derivationPath),
                backendId = networkIndex.toString(),
                name = "Network $networkIndex",
                currencySymbol = "N$networkIndex",
                derivationPath = derivationPath,
                isTestnet = false,
                standardType = Network.StandardType.ERC20,
                hasFiatFeeRate = false,
                canHandleTokens = false,
                transactionExtrasType = Network.TransactionExtrasType.NONE,
                nameResolvingType = Network.NameResolvingType.NONE,
            ),
            name = "NETWORK$networkIndex",
            type = "N$networkIndex",
            iconResId = R.drawable.ic_eth_16,
            isMainNetwork = networkIndex == 0,
            isSelected = false,
            onSelectedStateChange = { toggleNetwork(currencyIndex, networkIndex, isSelected = it) },
            onLongClick = {},
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
            is CurrencyItemUM.Custom,
            is CurrencyItemUM.Loading,
            is CurrencyItemUM.SearchNothingFound,
            -> return
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
                                        iconResId = if (isSelected) {
                                            R.drawable.img_eth_22
                                        } else {
                                            R.drawable.ic_eth_16
                                        },
                                        isSelected = isSelected,
                                    )
                                }
                            }
                        },
                    )
                    ?: return

                item.copy(networks = updatedNetworks)
            }
            is CurrencyItemUM.Custom,
            is CurrencyItemUM.Loading,
            is CurrencyItemUM.SearchNothingFound,
            -> return
        }

        val id = "${currencyIndex}_$networkIndex"
        if (changedItemsIds.contains(id)) {
            changedItemsIds.remove(id)
        } else {
            changedItemsIds.add(id)
        }

        previewState.update { state ->
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