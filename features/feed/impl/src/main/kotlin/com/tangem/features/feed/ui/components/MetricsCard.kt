package com.tangem.features.feed.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

@Composable
internal fun MetricsCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cardColor: Color = TangemTheme.colors3.bg.secondary,
    title: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(color = cardColor)
            .conditional(
                condition = onClick != null,
                modifier = {
                    if (onClick != null) {
                        clickable(onClick = onClick)
                    } else {
                        this
                    }
                },
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        title()
        content()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MetricsCardPreview() {
    TangemThemePreviewRedesign {
        MetricsCard(
            modifier = Modifier.heightIn(120.dp),
            title = {
                Text(
                    text = "$ 22.4 M",
                    style = TangemTheme.typography3.heading.medium,
                    color = TangemTheme.colors3.text.primary,
                )
            },
            content = {
                Text(
                    text = "Market cap",
                    style = TangemTheme.typography3.caption.medium,
                    color = TangemTheme.colors3.text.secondary,
                )
            },
            onClick = {},
        )
    }
}