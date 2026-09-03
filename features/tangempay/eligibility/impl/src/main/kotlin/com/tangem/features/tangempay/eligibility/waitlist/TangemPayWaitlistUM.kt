package com.tangem.features.tangempay.eligibility.waitlist

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent

@Immutable
internal data class TangemPayWaitlistUM(
    val email: String,
    val isMarketingConsentGiven: Boolean,
    val isSaveEnabled: Boolean,
    val onEmailChange: (String) -> Unit,
    val onEmailClearClick: () -> Unit,
    val onMarketingConsentClick: () -> Unit,
    val onPrivacyPolicyClick: () -> Unit,
    val onSaveClick: () -> Unit,
    val onDismiss: () -> Unit,
) : TangemBottomSheetConfigContent