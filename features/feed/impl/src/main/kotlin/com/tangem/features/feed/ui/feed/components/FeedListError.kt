package com.tangem.features.feed.ui.feed.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.UnableToLoadData
import com.tangem.core.ui.res.LocalMainBottomSheetColor

@Composable
internal fun FeedListGlobalError(onRetryClick: () -> Unit, currentDate: String, modifier: Modifier = Modifier) {
    val background = LocalMainBottomSheetColor.current.value
    Column(modifier) {
        DateBlock(currentDate)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawRect(background) }
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            UnableToLoadData(onRetryClick = onRetryClick)
        }
    }
}