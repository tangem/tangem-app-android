package com.tangem.feature.wallet.presentation.wallet.ui.components.visa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun TangemPayLoadingScreenBlock(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = TangemTheme.colors.background.primary, shape = TangemTheme.shapes.roundedCornersXMedium)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleShimmer(modifier = Modifier.size(36.dp))
        Column(
            modifier = Modifier.padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            RectangleShimmer(modifier = Modifier.padding(vertical = 4.dp).sizeIn(minWidth = 70.dp, minHeight = 12.dp))
            RectangleShimmer(modifier = Modifier.padding(vertical = 2.dp).sizeIn(minWidth = 52.dp, minHeight = 12.dp))
        }
        SpacerWMax()
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            RectangleShimmer(modifier = Modifier.padding(vertical = 4.dp).sizeIn(minWidth = 40.dp, minHeight = 12.dp))
            RectangleShimmer(modifier = Modifier.padding(vertical = 2.dp).sizeIn(minWidth = 40.dp, minHeight = 12.dp))
        }
    }
}