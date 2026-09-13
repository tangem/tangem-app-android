package com.tangem.common.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

/**
 * Title and subtitle opening a feature-onboarding screen, above the benefit cards.
 *
 * @param title headline of the screen.
 * @param subtitle supporting paragraph under the headline.
 * @param modifier applied to the root column.
 */
@Composable
fun OnboardingHeadline(title: TextReference, subtitle: TextReference, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title.resolveReference(),
            style = TangemTheme.typography3.heading.medium,
            color = TangemTheme.colors3.text.primary,
        )
        Text(
            text = subtitle.resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Composable
private fun OnboardingHeadlinePreview() {
    TangemThemePreviewRedesign {
        OnboardingHeadline(
            title = stringReference("Open Gacha account"),
            subtitle = stringReference("Buy sealed packs of real graded collectible cards"),
        )
    }
}