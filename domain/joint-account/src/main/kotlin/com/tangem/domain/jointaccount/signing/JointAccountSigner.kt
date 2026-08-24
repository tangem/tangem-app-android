package com.tangem.domain.jointaccount.signing

import arrow.core.Either
import com.tangem.domain.jointaccount.model.JointAccountSignInput
import com.tangem.domain.jointaccount.model.JointAccountSignResult
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Signs a joint account payload with the card of [UserWalletId] in a single NFC session.
 *
 * The implementation owns everything session-related: opening the session, filtering it by wallet rather than
 * by card id (any backup card of that wallet signs equally), and persisting the derived owner key — the
 * session's in-memory copy dies with the session.
 */
interface JointAccountSigner {

    suspend fun sign(
        userWalletId: UserWalletId,
        input: JointAccountSignInput,
    ): Either<Throwable, JointAccountSignResult>
}