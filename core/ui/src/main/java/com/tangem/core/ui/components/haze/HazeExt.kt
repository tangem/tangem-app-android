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
internal fun ProvideHaze(content: @Composable () -> Unit) {
    val hazeState = rememberHazeState()
    CompositionLocalProvider(
        LocalHazeState provides hazeState,
        LocalHazeStyle provides CupertinoMaterials.ultraThin(),
    ) {
        content()
    }
}

/**
 * Applies a haze effect to the [Modifier] with consideration of global haze settings and power saving mode.
 *
 * @param configure A lambda to configure the [HazeEffectScope].
 * @return A [Modifier] with the configured haze effect applied.
 */
@Composable
fun Modifier.hazeEffectTangem(
    style: HazeStyle = HazeStyle.Unspecified,
    configure: HazeEffectScope.() -> Unit = {},
): Modifier {
    val powerSavingEnabled = LocalPowerSavingState.current.isPowerSavingModeEnabled.collectAsState()
    val hazeState = LocalHazeState.current
    val isGlobalBlurEnabled = hazeState.blurEnabled && !powerSavingEnabled.value
    val rootBackground by LocalRootBackgroundColor.current

    return hazeEffect(hazeState, style) {
        fallbackTint = HazeTint(rootBackground)
        if (isGlobalBlurEnabled) {
            configure()
        }
        blurEnabled = isGlobalBlurEnabled
    }
}

/**
 * Applies a haze source to the [Modifier] using the current global haze state.
 */
@Composable
fun Modifier.hazeSourceTangem(zIndex: Float = 0f, key: Any? = null) =
    this.hazeSource(LocalHazeState.current, zIndex, key)