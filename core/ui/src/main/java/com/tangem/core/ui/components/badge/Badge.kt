package com.tangem.core.ui.components.badge

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

/**
 * Badge component
 *
 * @param iconRes   icon drawable resource
 * @param text      text reference
 * @param modifier  composable modifier
 *
 * @see <a href="https://www.figma.com/design/14ISV23YB1yVW1uNVwqrKv/Android?node-id=3467-2362&t=SnNQ4qhxwj2mhhra-4">Figma</a>
 */
@Composable
fun Badge(@DrawableRes iconRes: Int, text: TextReference, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .border(1.dp, TangemTheme.colors.stroke.primary, RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp),
    ) {
        Icon(
            painter = rememberVectorPainter(
                ImageVector.vectorResource(iconRes),
            ),
            contentDescription = null,
            tint = TangemTheme.colors.icon.informative,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = text.resolveReference(),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Badge_Preview() {
    TangemThemePreview {
        Badge(
            iconRes = R.drawable.ic_star_mini_24,
            text = stringReference("4.9"),
        )
    }
}
// endregion