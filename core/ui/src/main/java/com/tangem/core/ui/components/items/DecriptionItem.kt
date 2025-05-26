package com.tangem.core.ui.components.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.PreviewShimmerContainer
import com.tangem.utils.StringsSigns

@Composable
fun DescriptionItem(
    description: TextReference,
    hasFullDescription: Boolean,
    onReadMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TangemTheme.typography.body2,
) {
    if (hasFullDescription) {
        val text = buildAnnotatedString {
            withStyle(SpanStyle(color = TangemTheme.colors.text.secondary)) {
                append(description.resolveReference())
            }
            withStyle(SpanStyle(color = TangemTheme.colors.text.accent)) {
                append(
                    " " + stringResourceSafe(R.string.common_read_more).replace(
                        ' ',
                        StringsSigns.NON_BREAKING_SPACE,
                    ),
                )
            }
        }

        Text(
            modifier = modifier
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = onReadMoreClick,
                ),
            text = text,
            style = textStyle,
        )
    } else {
        Text(
            modifier = modifier,
            text = description.resolveReference(),
            style = textStyle,
            color = TangemTheme.colors.text.secondary,
        )
    }
}

@Composable
public fun DescriptionPlaceholder(modifier: Modifier = Modifier) {
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
        DescriptionItem(
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