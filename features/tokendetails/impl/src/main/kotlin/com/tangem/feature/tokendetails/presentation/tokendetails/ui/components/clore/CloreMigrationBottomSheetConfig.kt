package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.clore

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent

@Immutable
data class CloreMigrationBottomSheetConfig(
    val message: String = "",
    val signature: String = "",
    val isSigningInProgress: Boolean = false,
    val onMessageChange: (String) -> Unit,
    val onSignClick: () -> Unit,
    val onCopyClick: () -> Unit,
    val onOpenPortalClick: () -> Unit,
) : TangemBottomSheetConfigContent