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
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.tangem.core.ui.extensions.conditionalCompose
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_control_box_24
import com.tangem.core.ui.res.generated.icons.ic_control_box_24_filled
import com.tangem.core.ui.res.generated.icons.ic_control_checkmark_24
import com.tangem.core.ui.res.generated.icons.ic_control_indeterminate_24

/**
 * Design-system v2 tri-state checkbox: an `unchecked` outline box, a `checked` filled box with a
 * checkmark, or an `indeterminate` filled box with a dash.
 *
 * [Figma](https://www.figma.com/design/y8arHOHCa6HjMpOMJ0Ykj6/DS-64-%7C-Token-Icon?node-id=3650-628)
 *
 * @param state Current tri-state value. See [ToggleableState].
 * @param onClick Invoked on toggle. `null` makes the checkbox non-interactive.
 * @param isEnabled When `false`, the checkbox is dimmed and clicks are ignored.
 * @param contentDescription Accessibility label announced by TalkBack.
 * @param interactionSource Interaction source for press/focus state.
 */
@Suppress("MagicNumber", "LongMethod")
@Composable
fun TangemCheckbox(
    state: ToggleableState,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    contentDescription: String? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val contentAlpha = if (isEnabled) 1f else 0.4f // opacity/disabled

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
    // Drives the filled box growing over the outline (0 = unchecked, 1 = filled).
    val fillProgress by animateFloatAsState(
        targetValue = if (state == ToggleableState.Off) 0f else 1f,
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
            .conditionalCompose(onClick != null) {
                triStateToggleable(
                    state = state,
                    onClick = requireNotNull(onClick),
                    enabled = isEnabled,
                    role = Role.Checkbox,
                    interactionSource = interactionSource,
                    indication = null,
                )
            }
            .size(24.dp)
            .clip(CheckboxShape)
            .drawBehind { drawRect(pressColor) }
            .conditionalCompose(isFocused) {
                border(
                    width = 2.dp, // border-width/md
                    color = TangemTheme.colors3.interaction.focusRing.brand,
                    shape = CheckboxShape,
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        // Outline box — always present; the filled box grows over it.
        Icon(
            imageVector = Icons.ic_control_box_24,
            contentDescription = null,
            tint = TangemTheme.colors3.icon.primary,
        )
        // Filled box — fades and scales in from the center as the checkbox becomes filled.
        Icon(
            imageVector = Icons.ic_control_box_24_filled,
            contentDescription = null,
            tint = TangemTheme.colors3.icon.primary,
            modifier = Modifier.graphicsLayer {
                alpha = fillProgress
                val markScale = lerp(start = 0.5f, stop = 1f, fraction = fillProgress)
                scaleX = markScale
                scaleY = markScale
            },
        )
        // Mark — checkmark or dash pops in, and crossfades when switching between the two.
        AnimatedContent(
            targetState = state,
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
        ) { current ->
            when (current) {
                ToggleableState.On -> Icon(
                    imageVector = Icons.ic_control_checkmark_24,
                    contentDescription = null,
                    tint = TangemTheme.colors3.icon.inverse,
                )
                ToggleableState.Indeterminate -> Icon(
                    imageVector = Icons.ic_control_indeterminate_24,
                    contentDescription = null,
                    tint = TangemTheme.colors3.icon.inverse,
                )
                ToggleableState.Off -> Box(Modifier.size(24.dp))
            }
        }
    }
}

/**
 * Boolean (checked / unchecked) overload of [TangemCheckbox] for the common two-state case.
 *
 * @param checked Whether the checkbox is checked.
 * @param onCheckedChange Invoked with the toggled value. `null` makes the checkbox non-interactive.
 */
@Composable
fun TangemCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    contentDescription: String? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    TangemCheckbox(
        state = ToggleableState(checked),
        onClick = onCheckedChange?.let { { it(!checked) } },
        modifier = modifier,
        isEnabled = isEnabled,
        contentDescription = contentDescription,
        interactionSource = interactionSource,
    )
}

private val CheckboxShape = RoundedCornerShape(6.dp)

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TangemCheckboxPreview() {
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
            ToggleableState.entries.forEach { state ->
                TangemCheckbox(state = state, onClick = {}, isEnabled = isEnabled)
            }
        }
    }
}