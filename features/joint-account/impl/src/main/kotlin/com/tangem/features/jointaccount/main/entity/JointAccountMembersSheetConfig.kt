package com.tangem.features.jointaccount.main.entity

import com.tangem.core.ui.extensions.TextReference

/** Not serializable: the slot is created with `serializer = null`, so an open sheet just closes on process death */
internal sealed interface JointAccountMembersSheetConfig {

    data class MemberCard(val card: MemberCardConfig) : JointAccountMembersSheetConfig

    data class AboutSignatures(val threshold: Int, val total: Int) : JointAccountMembersSheetConfig

    data class InviteMember(val message: TextReference) : JointAccountMembersSheetConfig
}