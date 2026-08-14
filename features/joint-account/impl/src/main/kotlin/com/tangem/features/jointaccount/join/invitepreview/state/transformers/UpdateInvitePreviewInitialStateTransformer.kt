package com.tangem.features.jointaccount.join.invitepreview.state.transformers

import com.tangem.features.jointaccount.join.invitepreview.ui.state.JointAccountInvitePreviewUM
import com.tangem.utils.transformer.Transformer

internal class UpdateInvitePreviewInitialStateTransformer(
    private val onCreatorInfoClick: () -> Unit,
    private val onContinueClick: () -> Unit,
    private val onCloseClick: () -> Unit,
) : Transformer<JointAccountInvitePreviewUM> {

    override fun transform(prevState: JointAccountInvitePreviewUM): JointAccountInvitePreviewUM = prevState.copy(
        onCreatorInfoClick = onCreatorInfoClick,
        onContinueClick = onContinueClick,
        onCloseClick = onCloseClick,
    )
}