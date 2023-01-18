package com.tangem.tap.features.details.ui.common

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.wallet.R

@Suppress("MagicNumber")
@Composable
fun TangemSwitch(
    onCheckedChange: (Boolean) -> Unit,
    checkedColor: Color = colorResource(id = R.color.control_checked),
    uncheckedColor: Color = colorResource(id = R.color.icon_informative),
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
        if (isChecked) checkedColor else uncheckedColor
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
