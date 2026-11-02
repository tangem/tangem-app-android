package com.tangem.core.ui.ds2.toggle

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentDataType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDataType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.DsVersion
import com.tangem.core.ui.extensions.conditionalCompose
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.SwitchTestTags

/**
 * Design-system v2 on/off switch: a pill track with a knob that slides to the end and tints the track
 * when checked.
 *
 * Version: 1.0.0
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=11446-1700)
 *
 * Behavior notes:
 * - Toggling animates the track color (200 ms, ease-in-out) and slides the knob (200 ms, ease-out-back,
 *   so it slightly overshoots before settling).
 * - Fixed 48 × 24 dp; the knob follows the layout direction (slides to the left in RTL).
 * - Pass a [contentDescription] when there is no adjacent label merged into the same semantics node;
 *   TalkBack then announces it together with the on/off state.
 *
 * @param checked Whether the switch is on.
 * @param onCheckedChange Invoked with the toggled value. `null` makes the switch non-interactive.
 * @param modifier Applied to the root; the switch sizes itself, so pass only positioning/padding.
 * @param isEnabled When `false`, the switch is dimmed and clicks are ignored.
 * @param contentDescription Accessibility label announced by TalkBack.
 * @param interactionSource Interaction source for press/focus state.
 */
@Suppress("MagicNumber")
@Composable
fun TangemSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    contentDescription: String? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) TangemTheme.colors3.bg.status.info else TangemTheme.colors3.bg.special.switch,
        animationSpec = tween(durationMillis = 200, easing = EaseInOut),
        label = "trackColor",
    )
    val knobOffset by animateDpAsState(
        targetValue = if (checked) KnobTravel else 0.dp,
        animationSpec = tween(durationMillis = 200, easing = EaseOutBack),
        label = "knobOffset",
    )

    Box(
        modifier = modifier
            .graphicsLayer { alpha = if (isEnabled) 1f else 0.4f }
            .semantics(mergeDescendants = true) {
                if (!isEnabled) disabled()
                if (onCheckedChange == null) {
                    role = Role.Switch
                    toggleableState = ToggleableState(checked)
                    contentDataType = ContentDataType.Toggle
                }
                contentDescription?.let { this.contentDescription = it }
            }
            .conditionalCompose(onCheckedChange != null) {
                toggleable(
                    value = checked,
                    onValueChange = requireNotNull(onCheckedChange),
                    enabled = isEnabled,
                    role = Role.Switch,
                    interactionSource = interactionSource,
                    indication = null,
                )
            }
            .testTag(SwitchTestTags.SWITCH)
            .size(width = 48.dp, height = 24.dp)
            .padding(vertical = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(trackColor)
                .padding(2.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .offset(x = knobOffset)
                    .size(width = KnobWidth, height = 16.dp)
                    .clip(CircleShape)
                    .background(TangemTheme.colors3.icon.staticDark),
            )
        }
    }
}

private val KnobWidth = 28.dp
private val KnobTravel = 48.dp - 2.dp * 2 - KnobWidth

/** Public API surface of [TangemSwitch]. */
object TangemSwitch {

    val version = DsVersion(major = 1, minor = 0, patch = 0)
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TangemSwitchPreview() {
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
            var isChecked by remember { mutableStateOf(false) }
            TangemSwitch(checked = isChecked, onCheckedChange = { isChecked = it }, isEnabled = isEnabled)
            TangemSwitch(checked = false, onCheckedChange = {}, isEnabled = isEnabled)
            TangemSwitch(checked = true, onCheckedChange = {}, isEnabled = isEnabled)
        }
    }
}