package com.tangem.features.introduction.impl.ui.state

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import kotlinx.collections.immutable.ImmutableList

internal data class IntroductionUM(
    val isVideoReady: Boolean,
    val isVideoFailed: Boolean,
    val isMotionEnabled: Boolean,
    val scenes: ImmutableList<WelcomeSceneUM>,
    val bottomSheetConfig: TangemBottomSheetConfig?,
    val haveWalletBottomSheetConfig: TangemBottomSheetConfig?,
    val onCreateWalletClick: () -> Unit,
    val onIHaveWalletClick: () -> Unit,
    val onTermsOfServiceClick: () -> Unit,
    val onPrivacyPolicyClick: () -> Unit,
)