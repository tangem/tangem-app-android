package com.tangem.features.jointaccount.join.invitepreview.state

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.features.jointaccount.join.invitepreview.ui.state.JointAccountInvitePreviewUM
import com.tangem.utils.transformer.Transformer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ModelScoped
internal class JointAccountInvitePreviewStateController @Inject constructor() {

    val uiState: StateFlow<JointAccountInvitePreviewUM>
        field = MutableStateFlow<JointAccountInvitePreviewUM>(
            value = JointAccountInvitePreviewUM.Loading(onCloseClick = {}),
        )

    fun update(transformer: Transformer<JointAccountInvitePreviewUM>) {
        uiState.update(function = transformer::transform)
    }
}