package com.tangem.core.ui.components.bottomsheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.res.TangemTheme

@Composable
fun TangemBottomSheetDraggableHeader(color: Color = TangemTheme.colors.background.primary) {
    Surface(
        modifier = Modifier.height(TangemTheme.dimens.size20),
        color = color,
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = TangemTheme.dimens.spacing8)
                .size(
                    width = TangemTheme.dimens.size32,
                    height = TangemTheme.dimens.size4,
                )
                .background(
                    color = TangemTheme.colors.icon.inactive,
                    shape = TangemTheme.shapes.roundedCornersSmall,
                ),
        )
    }
}
