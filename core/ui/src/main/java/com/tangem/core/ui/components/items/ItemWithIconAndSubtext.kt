package com.tangem.core.ui.components.items

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

@Composable
fun ItemWithIconAndSubtext(icon: Int, name: String, symbol: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            modifier = Modifier
                .padding(end = 8.dp)
                .size(24.dp),
            painter = painterResource(icon),
            contentDescription = null,
        )
        Text(
            modifier = Modifier.weight(1f, fill = false),
            text = name,
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.primary1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(text = symbol, style = TangemTheme.typography.caption1, color = TangemTheme.colors.text.tertiary)
    }
}

@Composable
fun ItemWithIconAndSubtextShimmer(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleShimmer(
            modifier = Modifier
                .padding(end = 8.dp)
                .size(24.dp),
        )
        TextShimmer(
            style = TangemTheme.typography.subtitle2,
            modifier = Modifier.width(70.dp),
        )
    }
}

@Preview
@Composable
private fun ContentPreview() {
    TangemThemePreview {
        Column {
            ItemWithIconAndSubtextShimmer()
            ItemWithIconAndSubtext(
                icon = R.drawable.ic_solana_16,
                name = "Solana",
                symbol = "SOL",
            )
        }
    }
}