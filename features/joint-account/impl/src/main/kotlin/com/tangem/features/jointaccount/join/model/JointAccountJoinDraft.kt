package com.tangem.features.jointaccount.join.model

import com.tangem.domain.jointaccount.model.JointAccountConfig
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Accumulated result of the join flow steps.
 *
 * @property selectedWalletId wallet the invitee joins with, chosen on the invite preview step
 * @property config the account configuration exactly as the preview returned it — the join payload must
 * carry it verbatim, the backend rejects a mismatch
 * @property displayName name the other members will see, entered on the display name step
 */
internal data class JointAccountJoinDraft(
    val selectedWalletId: UserWalletId? = null,
    val config: JointAccountConfig? = null,
    val displayName: String? = null,
)