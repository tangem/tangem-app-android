package com.tangem.features.feed.ui.v2

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton

@Composable
internal fun FeedNavActionButton(
    icon: ImageVector,
    onClick: () -> Unit,
    isEnabled: Boolean = true,
    isDimmed: Boolean = false,
) {
    TangemButton(
        variant = TangemButton.Variant.Material,
        size = TangemButton.Size.X11,
        isEnabled = isEnabled,
        isDimmed = isDimmed,
        iconStart = TangemIconUM.Icon(icon),
        onClick = onClick,
    )
}