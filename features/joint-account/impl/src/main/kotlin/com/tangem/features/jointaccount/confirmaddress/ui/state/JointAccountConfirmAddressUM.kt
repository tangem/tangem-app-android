package com.tangem.features.jointaccount.confirmaddress.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference

/**
 * @property address    the Safe address to compare with the other members; `null` until the account arrives
 */
@Immutable
internal data class JointAccountConfirmAddressUM(
    val address: String?,
    val shareMessage: TextReference?,
    val buttonIcon: TangemIconUM?,
    val isConfirming: Boolean,
    val onShareClick: (message: String) -> Unit,
    val onMismatchClick: () -> Unit,
    val onConfirmClick: () -> Unit,
    val onBackClick: () -> Unit,
)