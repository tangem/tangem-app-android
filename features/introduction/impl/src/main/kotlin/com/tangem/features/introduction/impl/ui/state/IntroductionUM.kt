package com.tangem.features.introduction.impl.ui.state

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import kotlinx.collections.immutable.ImmutableList

internal data class IntroductionUM(
    val isVideoReady: Boolean,
    /** `null` until the decoder reports the geometry of the frames it is about to render. */
    val videoAspectRatio: Float?,
    val isVideoFailed: Boolean,
    val isVideoRunning: Boolean,
    val isMotionEnabled: Boolean,
    val scenes: ImmutableList<WelcomeSceneUM>,
    val bottomSheetConfig: TangemBottomSheetConfig?,
    val haveWalletBottomSheetConfig: TangemBottomSheetConfig?,
    val socialLoginProgress: SocialLoginProgressUM?,
    val onCreateWalletClick: () -> Unit,
    val onIHaveWalletClick: () -> Unit,
    val onCreateWalletSheetCollapsed: () -> Unit,
    val onTermsOfServiceClick: () -> Unit,
    val onPrivacyPolicyClick: () -> Unit,
) {

    val isSocialLoginShown: Boolean
        get() = socialLoginProgress != null
}