package com.tangem.features.jointaccount.join.invitepreview.state.transformers

import com.tangem.features.jointaccount.join.invitepreview.ui.state.JointAccountInvitePreviewUM
import com.tangem.utils.transformer.Transformer

internal class SetContinueLoadingTransformer : Transformer<JointAccountInvitePreviewUM> {

    override fun transform(prevState: JointAccountInvitePreviewUM): JointAccountInvitePreviewUM {
        if (prevState !is JointAccountInvitePreviewUM.Content) return prevState

        return prevState.copy(isContinueLoading = true)
    }
}