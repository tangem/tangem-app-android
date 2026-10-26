package com.tangem.features.polymarket.impl.main.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import kotlin.math.ceil

@Composable
internal fun PolymarketCategoryTabsSkeleton(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val tabCount = ceil(maxWidth / 68.dp).toInt()
        LazyRow(
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(count = tabCount) {
                TangemShimmer(modifier = Modifier.size(width = 64.dp, height = 36.dp), radius = 18.dp)
            }
        }
    }
}