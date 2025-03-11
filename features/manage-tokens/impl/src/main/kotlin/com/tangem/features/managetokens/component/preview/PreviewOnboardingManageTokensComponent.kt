package com.tangem.features.managetokens.component.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.features.managetokens.component.OnboardingManageTokensComponent
import com.tangem.features.managetokens.entity.item.CurrencyItemUM
import com.tangem.features.managetokens.entity.item.CurrencyNetworkUM
import com.tangem.features.managetokens.entity.managetokens.OnboardingManageTokensUM
import com.tangem.features.managetokens.impl.R
import com.tangem.features.managetokens.ui.OnboardingManageTokensContent
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList

internal class PreviewOnboardingManageTokensComponent(
    private val isLoading: Boolean = false,
) : OnboardingManageTokensComponent {

    private val state = OnboardingManageTokensUM(
        items = initItems(),
        isInitialBatchLoading = false,
        isNextBatchLoading = true,
        loadMore = { false },
        onBack = {},
        search = SearchBarUM(
            placeholderText = resourceReference(R.string.manage_tokens_search_placeholder),
            query = "",
            onQueryChange = {},
            isActive = false,
            onActiveChange = { },
        ),
        actionButtonConfig = OnboardingManageTokensUM.ActionButtonConfig.Later(
            onClick = {},
            showProgress = false,
        ),
    )

    @Composable
    override fun Content(modifier: Modifier) {
        OnboardingManageTokensContent(state = state, modifier = modifier)
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
            CurrencyItemUM.Basic.NetworksUM.Expanded(getCurrencyNetworks())
        } else {
            CurrencyItemUM.Basic.NetworksUM.Collapsed
        },
        onExpandClick = {},
    )

    private fun getCurrencyNetworks() = List(size = 3) { networkIndex ->
        CurrencyNetworkUM(
            network = Network(
                id = Network.ID(networkIndex.toString()),
                backendId = networkIndex.toString(),
                name = "Network $networkIndex",
                currencySymbol = "N$networkIndex",
                derivationPath = Network.DerivationPath.Card(""),
                isTestnet = false,
                standardType = Network.StandardType.ERC20,
                hasFiatFeeRate = false,
                canHandleTokens = false,
                transactionExtrasType = Network.TransactionExtrasType.NONE,
            ),
            name = "NETWORK$networkIndex",
            type = "N$networkIndex",
            iconResId = R.drawable.ic_eth_16,
            isMainNetwork = networkIndex == 0,
            isSelected = false,
            onSelectedStateChange = {},
            onLongClick = {},
        )
    }.toImmutableList()
}