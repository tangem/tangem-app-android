package com.tangem.features.jointaccount.main.entity

import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.jointaccount.main.JointAccountMembersUM

/**
 * Slot config for the member card modal.
 *
 * Carries the member's already-resolved display data. [address] is the raw owner address string —
 * kept unformatted so it can be copied to the clipboard verbatim. Not serializable — the member card

 */
internal data class MemberCardConfig(
    val avatar: JointAccountMembersUM.MemberAvatarUM,
    val name: TextReference,
    val address: String,
)