package com.tangem.features.jointaccount.compromised

import com.tangem.domain.models.wallet.UserWalletId

/** What every entry to a joint account whose composition does not verify shows instead of opening it */
interface JointAccountCompromisedAlert {

    fun show(userWalletId: UserWalletId)

    /** Shows the alert when [accountId] is a joint account whose composition does not verify, and says whether it did */
    suspend fun showIfCompromised(userWalletId: UserWalletId, accountId: String): Boolean
}