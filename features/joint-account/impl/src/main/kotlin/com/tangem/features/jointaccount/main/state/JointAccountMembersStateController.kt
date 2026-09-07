package com.tangem.features.jointaccount.main.state

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ModelScoped
internal class JointAccountMembersStateController @Inject constructor() {

    val uiState: StateFlow<JointAccountMembersUM>
        field = MutableStateFlow(value = getInitialState())

    fun update(transformer: Transformer<JointAccountMembersUM>) {
        uiState.update(function = transformer::transform)
    }

    private fun getInitialState(): JointAccountMembersUM = JointAccountMembersUM(
        title = TextReference.EMPTY,
        progress = TextReference.EMPTY,
        shareSafely = JointAccountMembersUM.ShareSafelyUM(
            title = TextReference.EMPTY,
            description = TextReference.EMPTY,
            onClick = {},
        ),
        members = persistentListOf(),
        otherMembersLabel = null,
        canArchive = false,
        activation = null,
        onArchiveClick = {},
        onCloseClick = {},
    )
}