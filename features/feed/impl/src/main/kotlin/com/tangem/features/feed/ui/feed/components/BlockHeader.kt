package com.tangem.features.feed.ui.feed.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_right_24

@Composable
internal fun ColumnScope.Header(
    onSeeAllClick: () -> Unit,
    isLoading: Boolean,
    shouldShowSeeAll: Boolean,
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
) {
    SpacerH(12.dp)
    AnimatedContent(
        targetState = isLoading,
        modifier = modifier,
    ) { animatedState ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (animatedState) {
                val lineHeight = with(LocalDensity.current) {
                    TangemTheme.typography3.heading.small.lineHeight.toDp()
                }
                TangemShimmer(
                    radius = 16.dp,
                    modifier = Modifier
                        .width(130.dp)
                        .height(lineHeight)
                        .padding(vertical = 2.dp),
                )
            } else {
                Box(modifier = Modifier.weight(1f)) {
                    title()
                }
                SpacerW(8.dp)
                AnimatedVisibility(shouldShowSeeAll) {
                    Row(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clickable(onClick = onSeeAllClick),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResourceSafe(R.string.common_see_all),
                            color = TangemTheme.colors3.text.primary,
                            style = TangemTheme.typography3.body.medium,
                        )
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = Icons.ic_chevron_right_24,
                            tint = TangemTheme.colors3.icon.secondary,
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }
}