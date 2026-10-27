package com.tangem.features.jointaccount.main.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.jointaccount.deeplink.JointAccountInviteLinkBuilder
import javax.inject.Inject
import com.tangem.core.ui.R as CoreUiR

@ModelScoped
internal class JointAccountInviteSharing @Inject constructor(
    private val shareManager: ShareManager,
) {

    fun message(inviteId: String, accountName: TextReference): TextReference = resourceReference(
        id = CoreUiR.string.joint_account_invite_member_message,
        formatArgs = wrappedList(accountName, JointAccountInviteLinkBuilder.build(inviteId = inviteId)),
    )

    fun share(message: String) {
        shareManager.shareTrackedText(text = message)
    }
}