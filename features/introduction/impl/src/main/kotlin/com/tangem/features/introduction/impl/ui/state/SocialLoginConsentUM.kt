package com.tangem.features.introduction.impl.ui.state

import com.tangem.core.ui.extensions.TextReference

internal data class SocialLoginConsentUM(
    val title: TextReference,
    val description: TextReference,
    val acknowledgement: TextReference,
    val isAcknowledged: Boolean,
    val isAcknowledgementMissing: Boolean,
    val isEmailSharingAllowed: Boolean,
    val onAcknowledgedChange: (Boolean) -> Unit,
    val onEmailSharingChange: (Boolean) -> Unit,
    val onConfirmClick: () -> Unit,
    val onCloseClick: () -> Unit,
)