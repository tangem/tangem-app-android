package com.tangem.features.introduction.impl.ui.state

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig

internal data class AddWalletUM(
    val sheetConfig: TangemBottomSheetConfig?,
    val socialLoginProgress: SocialLoginProgressUM?,
    val socialLoginConsent: SocialLoginConsentUM?,
    val onSheetCollapsed: () -> Unit,
)