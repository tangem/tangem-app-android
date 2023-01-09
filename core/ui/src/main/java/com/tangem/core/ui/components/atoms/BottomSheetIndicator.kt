package com.tangem.core.ui.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.res.IconColorType
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.iconColor

/**
 * Bottom sheet indicator
 *
 * @see <a href="https://www.figma.com/file/17JyRbuUEZ42DluaFEuGQk/Atoms?node-id=135%3A25&t=3dvxYMZBox4jxJc1-4">
 * Figma Component</a>
 */
@Deprecated(message = "Use Hand component instead")
@Composable
fun BottomSheetIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(TangemTheme.dimens.size20),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(TangemTheme.dimens.size32)
                .height(TangemTheme.dimens.size4)
                .background(
                    color = MaterialTheme.colors.iconColor(type = IconColorType.INACTIVE),
                    shape = RoundedCornerShape(TangemTheme.dimens.radius2),
                ),
        )
    }
}

@Preview(heightDp = 20, showBackground = true)
@Composable
fun Preview_BottomSheetIndicator_InLightTheme() {
    TangemTheme(isDark = false) {
        BottomSheetIndicator()
    }
}

@Preview(heightDp = 20, showBackground = true)
@Composable
fun Preview_BottomSheetIndicator_InDarkTheme() {
    TangemTheme(isDark = true) {
        BottomSheetIndicator()
    }
}
