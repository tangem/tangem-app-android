package com.tangem.features.managetokens.component.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.components.currency.tokenicon.TokenIconState
import com.tangem.core.ui.components.rows.model.ChainRowUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.managetokens.component.ManageTokensComponent
import com.tangem.features.managetokens.entity.CurrencyItemUM
import com.tangem.features.managetokens.entity.ManageTokensUM
import com.tangem.features.managetokens.impl.R
import com.tangem.features.managetokens.ui.ManageTokensScreen
import kotlinx.collections.immutable.toImmutableList

internal class PreviewManageTokensComponent : ManageTokensComponent {

    private val previewState: ManageTokensUM = ManageTokensUM(
        popBack = {},
        items = List(size = 30) { index ->
            if (index < 2) {
                CurrencyItemUM.Custom(
                    id = index.toString(),
                    model = ChainRowUM(
                        name = stringReference("Custom $index"),
                        type = stringReference("CM$index"),
                        icon = TokenIconState.CustomTokenIcon(
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
            } else {
                CurrencyItemUM.Basic(
                    id = index.toString(),
                    model = ChainRowUM(
                        name = stringReference("Basic $index"),
                        type = stringReference("BT$index"),
                        icon = TokenIconState.CoinIcon(
                            url = null,
                            fallbackResId = R.drawable.img_btc_22,
                            isGrayscale = false,
                            showCustomBadge = false,
                        ),
                        showCustom = false,
                    ),
                    isExpanded = false,
                    onExpandClick = {},
                )
            }
        }.toImmutableList(),
    )

    @Composable
    override fun Content(modifier: Modifier) {
        ManageTokensScreen(
            modifier = modifier,
            state = previewState,
        )
    }
}