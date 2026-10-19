package com.tangem.features.jointaccount.main.entity

import androidx.annotation.DrawableRes
import com.tangem.core.ui.extensions.TextReference

/**
 * Configs of the bottom sheets the members screen hosts in its single slot. Not serializable — the slot is

 */
internal sealed interface JointAccountMembersSheetConfig {

    data class MemberCard(val card: MemberCardConfig) : JointAccountMembersSheetConfig

    /**
     * "Activate account" confirmation. [cryptoAccountId] is threaded back into
     * [com.tangem.features.jointaccount.main.model.JointAccountMembersModel.onConfirmActivationClick] rather
     * than re-read off the screen state, so the confirm action always targets the account the sheet was
     * opened for.
     */
    data class ActivationConfirmation(
        val cryptoAccountId: String,
        @DrawableRes val walletInteractionIconRes: Int?,
    ) : JointAccountMembersSheetConfig

    /**
     * "About signatures" sheet.
     *
     * @property threshold signatures required to execute an operation.
     * @property total     members of the account.
     */
    data class AboutSignatures(
        val threshold: Int,
        val total: Int,
    ) : JointAccountMembersSheetConfig

    /**
     * "Invite Member" sheet.
     *
     * @property message the invite text shown in the sheet and sent on share.
     */
    data class InviteMember(val message: TextReference) : JointAccountMembersSheetConfig
}