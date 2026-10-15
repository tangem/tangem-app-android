package com.tangem.core.ui.components.bottomsheets.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.MainScreenTestTags

@Composable
fun TangemBottomSheetDraggableHeaderLegacy(color: Color = TangemTheme.colors.background.primary) {
    Surface(
        modifier = Modifier.height(20.dp),
        color = color,
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .size(
                    width = 32.dp,
                    height = 4.dp,
                )
                .background(
                    color = TangemTheme.colors.icon.inactive,
                    shape = TangemTheme.shapes.roundedCornersSmall,
                ),
        )
    }
}

@Composable
fun TangemBottomSheetDraggableHeader() {
    Box(
        modifier = Modifier
            .testTag(MainScreenTestTags.MARKETS_SHEET_DRAG_HANDLE)
            .height(12.dp)
            .padding(vertical = 4.dp)
            .size(
                width = 40.dp,
                height = 4.dp,
            )
            .background(
                color = TangemTheme.colors3.icon.tertiary,
                shape = RoundedCornerShape(2.dp),
            ),
    )
}