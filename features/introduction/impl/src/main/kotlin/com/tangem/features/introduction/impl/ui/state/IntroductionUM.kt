package com.tangem.features.introduction.impl.ui.state

import androidx.compose.ui.unit.IntSize
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import kotlinx.collections.immutable.ImmutableList

internal data class IntroductionUM(
    val isVideoReady: Boolean,
    /** Pixel size of the video frames, known before the first one so the surface never changes size under them. */
    val videoSize: IntSize,
    /** A new value asks for a new video surface: a stuck output only recovers on a fresh one ([REDACTED_TASK_KEY]). */
    val videoSurfaceKey: Int,
    val isVideoFailed: Boolean,
    val isVideoRunning: Boolean,
    val isMotionEnabled: Boolean,
    val scenes: ImmutableList<WelcomeSceneUM>,
    val bottomSheetConfig: TangemBottomSheetConfig?,
    val haveWalletBottomSheetConfig: TangemBottomSheetConfig?,
    val onCreateWalletClick: () -> Unit,
    val onIHaveWalletClick: () -> Unit,
    val onBuyTangemClick: () -> Unit,
    val onCreateWalletSheetCollapsed: () -> Unit,
    val onTermsOfServiceClick: () -> Unit,
)