package com.tangem.core.ui.ds.topbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.conditionalCompose
import com.tangem.core.ui.res.TangemTheme

/**
 * Composable for top bar action content
 *
 * @param actionUM user model for top bar action
 * @param modifier modifier for this composable
 * @param type type of top bar, affects size and padding of action content
 */
@Composable
fun TangemTopBarActionContent(
    actionUM: TangemTopBarActionUM,
    modifier: Modifier = Modifier,
    type: TangemTopBarType = TangemTopBarType.Default,
) {
    val background = lerp(
        start = Color.Transparent,
        stop = TangemTheme.colors2.button.backgroundSecondary,
        fraction = actionUM.ghostModeProgress,
    )
    val padding = (TangemTheme.dimens2.x11 - type.getSideContentSize()) / 2

    Icon(
        imageVector = ImageVector.vectorResource(id = actionUM.iconRes),
        contentDescription = null,
        tint = TangemTheme.colors2.graphic.neutral.primary,
        modifier = modifier
            .size(TangemTheme.dimens2.x11)
            .clip(CircleShape)
            .conditional(actionUM.isActionable) { background(background) }
            .conditionalCompose(actionUM.isActionable && actionUM.onClick != null) {
                clickableSingle(onClick = requireNotNull(actionUM.onClick))
            }
            .padding(padding),
    )
}