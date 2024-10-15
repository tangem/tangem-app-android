package com.tangem.features.markets.details.impl.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.common.ui.R
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.block.information.InformationBlock
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.markets.details.impl.ui.state.ListedOnUM
import kotlinx.coroutines.delay

/**
 * "Listed on" block
 *
 * @param state block state
 *
[REDACTED_AUTHOR]
 */
@Composable
internal fun ListedOnBlock(state: ListedOnUM, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        InformationBlock(
            title = {
                Text(
                    text = state.title.resolveReference(),
                    style = TangemTheme.typography.subtitle2,
                    color = TangemTheme.colors.text.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            modifier = Modifier
                .clip(shape = TangemTheme.shapes.roundedCornersXMedium)
                .clickable(enabled = state is ListedOnUM.Content) {
                    (state as? ListedOnUM.Content)?.onClick?.invoke()
                },
        ) {
            Description(
                state = state,
                modifier = Modifier.padding(bottom = TangemTheme.dimens.spacing12),
            )
        }

        if (state is ListedOnUM.Content) {
            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_right_24),
                contentDescription = null,
                tint = TangemTheme.colors.icon.informative,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = TangemTheme.dimens.spacing12),
            )
        }
    }
}

@Composable
internal fun ListedOnBlockPlaceholder(modifier: Modifier = Modifier) {
    InformationBlock(
        title = {
            TextShimmer(
                style = TangemTheme.typography.subtitle2,
                modifier = Modifier.fillMaxWidth(fraction = 0.5f),
            )
        },
        modifier = modifier,
    ) {
        TextShimmer(
            style = TangemTheme.typography.body2,
            modifier = Modifier
                .fillMaxWidth(fraction = 0.3f)
                .padding(bottom = TangemTheme.dimens.spacing12),
        )
    }
}

@Composable
private fun Description(state: ListedOnUM, modifier: Modifier = Modifier) {
    Text(
        text = state.description.resolveReference(),
        modifier = modifier,
        color = TangemTheme.colors.text.tertiary,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        style = TangemTheme.typography.body2,
    )
}

@Preview(widthDp = 328, heightDp = 68)
@Preview(name = "Dark Theme", widthDp = 328, heightDp = 68, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ListedOnBlock(@PreviewParameter(ListenOnUMProvider::class) state: ListedOnUM?) {
    TangemThemePreview {
        if (state == null) {
            ListedOnBlockPlaceholder()
        } else {
            ListedOnBlock(state = state)
        }
    }
}

@Preview
@Composable
private fun Preview_ListedOnBlock_StateChanging() {
    var state by remember { mutableStateOf<ListedOnUM?>(value = null) }

    Preview_ListedOnBlock(state = state)

    LaunchedEffect(key1 = null) {
        delay(timeMillis = 3000)

        state = ListedOnUM.Empty
    }
}

private class ListenOnUMProvider : CollectionPreviewParameterProvider<ListedOnUM?>(
    collection = listOf(
        ListedOnUM.Empty,
        ListedOnUM.Content(onClick = {}, amount = 5),
        null,
    ),
)