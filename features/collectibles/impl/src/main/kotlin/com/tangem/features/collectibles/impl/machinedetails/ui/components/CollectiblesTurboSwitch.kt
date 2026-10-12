package com.tangem.features.collectibles.impl.machinedetails.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme

/**
 * The Turbo-mode toggle.
 *
 * Its thumb is a wide pill rather than the circle [com.tangem.core.ui.components.TangemSwitch] draws, and its
 * colours are DS3 tokens, so the shared switch cannot be parameterised into this shape.
 */
@Deprecated("Use DS component when available")
@Composable
internal fun CollectiblesTurboSwitch(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val thumbOffset by animateDpAsState(
        targetValue = if (isChecked) 18.dp else 2.dp,
        label = "TurboThumbOffset",
    )

    Box(
        modifier = modifier
            .toggleable(
                value = isChecked,
                interactionSource = interactionSource,
                indication = ripple(bounded = false, color = Color.Transparent),
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .size(width = 48.dp, height = 20.dp)
            .background(color = TangemTheme.colors3.bg.opaque.secondary, shape = RoundedCornerShape(100.dp)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(width = 28.dp, height = 16.dp)
                .background(color = TangemTheme.colors3.bg.inverse, shape = RoundedCornerShape(100.dp)),
        )
    }
}