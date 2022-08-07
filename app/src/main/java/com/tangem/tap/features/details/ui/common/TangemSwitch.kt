package com.tangem.tap.features.details.ui.common

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.Card
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

@Composable
fun TangemSwitch(
    modifier: Modifier = Modifier,
    enabledColor: Color = colorResource(id = R.color.control_checked),
    disabledColor: Color = colorResource(id = R.color.icon_informative),
    size: Dp = 48.dp,
    checked: Boolean = false,
    onCheckedChange: (Boolean) -> Unit,
) {
    val transition = updateTransition(checked, label = "SwitchState")
    val color by transition.animateColor(
        transitionSpec = {
            tween(200, easing = FastOutLinearInEasing)
        },
        label = "",
    ) {
        when (it) {
            true -> enabledColor
            false -> disabledColor
        }
    }
    val interactionSource = remember { MutableInteractionSource() }
    val clickable = Modifier.clickable(
        interactionSource = interactionSource,
        indication = null,
    ) {
        if (!checked) {
            onCheckedChange(true)
        } else {
            onCheckedChange(false)
        }
    }

    Box(
        modifier = Modifier
            .then(clickable)
            .indication(
                interactionSource = MutableInteractionSource(),
                indication = rememberRipple(
                    bounded = true,
                    radius = 100.dp,
                    color = Color.Transparent,
                ),
            ),
    ) {
        BoxWithConstraints(
            modifier = modifier
                .width(size)
                .height(size / 2)
                .indication(MutableInteractionSource(), null)
                .background(color = color, shape = RoundedCornerShape(100)),
            contentAlignment = Alignment.CenterStart,
        ) {
            val roundCardSize = this.maxWidth / 2
            val xOffset by transition.animateDp(
                transitionSpec = {
                    tween(150, easing = LinearOutSlowInEasing)
                },
                label = "xOffset",
            ) { state ->
                when (state) {
                    false -> 0.dp
                    true -> this.maxWidth - roundCardSize
                }
            }

            Card(
                modifier = Modifier
                    .size(this.maxWidth / 2)
                    .offset(x = xOffset, y = 0.dp)
                    .padding(3.dp),
                shape = RoundedCornerShape(100),
                backgroundColor = Color.White,
                border = BorderStroke(
                    if (!checked) 0.5.dp else 0.dp,
                    color = Color.LightGray,
                ),
            ) {
            }
        }
    }
}