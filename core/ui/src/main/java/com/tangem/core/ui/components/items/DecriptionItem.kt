package com.tangem.core.ui.components.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemThemePreviewRedesign
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
    if (LocalRedesignEnabled.current) {
        DescriptionItemV2(
            description = description,
            hasFullDescription = hasFullDescription,
            onReadMoreClick = onReadMoreClick,
            modifier = modifier,
        )
    } else {
        DescriptionItemV1(
            description = description,
            hasFullDescription = hasFullDescription,
            onReadMoreClick = onReadMoreClick,
            modifier = modifier,
            textStyle = textStyle,
        )
    }
}

@Composable
private fun DescriptionItemV1(
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
private fun DescriptionItemV2(
    description: TextReference,
    hasFullDescription: Boolean,
    onReadMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (hasFullDescription) {
        val text = buildAnnotatedString {
            withStyle(SpanStyle(color = TangemTheme.colors2.text.neutral.tertiary)) {
                append(description.resolveReference())
            }
            withStyle(SpanStyle(color = TangemTheme.colors2.text.neutral.primary)) {
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
            style = TangemTheme.typography2.bodyRegular15,
        )
    } else {
        Text(
            modifier = modifier,
            text = description.resolveReference(),
            style = TangemTheme.typography2.bodyRegular15,
            color = TangemTheme.colors2.text.neutral.tertiary,
        )
    }
}

@Composable
fun DescriptionPlaceholder(modifier: Modifier = Modifier) {
    if (LocalRedesignEnabled.current) {
        DescriptionPlaceholderV2(modifier)
    } else {
        DescriptionPlaceholderV1(modifier)
    }
}

@Composable
private fun DescriptionPlaceholderV1(modifier: Modifier = Modifier) {
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

@Composable
private fun DescriptionPlaceholderV2(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TextShimmer(
            modifier = Modifier.fillMaxWidth(),
            style = TangemTheme.typography2.bodyRegular15,
            radius = TangemTheme.dimens2.x25,
        )
        TextShimmer(
            modifier = Modifier.fillMaxWidth(),
            style = TangemTheme.typography2.bodyRegular15,
            radius = TangemTheme.dimens2.x25,
        )
        TextShimmer(
            modifier = Modifier.fillMaxWidth(fraction = 0.8f),
            style = TangemTheme.typography2.bodyRegular15,
            radius = TangemTheme.dimens2.x25,
        )
    }
}

@Preview
@Composable
private fun ContentPreviewV1() {
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
private fun ContentPreviewV2() {
    CompositionLocalProvider(LocalRedesignEnabled provides true) {
        TangemThemePreviewRedesign {
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
}

@Preview
@Composable
private fun PreviewPlaceholderV1() {
    TangemThemePreview {
        PreviewShimmerContainer(
            actualContent = {
                ContentPreviewV1()
            },
            shimmerContent = {
                DescriptionPlaceholder()
            },
        )
    }
}

@Preview
@Composable
private fun PreviewPlaceholderV2() {
    CompositionLocalProvider(LocalRedesignEnabled provides true) {
        TangemThemePreviewRedesign {
            PreviewShimmerContainer(
                actualContent = {
                    ContentPreviewV2()
                },
                shimmerContent = {
                    DescriptionPlaceholder()
                },
            )
        }
    }
}