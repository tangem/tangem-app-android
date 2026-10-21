package com.tangem.features.jointaccount.main.confirmaddress.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.image.TangemIconUM

/**
 * @property address    the Safe address to compare with the other members; `null` until the account arrives
 */
@Immutable
internal data class JointAccountConfirmAddressUM(
    val address: String?,
    val buttonIcon: TangemIconUM?,
    val isConfirming: Boolean,
    val onShareClick: () -> Unit,
    val onMismatchClick: () -> Unit,
    val onConfirmClick: () -> Unit,
    val onBackClick: () -> Unit,
)