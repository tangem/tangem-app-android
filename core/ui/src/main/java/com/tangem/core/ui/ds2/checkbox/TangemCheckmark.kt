package com.tangem.core.ui.ds2.checkbox

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.tangem.core.ui.extensions.conditionalCompose
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_control_checkmark_24
import com.tangem.core.ui.res.generated.icons.ic_control_circle_24
import com.tangem.core.ui.res.generated.icons.ic_control_circle_24_filled

/**
 * Design-system v2 circular checkmark: an `unchecked` outline circle or a `checked` filled circle
 * with a checkmark. Unlike [TangemCheckbox], this control is round (border-radius/full) and
 * boolean-only — it has no indeterminate state.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=3671-5693)
 *
 * @param checked Whether the checkmark is checked.
 * @param onCheckedChange Invoked with the toggled value. `null` makes the control non-interactive.
 * @param isEnabled When `false`, the control is dimmed and clicks are ignored.
 * @param contentDescription Accessibility label announced by TalkBack.
 * @param interactionSource Interaction source for press/focus state.
 */
@Suppress("MagicNumber", "LongMethod")
@Composable
fun TangemCheckmark(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    contentDescription: String? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val contentAlpha = if (isEnabled) 1f else 0.4f

    // Whole control shrinks slightly while pressed and springs back on release.
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "pressScale",
    )
    // Press fill fades in/out instead of toggling instantly.
    val pressColor by animateColorAsState(
        targetValue = if (isPressed) TangemTheme.colors3.interaction.press.default else Color.Transparent,
        animationSpec = tween(durationMillis = 100),
        label = "pressColor",
    )
    // Drives the filled circle growing over the outline (0 = unchecked, 1 = filled).
    val fillProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "fillProgress",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = contentAlpha
                scaleX = pressScale
                scaleY = pressScale
            }
            .semantics(mergeDescendants = true) {
                if (!isEnabled) disabled()
                contentDescription?.let { this.contentDescription = it }
            }
            .conditionalCompose(onCheckedChange != null) {
                toggleable(
                    value = checked,
                    onValueChange = requireNotNull(onCheckedChange),
                    enabled = isEnabled,
                    role = Role.Checkbox,
                    interactionSource = interactionSource,
                    indication = null,
                )
            }
            .size(24.dp)
            .clip(CircleShape)
            .drawBehind { drawRect(pressColor) }
            .conditionalCompose(isFocused) {
                border(
                    width = 2.dp,
                    color = TangemTheme.colors3.interaction.focusRing.brand,
                    shape = CircleShape,
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        // Outline circle — always present; the filled circle grows over it.
        Icon(
            imageVector = Icons.ic_control_circle_24,
            contentDescription = null,
            tint = TangemTheme.colors3.icon.primary,
        )
        // Filled circle — fades and scales in from the center as the checkmark becomes filled.
        Icon(
            imageVector = Icons.ic_control_circle_24_filled,
            contentDescription = null,
            tint = TangemTheme.colors3.icon.primary,
            modifier = Modifier.graphicsLayer {
                alpha = fillProgress
                val markScale = lerp(start = 0.5f, stop = 1f, fraction = fillProgress)
                scaleX = markScale
                scaleY = markScale
            },
        )
        // Checkmark pops in and fades out as the checked state toggles.
        AnimatedContent(
            targetState = checked,
            transitionSpec = {
                val enter = scaleIn(
                    initialScale = 0.5f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ) + fadeIn()
                val exit = scaleOut(targetScale = 0.5f) + fadeOut()
                enter togetherWith exit
            },
            label = "mark",
        ) { isChecked ->
            if (isChecked) {
                Icon(
                    imageVector = Icons.ic_control_checkmark_24,
                    contentDescription = null,
                    tint = TangemTheme.colors3.icon.inverse,
                )
            } else {
                Box(Modifier.size(24.dp))
            }
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TangemCheckmarkPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            PreviewRow(label = "Enabled", isEnabled = true)
            PreviewRow(label = "Disabled", isEnabled = false)
        }
    }
}

@Composable
private fun PreviewRow(label: String, isEnabled: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.body.medium,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            listOf(false, true).forEach { checked ->
                TangemCheckmark(checked = checked, onCheckedChange = {}, isEnabled = isEnabled)
            }
        }
    }
}