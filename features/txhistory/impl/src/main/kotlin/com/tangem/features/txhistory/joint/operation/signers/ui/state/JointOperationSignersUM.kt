package com.tangem.features.txhistory.joint.operation.signers.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class JointOperationSignersUM(
    val membersCount: Int,
    val signers: ImmutableList<SignerUM>,
) {

    /** @property mark how the member answered; `null` while they are still to answer */
    @Immutable
    data class SignerUM(
        val name: String,
        val avatar: JointAccountMembersUM.MemberAvatarUM,
        val statusText: TextReference,
        val mark: Mark?,
    )

    enum class Mark { Signed, Declined }
}