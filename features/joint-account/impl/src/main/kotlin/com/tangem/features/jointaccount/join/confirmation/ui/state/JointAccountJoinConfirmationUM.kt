package com.tangem.features.jointaccount.join.confirmation.ui.state

import androidx.compose.runtime.Immutable

/**
 * @property onContinueClick proceed to the display name step
 * @property onCancelClick return to the invite preview; dismissing the sheet acts the same
 */
@Immutable
internal data class JointAccountJoinConfirmationUM(
    val onContinueClick: () -> Unit,
    val onCancelClick: () -> Unit,
)