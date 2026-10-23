package com.tangem.features.jointaccount.main.entity

import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.jointaccount.main.JointAccountMembersUM

/** [address] stays unformatted so it is copied to the clipboard verbatim */
internal data class MemberCardConfig(
    val avatar: JointAccountMembersUM.MemberAvatarUM,
    val name: TextReference,
    val address: String,
)