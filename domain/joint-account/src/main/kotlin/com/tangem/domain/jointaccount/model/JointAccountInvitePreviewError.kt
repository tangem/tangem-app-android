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

    /** Anything else: no network, a server error, an unexpected response. Safe to retry. */
    data class LoadFailed(val cause: Throwable) : JointAccountInvitePreviewError
}