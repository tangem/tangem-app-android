package com.tangem.domain.jointaccount.repository

import arrow.core.Either
import com.tangem.domain.jointaccount.model.JointAccountActivationPayload
import com.tangem.domain.jointaccount.model.JointAccountCreationPayload
import com.tangem.domain.jointaccount.model.JointAccountCreationResult
import com.tangem.domain.jointaccount.model.JointAccountInvitePreview
import com.tangem.domain.jointaccount.model.JointAccountInvitePreviewError
import com.tangem.domain.jointaccount.model.JointAccountJoinPayload
import com.tangem.domain.jointaccount.model.JointAccountJoinResult
import com.tangem.domain.jointaccount.model.JointOperationCreateError
import com.tangem.domain.jointaccount.model.JointOperationRejectError
import com.tangem.domain.jointaccount.model.JointOperationSignError
import com.tangem.domain.jointaccount.model.SafeTx
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

    /**
     * Redeems the invite: takes the slot and creates this wallet's own row in `accounts`.
     *
     * [payload] is the object the card signed and [signature] the EIP-191 signature over its RFC 8785 canonical
     * form — the same wire rules as in [create]. A 409 comes back as [JointAccountJoinResult.Conflict] — the
     * response does not tell a spent invite from an already-registered address, the domain resolves it by
     * re-reading the list. Everything else is thrown as it arrives from the API.
     */
    @Throws
    suspend fun join(
        userWalletId: UserWalletId,
        payload: JointAccountJoinPayload,
        signature: String,
    ): JointAccountJoinResult

    /**
     * Confirms the composition and activates the account.
     *
     * [payload] is the object the card signed and [signature] the EIP-191 signature over its RFC 8785 canonical
     * form — the same wire rules as in [create]. The endpoint is idempotent — activating an already `active`
     * account succeeds — so a request whose response was lost to a timeout is retried once here instead of
     * reporting an error for a likely-activated account. Every other error comes back as a `Left` as it arrives
     * from the API — the function never throws.
     */
    suspend fun activate(
        userWalletId: UserWalletId,
        payload: JointAccountActivationPayload,
        signature: String,
    ): Either<Throwable, Unit>

    /**
     * Previews the invite [inviteId] for wallet [walletId] — the configuration and who is inviting.
     *
     * [JointAccountInvitePreviewError.InvalidLink] covers a link the backend refuses or does not know — already
     * redeemed, this wallet already holding a slot, or no such invite. None of the three is retryable and all
     * read the same to the invitee. Anything else is [JointAccountInvitePreviewError.LoadFailed].
     */
    suspend fun getInvitePreview(
        walletId: UserWalletId,
        inviteId: String,
    ): Either<JointAccountInvitePreviewError, JointAccountInvitePreview>

    /**
     * The unused invites of the joint account [cryptoAccountId] that live on this device.
     *

     * list also answers "this device is not the creator's". Never throws: a store that cannot be read is an
     * empty list, so a screen built on this keeps rendering everything else.
     */
    suspend fun getInvites(userWalletId: UserWalletId, cryptoAccountId: String): List<String>

    /**
     * Registers a [safeTx] for collective signing, [signature] counting as the initiator's first one.
     */
    suspend fun createOperation(
        userWalletId: UserWalletId,
        safeAddress: String,
        chainId: Long,
        safeTx: SafeTx,
        signature: String,
    ): Either<JointOperationCreateError, Unit>

    /**
     * A 404 (the operation finished or was removed while the sheet was open) comes back as
     * [JointOperationSignError.OperationNotFound], a 409 (the threshold is already reached) as
     * [JointOperationSignError.ThresholdReached]. The function never throws.
     */
    suspend fun signOperation(
        userWalletId: UserWalletId,
        operationId: String,
        safeTx: SafeTx,
        signature: String,
    ): Either<JointOperationSignError, Unit>

    /** Cancels the operation when the caller is its initiator, records a decline otherwise */
    suspend fun rejectOperation(
        userWalletId: UserWalletId,
        operationId: String,
    ): Either<JointOperationRejectError, Unit>
}