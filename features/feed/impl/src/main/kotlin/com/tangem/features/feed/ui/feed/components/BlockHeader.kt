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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.components.buttons.SecondarySmallButton
import com.tangem.core.ui.components.buttons.SmallButtonConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun Header(
    onSeeAllClick: () -> Unit,
    isLoading: Boolean,
    shouldShowSeeAll: Boolean,
    title: @Composable () -> Unit,
) {
    val isRedesignEnabled = LocalRedesignEnabled.current
    AnimatedContent(isLoading) { animatedState ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (animatedState) {
                if (isRedesignEnabled) {
                    RectangleShimmer(
                        modifier = Modifier.size(width = 130.dp, height = 24.dp),
                        radius = TangemTheme.dimens2.x25,
                    )
                } else {
                    RectangleShimmer(modifier = Modifier.size(width = 104.dp, height = 18.dp))
                }
            } else {
                Box(modifier = Modifier.weight(1f)) {
                    title()
                }
                SpacerW(8.dp)
                AnimatedVisibility(shouldShowSeeAll) {
                    if (isRedesignEnabled) {
                        Row(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clickable(onClick = onSeeAllClick),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResourceSafe(R.string.common_see_all),
                                color = TangemTheme.colors2.text.neutral.primary,
                                style = TangemTheme.typography2.bodySemibold16,
                            )
                            Icon(
                                modifier = Modifier.size(24.dp),
                                imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_small_right_24),
                                tint = TangemTheme.colors2.markers.iconGray,
                                contentDescription = null,
                            )
                        }
                    } else {
                        SecondarySmallButton(
                            config = SmallButtonConfig(
                                text = TextReference.Res(R.string.common_see_all),
                                onClick = onSeeAllClick,
                            ),
                        )
                    }
                }
            }
        }
    }
}