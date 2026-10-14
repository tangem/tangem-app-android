package com.tangem.core.ui.components.atoms.radiobutton

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

/**
 * [Radio button](https://www.figma.com/design/14ISV23YB1yVW1uNVwqrKv/Android?node-id=101-119&t=FH84ljLBk1vmGAei-4)
 *
 * @param isSelected Whether the radio button is selected
 * @param onClick Called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param isEnabled Whether the button click is enabled
 */
@Composable
fun TangemRadioButton(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    Box(
        modifier = modifier.clickable(
            enabled = isEnabled,
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = false, radius = 16.dp),
            onClick = onClick,
        ),
    ) {
        val color = TangemTheme.colors.stroke.secondary
        val radius = with(LocalDensity.current) { 9.dp.toPx() }
        val width = with(LocalDensity.current) { 2.dp.toPx() }
        Canvas(
            modifier = Modifier
                .size(24.dp)
                .padding(2.dp),
        ) {
            drawCircle(
                color = color,
                radius = radius,
                style = Stroke(width),
            )
        }
        AnimatedVisibility(
            visible = isSelected,
            label = "Radio button animation",
            modifier = Modifier
                .size(24.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_check_circle_24),
                contentDescription = null,
                tint = TangemTheme.colors.control.checked,
            )
        }
    }
}

// region Preview
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemRadioButton_Preview() {
    var isSelected by remember { mutableStateOf(false) }
    TangemThemePreview {
        TangemRadioButton(isSelected, onClick = { isSelected = !isSelected })
    }
}
// endregion