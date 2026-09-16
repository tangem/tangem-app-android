package com.tangem.features.jointaccount.join.invitepreview.state.transformers

import com.tangem.features.jointaccount.join.confirmation.ui.state.JointAccountJoinConfirmationUM
import com.tangem.features.jointaccount.join.invitepreview.ui.state.JointAccountInvitePreviewUM
import com.tangem.utils.transformer.Transformer

/** Shows the joining confirmation sheet when [confirmation] is not `null` and hides it otherwise */
internal class SetJoinConfirmationTransformer(
    private val confirmation: JointAccountJoinConfirmationUM?,
) : Transformer<JointAccountInvitePreviewUM> {

    override fun transform(prevState: JointAccountInvitePreviewUM): JointAccountInvitePreviewUM {
        if (prevState !is JointAccountInvitePreviewUM.Content) return prevState

        return prevState.copy(confirmation = confirmation)
    }
}