package com.tangem.features.pushnotificationsettings.impl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun PushNotificationSettingsLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = TangemTheme.dimens.spacing16,
                vertical = TangemTheme.dimens.spacing12,
            ),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
    ) {
        repeat(SHIMMER_ROW_COUNT) {
            ShimmerRow()
        }
    }
}

@Composable
private fun ShimmerRow(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
    ) {
        BlockCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = TangemTheme.dimens.spacing16,
                        vertical = TangemTheme.dimens.spacing12,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RectangleShimmer(
                    modifier = Modifier
                        .width(SHIMMER_TITLE_WIDTH)
                        .height(SHIMMER_TITLE_HEIGHT),
                )
                RectangleShimmer(
                    modifier = Modifier
                        .width(SHIMMER_SWITCH_WIDTH)
                        .height(SHIMMER_SWITCH_HEIGHT),
                    radius = TangemTheme.dimens.radius12,
                )
            }
        }
        RectangleShimmer(
            modifier = Modifier
                .padding(horizontal = TangemTheme.dimens.spacing16)
                .width(SHIMMER_SUBTITLE_WIDTH)
                .height(SHIMMER_SUBTITLE_HEIGHT),
        )
    }
}

private const val SHIMMER_ROW_COUNT = 3
private val SHIMMER_TITLE_WIDTH = 160.dp
private val SHIMMER_TITLE_HEIGHT = 18.dp
private val SHIMMER_SWITCH_WIDTH = 40.dp
private val SHIMMER_SWITCH_HEIGHT = 22.dp
private val SHIMMER_SUBTITLE_WIDTH = 220.dp
private val SHIMMER_SUBTITLE_HEIGHT = 14.dp