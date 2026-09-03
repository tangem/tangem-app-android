package com.tangem.features.jointaccount.main.state.transformers

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.utils.transformer.Transformer
import com.tangem.core.ui.R as CoreUiR

internal class UpdateMembersInitialStateTransformer(
    private val isInviteMode: Boolean,
    private val onShareSafelyClick: () -> Unit,
    private val onArchiveClick: () -> Unit,
    private val onCloseClick: () -> Unit,
) : Transformer<JointAccountMembersUM> {

    override fun transform(prevState: JointAccountMembersUM): JointAccountMembersUM = prevState.copy(
        title = resourceReference(
            id = if (isInviteMode) {
                CoreUiR.string.joint_account_invite_members_title
            } else {
                CoreUiR.string.joint_account_members_title
            },
        ),
        shareSafely = JointAccountMembersUM.ShareSafelyUM(
            title = resourceReference(CoreUiR.string.joint_account_invite_members_safety_title),
            description = resourceReference(CoreUiR.string.joint_account_invite_members_safety_subtitle),
            onClick = onShareSafelyClick,
        ),
        onArchiveClick = onArchiveClick,
        onCloseClick = onCloseClick,
    )
}