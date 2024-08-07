package com.tangem.features.markets.details.impl.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.PreviewShimmerContainer
import com.tangem.features.markets.impl.R

@Composable
internal fun Description(
    description: TextReference,
    hasFullDescription: Boolean,
    onReadMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (hasFullDescription) {
        val text = buildAnnotatedString {
            withStyle(SpanStyle(color = TangemTheme.colors.text.secondary)) {
                append(description.resolveReference())
            }
            withStyle(SpanStyle(color = TangemTheme.colors.text.accent)) {
                append(" " + stringResource(R.string.common_read_more))
            }
        }

        ClickableText(
            modifier = modifier,
            text = text,
            style = TangemTheme.typography.body2,
        ) {
            text.spanStyles.getOrNull(1)?.let { spanStyle ->
                if (it in spanStyle.start..spanStyle.end) {
                    onReadMoreClick()
                }
            }
        }
    } else {
        Text(
            modifier = modifier,
            text = description.resolveReference(),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.secondary,
        )
    }
}

@Composable
internal fun DescriptionPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
    ) {
        TextShimmer(
            modifier = Modifier.fillMaxWidth(),
            style = TangemTheme.typography.body2,
            textSizeHeight = true,
        )
        TextShimmer(
            modifier = Modifier.fillMaxWidth(),
            style = TangemTheme.typography.body2,
            textSizeHeight = true,
        )
        TextShimmer(
            modifier = Modifier.fillMaxWidth(fraction = 0.8f),
            style = TangemTheme.typography.body2,
            textSizeHeight = true,
        )
    }
}

@Preview
@Composable
private fun ContentPreview() {
    TangemThemePreview {
        Description(
            description = stringReference(
                "XRP (XRP) is a cryptocurrency launched in January 2009, where the first " +
                    "genesis block was mined on 9th January 2009",
            ),
            hasFullDescription = true,
            onReadMoreClick = {},
        )
    }
}

@Preview
@Composable
private fun PreviewPlaceholder() {
    TangemThemePreview {
        PreviewShimmerContainer(
            actualContent = {
                ContentPreview()
            },
            shimmerContent = {
                DescriptionPlaceholder()
            },
        )
    }
}
