package com.tangem.features.jointaccount.main.model

import com.tangem.domain.models.wallet.UserWalletId

/** [onCloseFlow] leaves the whole members flow, not just the step on top of its stack */
internal data class JointAccountMembersListParams(
    val userWalletId: UserWalletId,
    val cryptoAccountId: String?,
    val onCloseFlow: () -> Unit,
)