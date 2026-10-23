package com.tangem.features.jointaccount.join.invitepreview.entity

import com.tangem.features.jointaccount.main.entity.MemberCardConfig

/** Not serializable: the slot is created with `serializer = null`, so an open sheet just closes on process death */
internal sealed interface JointAccountInvitePreviewSheetConfig {

    data object WalletSelector : JointAccountInvitePreviewSheetConfig

    data class CreatorCard(val card: MemberCardConfig) : JointAccountInvitePreviewSheetConfig

    data class AboutSignatures(val threshold: Int, val total: Int) : JointAccountInvitePreviewSheetConfig
}