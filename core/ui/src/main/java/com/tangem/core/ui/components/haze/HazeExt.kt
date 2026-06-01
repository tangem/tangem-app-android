package com.tangem.core.ui.components.haze

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.LocalPowerSavingState
import com.tangem.core.ui.res.LocalRootBackgroundColor
import dev.chrisbanes.haze.*
import dev.chrisbanes.haze.materials.CupertinoMaterials
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun ProvideHaze(content: @Composable () -> Unit) {
    val hazeState = rememberHazeState()
    CompositionLocalProvider(
        LocalHazeState provides hazeState,
        LocalHazeStyle provides CupertinoMaterials.ultraThin(),
    ) {
        content()
    }
}

/**
 * Returns whether the haze blur effect would actually render for the given [state], taking both
 * the global [HazeState.blurEnabled] flag and the device's power-saving mode into account.
 *
 * Callers that pass a fully-transparent fallback to [hazeEffectTangem] should use this to decide
 * whether they need to render an opaque fallback layer themselves — otherwise the surface can
 * become invisible whenever blur is disabled (e.g. while power-saving mode is on).
 */
@Composable
fun isHazeBlurEffectivelyEnabled(state: HazeState = LocalHazeState.current): Boolean {
    val isPowerSavingEnabled by LocalPowerSavingState.current.isPowerSavingModeEnabled.collectAsState()
    return state.blurEnabled && !isPowerSavingEnabled
}

/**
 * Applies a haze effect to the [Modifier] with consideration of global haze settings and power saving mode.
 *
 * @param configure A lambda to configure the [HazeEffectScope].
 * @return A [Modifier] with the configured haze effect applied.
 */
@Composable
fun Modifier.hazeEffectTangem(
    state: HazeState = LocalHazeState.current,
    style: HazeStyle = CupertinoMaterials.ultraThin(),
    configure: HazeEffectScope.() -> Unit = {},
): Modifier {
    val isGlobalBlurEnabled = isHazeBlurEffectivelyEnabled(state)
    val rootBackground by LocalRootBackgroundColor.current

    return hazeEffect(state, style) {
        blurEnabled = isGlobalBlurEnabled
        fallbackTint = HazeTint(rootBackground.copy(alpha = 0.5f))
        configure()
        blurEnabled = blurEnabled && isGlobalBlurEnabled
    }
}

/**
 * Applies a haze foreground effect to the [Modifier]
 *
 * @param style The [HazeStyle] to apply. Defaults to [HazeStyle.Unspecified].
 * @param isBlurEnabled A Boolean indicating whether blur is enabled. Defaults to true.
 * @param reactToPowerSavingMode A Boolean indicating whether the haze effect should react to power saving mode.
 * Defaults to false.
 * @param configure A lambda to configure the [HazeEffectScope].
 * @return A [Modifier] with the configured haze foreground effect applied.
 */
@Composable
fun Modifier.hazeForegroundEffectTangem(
    style: HazeStyle = HazeStyle.Unspecified,
    reactToPowerSavingMode: Boolean = false,
    isBlurEnabled: Boolean = true,
    configure: HazeEffectScope.() -> Unit = {},
): Modifier {
    val powerSavingEnabled = LocalPowerSavingState.current.isPowerSavingModeEnabled.collectAsState()
    val isGlobalBlurEnabled = isBlurEnabled && (!reactToPowerSavingMode || !powerSavingEnabled.value)

    return hazeEffect(
        style = style,
    ) {
        blurEnabled = isGlobalBlurEnabled
        configure()
    }
}

/**
 * Applies a haze source to the [Modifier] using the current global haze state.
 */
@Composable
fun Modifier.hazeSourceTangem(state: HazeState = LocalHazeState.current, zIndex: Float = 0f, key: Any? = null) =
    this.hazeSource(state, zIndex, key)