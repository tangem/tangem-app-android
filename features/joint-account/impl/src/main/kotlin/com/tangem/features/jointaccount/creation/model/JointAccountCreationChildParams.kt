package com.tangem.features.jointaccount.creation.model

import com.tangem.domain.models.wallet.UserWalletId

/** What the flow container hands to every step: the flow input plus the shared draft */
internal data class JointAccountCreationChildParams(
    val userWalletId: UserWalletId,
    val draftHolder: JointAccountCreationDraftHolder,
)