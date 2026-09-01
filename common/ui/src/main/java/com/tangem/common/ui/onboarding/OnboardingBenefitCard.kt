package com.tangem.common.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_lightning_24

/**
 * Single selling point of a feature-onboarding screen: an icon over a two-line caption, on a filled card.
 *
 * Laid out in a grid by the caller. Cards in one row are expected to share a height — give each
 * `Modifier.weight(1f).fillMaxHeight()` inside a `Row` measured with [androidx.compose.foundation.layout
 * .IntrinsicSize.Min], so the icons of neighbouring cards stay aligned.
 *
 * @param icon 24dp glyph pinned to the top of the card.
 * @param title first caption line.
 * @param subtitle second caption line, pushed to the card's bottom edge.
 * @param modifier applied to the card root; the caller sets the sizing.
 */
@Composable
fun OnboardingBenefitCard(
    icon: ImageVector,
    title: TextReference,
    subtitle: TextReference,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(color = TangemTheme.colors3.bg.secondary, shape = RoundedCornerShape(24.dp))
            .padding(start = 16.dp, end = 24.dp, top = 16.dp, bottom = 16.dp)
            .heightIn(min = 100.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            imageVector = icon,
            contentDescription = null,
            tint = TangemTheme.colors3.icon.primary,
        )
        Column {
            Text(
                text = title.resolveReference(),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.primary,
            )
            Text(
                text = subtitle.resolveReference(),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        }
    }
}

@Preview(widthDp = 180, showBackground = true)
@Composable
private fun OnboardingBenefitCardPreview() {
    TangemThemePreviewRedesign {
        OnboardingBenefitCard(
            icon = Icons.ic_lightning_24,
            title = stringReference("Tapless pack opening"),
            subtitle = stringReference("No card scan needed to open"),
        )
    }
}