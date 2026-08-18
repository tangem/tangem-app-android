package com.tangem.features.jointaccount.join.model

import com.tangem.domain.models.wallet.UserWalletId

/**
 * Accumulated result of the join flow steps.
 *
 * @property selectedWalletId wallet the invitee joins with, chosen on the invite preview step
 * @property displayName name the other members will see, entered on the display name step
 */
internal data class JointAccountJoinDraft(
    val selectedWalletId: UserWalletId? = null,
    val displayName: String? = null,
)