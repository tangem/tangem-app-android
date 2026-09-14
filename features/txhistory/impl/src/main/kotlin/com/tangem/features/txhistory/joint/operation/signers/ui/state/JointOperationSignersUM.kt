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

    /**

     * @property isAnswered drives the confirmation mark; a member who declined carries none
     */
    @Immutable
    data class SignerUM(
        val name: String,
        val avatar: JointAccountMembersUM.MemberAvatarUM,
        val statusText: TextReference,
        val isAnswered: Boolean,
    )
}