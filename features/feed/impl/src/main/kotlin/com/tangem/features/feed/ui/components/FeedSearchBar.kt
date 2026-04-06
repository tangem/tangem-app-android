package com.tangem.features.feed.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.ui.feed.state.FeedListSearchBar

@Composable
internal fun FeedSearchBar(
    isSearchBarClickable: Boolean,
    feedListSearchBar: FeedListSearchBar,
    modifier: Modifier = Modifier,
    startContent: @Composable (() -> Unit)? = null,
    endContent: @Composable (() -> Unit)? = null,
) {
    if (LocalRedesignEnabled.current) {
        FeedSearchBarV2(
            isSearchBarClickable = isSearchBarClickable,
            feedListSearchBar = feedListSearchBar,
            modifier = modifier,
            startContent = startContent,
            endContent = endContent,
        )
    } else {
        FeedSearchBarV1(
            isSearchBarClickable = isSearchBarClickable,
            feedListSearchBar = feedListSearchBar,
            modifier = modifier,
        )
    }
}

@Composable
private fun FeedSearchBarV1(
    isSearchBarClickable: Boolean,
    feedListSearchBar: FeedListSearchBar,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(36.dp))
            .background(color = TangemTheme.colors.field.focused)
            .conditional(condition = isSearchBarClickable) {
                clickable(onClick = feedListSearchBar.onBarClick)
            }
            .padding(14.dp),
    ) {
        Icon(
            modifier = Modifier.size(TangemTheme.dimens.size20),
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_search_24),
            tint = TangemTheme.colors.icon.informative,
            contentDescription = null,
        )

        SpacerW(14.dp)

        Text(
            text = feedListSearchBar.placeholderText.resolveReference(),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}

@Composable
private fun FeedSearchBarV2(
    isSearchBarClickable: Boolean,
    feedListSearchBar: FeedListSearchBar,
    modifier: Modifier = Modifier,
    startContent: @Composable (() -> Unit)? = null,
    endContent: @Composable (() -> Unit)? = null,
) {
    TangemTopBar(
        modifier = modifier,
        startContent = startContent,
        endContent = endContent,
        type = TangemTopBarType.BottomSheet,
        reserveSlotSpace = false,
        content = {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = if (startContent != null) TangemTheme.dimens2.x3 else 0.dp,
                        end = if (endContent != null) TangemTheme.dimens2.x3 else 0.dp,
                    )
                    .clip(CircleShape)
                    .background(color = TangemTheme.colors2.button.backgroundSecondary)
                    .conditional(condition = isSearchBarClickable) {
                        clickable(onClick = feedListSearchBar.onBarClick)
                    }
                    .padding(TangemTheme.dimens2.x3),
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    modifier = Modifier.size(TangemTheme.dimens2.x5),
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_search_default_24),
                    tint = TangemTheme.colors2.markers.iconGray,
                    contentDescription = null,
                )

                SpacerW(TangemTheme.dimens2.x1)

                Text(
                    text = feedListSearchBar.placeholderText.resolveReference(),
                    style = TangemTheme.typography2.bodySemibold16,
                    color = TangemTheme.colors2.text.neutral.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
    )
}