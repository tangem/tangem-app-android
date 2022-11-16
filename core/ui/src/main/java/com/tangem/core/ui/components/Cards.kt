package com.tangem.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TextColorType
import com.tangem.core.ui.res.textColor

@Preview(widthDp = 328, heightDp = 48, showBackground = true)
@Composable
fun Preview_SimpleInfoCard_InLightTheme() {
    TangemTheme(isDarkTheme = false) {
        SmallInfoCard(startText = "Balance", endText = "0.4405434 BTC")
    }
}

@Preview(widthDp = 328, heightDp = 48, showBackground = true)
@Composable
fun Preview_SimpleInfoCard_InDarkTheme() {
    TangemTheme(isDarkTheme = true) {
        SmallInfoCard(startText = "Balance", endText = "0.4405434 BTC")
    }
}

/**
 * Small card with text information attached to the edges
 *
 * @param startText text information attached to the left edge
 * @param endText   text information attached to the right edge
 *
 * @see <a href = "https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?node-id=290%3A339&t=WdN5XpixzZLlQAZO-4"
 * >Figma component</a>
 */
@Composable
fun SmallInfoCard(startText: String, endText: String) {
    Surface(
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.radius12)),
        color = MaterialTheme.colors.secondary,
        elevation = dimensionResource(id = R.dimen.elevation2),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.size48))
                .padding(
                    horizontal = dimensionResource(id = R.dimen.spacing16),
                    vertical = dimensionResource(id = R.dimen.spacing12),
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = startText,
                color = MaterialTheme.colors.textColor(type = TextColorType.TERTIARY),
                maxLines = 1,
                style = MaterialTheme.typography.subtitle2
            )
            Text(
                text = endText,
                color = MaterialTheme.colors.textColor(type = TextColorType.PRIMARY1),
                maxLines = 1,
                style = MaterialTheme.typography.body2
            )
        }
    }
}
