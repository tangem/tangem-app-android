package com.tangem.feature.swap.ui

import android.content.res.Configuration
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.buttons.SecondarySmallButton
import com.tangem.core.ui.components.buttons.SmallButtonConfig
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

private const val HALF_PERIOD_MS = 1800 // half period; full cycle (Reverse) = 3.6s
private const val START_DELAY_MS = 500

// Peak opacity of the emphasis overlay at the top of the breath. Kept low so the pill only tints
// slightly rather than fully changing color — a gentle "breathing", not a flash.
private const val PULSE_PEAK_ALPHA = 0.3f

/**
 * The "Choose token" pill on the swap screen. While no token is selected, its background gently
 * "breathes" to draw attention to the pending token choice.
 *
 * The pulse runs only when [isPulseEnabled] AND the device has not disabled animations
 * (`ANIMATOR_DURATION_SCALE != 0`); otherwise a plain [SecondarySmallButton] is rendered. At rest
 * the pulsing pill is pixel-identical to that fallback, so toggling the animation never shifts the
 * layout or color.
 */
@Composable
internal fun ChooseTokenPulseButton(isPulseEnabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val animatorScale = remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
    }
    val shouldPulse = isPulseEnabled && animatorScale != 0f

    if (shouldPulse) {
        PulsingChooseTokenPill(onClick = onClick, modifier = modifier)
    } else {
        SecondarySmallButton(
            config = SmallButtonConfig(
                text = resourceReference(R.string.common_choose_token),
                icon = TangemButtonIconPosition.End(R.drawable.ic_chevron_24),
                onClick = onClick,
            ),
            modifier = modifier,
        )
    }
}

/**
 * The pulsing variant of the pill: a solid `button.secondary` base with
 * a neutral emphasis overlay whose alpha is animated `0 → PULSE_PEAK_ALPHA → 0`. The overlay color
 * is `text.tertiary` — a mid-gray that is the same value in light and dark, so the pill darkens
 * slightly in light and lightens slightly in dark, i.e. it grows more prominent against its
 * background in both themes. The overlay is drawn on top of the base but under the content
 * (text + chevron), so the label and chevron stay crisp.
 */
@Suppress("MagicNumber")
@Composable
private fun PulsingChooseTokenPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(size = 16.dp)
    val transition = rememberInfiniteTransition(label = "choose_token_pulse")
    val emphasisAlpha by transition.animateFloat(
        initialValue = 0f,
        targetValue = PULSE_PEAK_ALPHA,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = HALF_PERIOD_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(START_DELAY_MS),
        ),
        label = "choose_token_pulse_alpha",
    )
    val emphasisColor = TangemTheme.colors.text.tertiary

    Box(
        modifier = modifier
            .defaultMinSize(
                minWidth = 46.dp,
                minHeight = 24.dp,
            )
            .clip(shape)
            .background(color = TangemTheme.colors.button.secondary, shape = shape)
            // Emphasis overlay is read in the draw phase, so the breath repaints without recomposing.
            .drawBehind { drawRect(color = emphasisColor, alpha = emphasisAlpha) }
            .clickable(onClick = onClick)
            .padding(
                start = 12.dp,
                end = 8.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                modifier = Modifier.padding(vertical = 4.dp),
                text = resourceReference(R.string.common_choose_token).resolveReference(),
                color = TangemTheme.colors.text.primary1,
                maxLines = 1,
                style = TangemTheme.typography.button,
            )
            Spacer(modifier = Modifier.requiredWidth(4.dp))
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(id = R.drawable.ic_chevron_24),
                tint = TangemTheme.colors.icon.secondary,
                contentDescription = null,
            )
        }
    }
}

// region Preview
@Suppress("MagicNumber")
@Preview(showBackground = true, widthDp = 200)
@Preview(showBackground = true, widthDp = 200, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ChooseTokenPulseButton_Preview() {
    TangemThemePreview {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors.background.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Pulsing (animation is static in a preview — shows the pill at a single frame)
            ChooseTokenPulseButton(isPulseEnabled = true, onClick = {})
            // Non-pulsing fallback
            ChooseTokenPulseButton(isPulseEnabled = false, onClick = {})
        }
    }
}
// endregion