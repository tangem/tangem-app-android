package com.tangem.features.feed.ui.news.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerH

@Composable
fun NewsDetailsPlaceholder(background: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().background(background).padding(16.dp),
    ) {
        RectangleShimmer(modifier = Modifier.size(width = 112.dp, height = 20.dp))
        SpacerH(8.dp)
        RectangleShimmer(
            modifier = Modifier.fillMaxWidth().height(28.dp),
        )
        SpacerH(4.dp)
        RectangleShimmer(modifier = Modifier.size(height = 28.dp, width = 208.dp))
        SpacerH(20.dp)
        RectangleShimmer(modifier = Modifier.size(height = 36.dp, width = 99.dp), radius = 12.dp)
        SpacerH(32.dp)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RectangleShimmer(
                modifier = Modifier.fillMaxWidth().height(20.dp),
            )
            RectangleShimmer(
                modifier = Modifier.fillMaxWidth().height(20.dp).padding(end = 30.dp),
            )
            RectangleShimmer(
                modifier = Modifier.fillMaxWidth().height(20.dp).padding(end = 30.dp),
            )
            RectangleShimmer(
                modifier = Modifier.fillMaxWidth().height(20.dp).padding(end = 24.dp),
            )
            RectangleShimmer(
                modifier = Modifier.fillMaxWidth().height(20.dp).padding(end = 70.dp),
            )
            RectangleShimmer(
                modifier = Modifier.fillMaxWidth().height(20.dp).padding(end = 30.dp),
            )
            RectangleShimmer(
                modifier = Modifier.fillMaxWidth().height(20.dp).padding(end = 96.dp),
            )
            RectangleShimmer(
                modifier = Modifier.fillMaxWidth().height(20.dp).padding(end = 100.dp),
            )
        }
    }
}