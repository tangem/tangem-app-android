@file:Suppress("MagicNumber", "LongMethod")
package com.tangem.feature.tester.presentation.storybook.page.opportunities

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.opportunities.OpportunitiesBG
import com.tangem.core.ui.res.TangemTheme

private data class IconVariant(
    val label: String,
    val icon: TangemIconUM,
)

private val variants = listOf(
    IconVariant(
        label = "Bitcoin",
        icon = TangemIconUM.Currency(
            CurrencyIconState.CoinIcon(
                url = null,
                fallbackResId = R.drawable.img_btc_22,
                isGrayscale = false,
                shouldShowCustomBadge = false,
            ),
        ),
    ),
    IconVariant(
        label = "Solana",
        icon = TangemIconUM.Currency(
            CurrencyIconState.CoinIcon(
                url = null,
                fallbackResId = R.drawable.img_solana_22,
                isGrayscale = false,
                shouldShowCustomBadge = false,
            ),
        ),
    ),
    IconVariant(
        label = "Avalanche",
        icon = TangemIconUM.Currency(
            CurrencyIconState.CoinIcon(
                url = null,
                fallbackResId = R.drawable.img_avalanche_22,
                isGrayscale = false,
                shouldShowCustomBadge = false,
            ),
        ),
    ),
    IconVariant(
        label = "BNB Smart Chain",
        icon = TangemIconUM.Currency(
            CurrencyIconState.CoinIcon(
                url = null,
                fallbackResId = R.drawable.img_bsc_22,
                isGrayscale = false,
                shouldShowCustomBadge = false,
            ),
        ),
    ),
    IconVariant(
        label = "Cardano",
        icon = TangemIconUM.Currency(
            CurrencyIconState.CoinIcon(
                url = null,
                fallbackResId = R.drawable.img_cardano_22,
                isGrayscale = false,
                shouldShowCustomBadge = false,
            ),
        ),
    ),
)

@Composable
internal fun OpportunitiesBGStory(modifier: Modifier = Modifier) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors2.surface.level1),
    ) {
        variants.forEach { variant ->
            item(variant.label) {
                OpportunitiesBG(
                    icon = variant.icon,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                    ) {
                        TangemIcon(
                            tangemIconUM = variant.icon,
                            modifier = Modifier.size(40.dp),
                        )
                        Text(
                            text = variant.label,
                            style = TangemTheme.typography.subtitle1,
                            color = TangemTheme.colors.text.primary1,
                        )
                    }
                }
            }
        }
    }
}