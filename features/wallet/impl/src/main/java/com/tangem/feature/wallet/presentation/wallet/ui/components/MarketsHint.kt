package com.tangem.feature.wallet.presentation.wallet.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.feature.wallet.impl.R

private const val STARS_INLINE_CONTENT_ID = "stars"

@Composable
internal fun MarketsHint(isVisible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        modifier = modifier,
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = 300)),
        exit = fadeOut(animationSpec = tween(durationMillis = 300)),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Swipe up to explore the market", // todo redesign main lokalise
                style = TangemTheme.typography2.bodyRegular14,
                color = TangemTheme.colors2.text.neutral.primary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = buildAnnotatedString {
                    append("Find new hidden gems ") // todo redesign main lokalise
                    appendInlineContent(
                        STARS_INLINE_CONTENT_ID,
                        alternateText = "\uDBC0\uDDBF",
                    )
                },
                inlineContent = mapOf(
                    STARS_INLINE_CONTENT_ID to InlineTextContent(
                        placeholder = Placeholder(
                            width = TangemTheme.typography2.bodyRegular14.fontSize,
                            height = TangemTheme.typography2.bodyRegular14.fontSize,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                        ),
                        children = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_magic_default_24),
                                tint = TangemTheme.colors2.text.neutral.tertiary,
                                contentDescription = null,
                            )
                        },
                    ),
                ),
                style = TangemTheme.typography2.bodyRegular14,
                color = TangemTheme.colors2.text.neutral.tertiary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun MarketsHint_Preview() {
    TangemThemePreviewRedesign {
        MarketsHint(
            isVisible = true,
        )
    }
}
// endregion