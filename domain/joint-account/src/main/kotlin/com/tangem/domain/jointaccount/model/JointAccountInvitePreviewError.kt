package com.tangem.domain.jointaccount.model

/**
 * Why previewing an invite did not produce a [JointAccountInvitePreview].
 */
sealed interface JointAccountInvitePreviewError {

    /**
     * The invite cannot be joined: it does not exist, is already redeemed, or this wallet already holds a slot.
     * The invitee is asked to check the link or request a new one — there is nothing to retry.
     */
    data object InvalidLink : JointAccountInvitePreviewError

    /**
     * The request never reached the backend: no connection, or no answer in time. A link received over Quick Share
     * or AirDrop is opened while the phone is still reconnecting from the transfer, so this is worth retrying soon.
     */
    data class Unreachable(val cause: Throwable) : JointAccountInvitePreviewError

    /** Anything else: a server error, an unexpected response. Safe to retry. */
    data class LoadFailed(val cause: Throwable) : JointAccountInvitePreviewError
}