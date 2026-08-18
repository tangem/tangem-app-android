package com.tangem.common.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
 * Always-expanded question-and-answer block of a feature-onboarding screen.
 *
 * @param question heading of the block.
 * @param answer body under the heading.
 * @param hasTopBorder whether a hairline separates this block from the one above. The caller owns it
 *   because only the caller knows which item is first in its list.
 * @param modifier applied to the block root.
 */
@Composable
fun OnboardingFaqItem(
    question: TextReference,
    answer: TextReference,
    hasTopBorder: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
    ) {
        if (hasTopBorder) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(TangemTheme.colors3.border.secondary),
            )
        }
        Column(
            modifier = Modifier.padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = question.resolveReference(),
                style = TangemTheme.typography3.heading.small,
                color = TangemTheme.colors3.text.primary,
            )
            Text(
                text = answer.resolveReference(),
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        }
    }
}

@Preview(widthDp = 360, showBackground = true)
@Composable
private fun OnboardingFaqItemPreview() {
    TangemThemePreviewRedesign {
        OnboardingFaqItem(
            question = stringReference("What are the risks?"),
            answer = stringReference("Collectible values change and can fall."),
            hasTopBorder = true,
        )
    }
}