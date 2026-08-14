package com.tangem.features.feed.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

@Composable
internal fun TokenMarketInformationBlock(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    contentPadding: Dp = 16.dp,
    title: (@Composable () -> Unit),
    content: (@Composable BoxScope.() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(color = TangemTheme.colors3.bg.secondary)
            .padding(contentPadding),
        horizontalAlignment = Alignment.Start,
    ) {
        title()

        if (content != null) {
            Box(modifier = Modifier.fillMaxWidth()) {
                content(this)
            }
        }
    }
}

@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TokenMarketInformationBlockPreview() {
    TangemThemePreviewRedesign {
        TokenMarketInformationBlock(
            title = {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Listed on",
                        style = TangemTheme.typography3.heading.medium,
                        color = TangemTheme.colors3.text.primary,
                    )

                    SpacerWMax()

                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_24),
                        tint = TangemTheme.colors3.icon.secondary,
                        contentDescription = null,
                    )
                }
            },
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TangemTheme.colors3.bg.primary)
                        .padding(12.dp),
                ) {
                    Text(
                        text = "Smth",
                        color = TangemTheme.colors3.text.primary,
                    )
                }
            },
        )
    }
}