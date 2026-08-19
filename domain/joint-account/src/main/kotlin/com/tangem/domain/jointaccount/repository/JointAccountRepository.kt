package com.tangem.domain.jointaccount.repository

import com.tangem.domain.jointaccount.model.JointAccountCreationPayload
import com.tangem.domain.jointaccount.model.JointAccountCreationResult
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Write operations on joint accounts. Reading the accounts of a wallet is the supplier's job — see
 * `SingleJointAccountListSupplier`.
 */
interface JointAccountRepository {

    /**

     * `wallet.totalJointAccounts`, counted by the backend across active AND archived joint accounts.
     *
     * Always read from a fresh `/accounts` request, never from cache: a stale counter yields an index that is
     * already occupied, and that surfaces only as a 409 on creation — after the card tap is already spent.
     * Returns 0 when the backend does not report the counter yet.
     */
    @Throws
    suspend fun getFreeOwnerDerivationIndex(userWalletId: UserWalletId): Int

    /**
     * Registers a joint account and persists the invite ids from the response.
     *
     * [payload] is the object the card signed and [signature] the EIP-191 signature over its RFC 8785
     * canonical form. The request carries the payload as a regular JSON object: per the contract the backend
     * canonicalizes the payload "exactly as it arrives" before recovering the signer, so key order and
     * escaping on the wire do not matter — but the values must reach it untouched, never trimmed or
     * normalized.
     *
     * The backend issues the invites exactly once, in this response, and has no endpoint to read them back,
     * so they are stored before the account is returned.
     *
     * A conflict on the creator's owner address comes back as
     * [JointAccountCreationResult.CreatorAlreadyRegistered] — see its docs for why it is a result and not an
     * error. Everything else is thrown as it arrives from the API.
     */
    @Throws
    suspend fun create(
        userWalletId: UserWalletId,
        payload: JointAccountCreationPayload,
        signature: String,
    ): JointAccountCreationResult
}