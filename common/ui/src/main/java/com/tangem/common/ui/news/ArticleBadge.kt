package com.tangem.common.ui.news

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

@Composable
internal fun ArticleBadge(articleTagUM: ArticleTagUM, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .heightIn(min = 24.dp)
            .background(
                color = TangemTheme.colors.icon.informative.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        when (articleTagUM) {
            is ArticleTagUM.Category -> Unit
            is ArticleTagUM.Token -> {
                CurrencyIcon(
                    state = articleTagUM.iconState,
                    shouldDisplayNetwork = false,
                    iconSize = 16.dp,
                )
            }
        }
        Text(
            text = articleTagUM.title.resolveReference(),
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.text.secondary,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ArticleBadgePreview() {
    TangemThemePreview {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ArticleBadge(
                articleTagUM = ArticleTagUM.Token(
                    TextReference.Str("BTC"),
                    iconState = CurrencyIconState.CoinIcon(
                        url = "",
                        fallbackResId = 0,
                        isGrayscale = false,
                        shouldShowCustomBadge = false,
                    ),
                ),
            )
            ArticleBadge(
                articleTagUM = ArticleTagUM.Category(TextReference.Str("Regulation")),
            )
        }
    }
}