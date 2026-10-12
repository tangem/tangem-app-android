package com.tangem.features.jointaccount.join.invitepreview.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.features.jointaccount.join.confirmation.ui.state.JointAccountJoinConfirmationUM

@Immutable
internal sealed interface JointAccountInvitePreviewUM {

    data class Loading(val onCloseClick: () -> Unit) : JointAccountInvitePreviewUM

    data class Content(
        val accountName: String,
        val accountIcon: AccountIconUM.CryptoPortfolio,
        val requiredToSign: Int,
        val totalMembers: Int,
        val creatorName: String,
        val creatorAddress: String,
        val wallet: WalletUM?,
        val confirmation: JointAccountJoinConfirmationUM?,
        val onCreatorInfoClick: () -> Unit,
        val onContinueClick: () -> Unit,
        val onCloseClick: () -> Unit,
    ) : JointAccountInvitePreviewUM {

        data class WalletUM(
            val name: String,
            val onClick: () -> Unit,
        )
    }

    /** Loading failed for a reason other than an invalid link — safe to retry. */
    data class Failed(
        val onRetryClick: () -> Unit,
        val onCloseClick: () -> Unit,
    ) : JointAccountInvitePreviewUM

    /**
     * The invite cannot be joined: missing, already redeemed, or this wallet already holds a slot. A dead end —
     * there is nothing to retry, so the screen carries no top bar / close button, only [onGotItClick].
     */
    data class InvalidLink(val onGotItClick: () -> Unit) : JointAccountInvitePreviewUM
}