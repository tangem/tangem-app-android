package com.tangem.core.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme

@Suppress("MagicNumber")
@Composable
fun TangemSwitch(
    onCheckedChange: (Boolean) -> Unit,
    checkedColor: Color = TangemTheme.colors.icon.accent,
    uncheckedColor: Color = TangemTheme.colors.icon.informative,
    size: Dp = 48.dp,
    checked: Boolean = false,
    enabled: Boolean = true,
) {
    val transition = updateTransition(checked, label = "SwitchState")
    val color by transition.animateColor(
        transitionSpec = {
            tween(durationMillis = 200, easing = FastOutLinearInEasing)
        },
        label = "",
    ) { isChecked ->
        (if (isChecked) checkedColor else uncheckedColor)
            .copy(alpha = if (enabled) 1f else .4f)
    }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
            ) {
                onCheckedChange(!checked)
            }
            .indication(
                interactionSource = interactionSource,
                indication = rememberRipple(
                    bounded = false,
                    color = Color.Transparent,
                ),
            ),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .width(size)
                .height(size / 2)
                .indication(interactionSource, null)
                .background(color = color, shape = RoundedCornerShape(100)),
            contentAlignment = Alignment.CenterStart,
        ) {
            val roundCardSize = this.maxWidth / 2
            val xOffset by transition.animateDp(
                transitionSpec = {
                    tween(durationMillis = 150, easing = LinearOutSlowInEasing)
                },
                label = "xOffset",
            ) { enabled ->
                if (enabled) this.maxWidth - roundCardSize else 0.dp
            }

            Box(
                modifier = Modifier
                    .size(this.maxWidth / 2)
                    .offset(x = xOffset, y = 0.dp)
                    .padding(3.dp)
                    .background(color = Color.White, shape = RoundedCornerShape(100)),
            )
        }
    }
}
