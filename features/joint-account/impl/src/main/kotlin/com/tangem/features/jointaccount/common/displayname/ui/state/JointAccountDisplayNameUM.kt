package com.tangem.features.jointaccount.common.displayname.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference

/**
 * @property name raw input exactly as typed; only the edge whitespace is dropped when the name is handed on to the
 *   card-signed payload
 * @property buttonIcon wallet-interaction icon on the primary button; `null` for wallets without one (hot)
 * @property isButtonLoading the signature session is in progress; the button shows a loader instead of its content
 */
@Immutable
internal data class JointAccountDisplayNameUM(
    val name: String,
    val buttonText: TextReference,
    val buttonIcon: TangemIconUM?,
    val isButtonEnabled: Boolean,
    val isButtonLoading: Boolean,
    val onNameChange: (String) -> Unit,
    val onContinueClick: () -> Unit,
    val onBackClick: () -> Unit,
)