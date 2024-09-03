package com.tangem.common.ui.userwallet.state

import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.wallets.models.UserWalletId
import javax.annotation.concurrent.Immutable

@Immutable
data class UserWalletItemUM(
    val id: UserWalletId,
    val name: TextReference,
    val information: TextReference,
    val imageUrl: String,
    val isEnabled: Boolean,
    val endIcon: EndIcon = EndIcon.None,
    val onClick: () -> Unit,
) {
    enum class EndIcon {
        None,
        Arrow,
        Checkmark,
    }
}