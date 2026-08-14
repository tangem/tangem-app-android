package com.tangem.features.jointaccount.common.displayname.ui.state

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

/**
 * @property name raw input exactly as typed — never normalised, the value ends up in the card-signed payload
 * @property isError the input contains a forbidden character; the field is highlighted and the button disabled
 * @property buttonIconRes wallet-interaction icon on the primary button; `null` for wallets without one (hot)
 * @property isButtonLoading the signature session is in progress; the button shows a loader instead of its content
 */
@Immutable
internal data class JointAccountDisplayNameUM(
    val name: String,
    val isError: Boolean,
    val buttonText: TextReference,
    @DrawableRes val buttonIconRes: Int?,
    val isButtonEnabled: Boolean,
    val isButtonLoading: Boolean,
    val onNameChange: (String) -> Unit,
    val onContinueClick: () -> Unit,
    val onBackClick: () -> Unit,
)