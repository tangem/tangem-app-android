package com.tangem.features.jointaccount.join.invitepreview.entity

import com.tangem.features.jointaccount.main.entity.MemberCardConfig

/**

 * with `serializer = null`, so an open sheet is not restored across process death, it just closes.
 */
internal sealed interface JointAccountInvitePreviewSheetConfig {

    data object WalletSelector : JointAccountInvitePreviewSheetConfig

    data class CreatorCard(val card: MemberCardConfig) : JointAccountInvitePreviewSheetConfig

    /**
     * "About signatures" sheet.
     *
     * @property threshold signatures required to execute an operation.
     * @property total     members of the account.
     */
    data class AboutSignatures(val threshold: Int, val total: Int) : JointAccountInvitePreviewSheetConfig
}