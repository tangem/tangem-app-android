package com.tangem.features.feed.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.ui.LocalIsOpenedInBottomSheet
import com.tangem.features.feed.ui.feed.state.FeedListSearchBar

@Composable
internal fun FeedSearchBar(
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
        type = if (LocalIsOpenedInBottomSheet.current) TangemTopBarType.BottomSheet else TangemTopBarType.Default,
        reserveSlotSpace = false,
        content = {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = if (startContent != null) 12.dp else 0.dp,
                        end = if (endContent != null) 12.dp else 0.dp,
                    )
                    .clip(CircleShape)
                    .hazeEffectTangem {
                        blurRadius = 8.dp
                    }
                    .conditional(condition = isSearchBarClickable) {
                        clickable(onClick = feedListSearchBar.onBarClick)
                    }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_search_default_24),
                    tint = TangemTheme.colors3.icon.secondary,
                    contentDescription = null,
                )

                SpacerW(4.dp)

                Text(
                    text = feedListSearchBar.placeholderText.resolveReference(),
                    style = TangemTheme.typography3.body.medium,
                    color = TangemTheme.colors3.text.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
    )
}