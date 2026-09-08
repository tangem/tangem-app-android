package com.tangem.features.jointaccount.main.state

import com.tangem.core.decompose.di.ModelScoped
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
        topBar = JointAccountMembersUM.TopBarUM.Dismissible(
            canArchive = false,
            onArchiveClick = {},
            onCloseClick = {},
        ),
        header = null,
        shareSafely = null,
        joined = persistentListOf(),
        freeSlots = persistentListOf(),
        otherMembersLabel = null,
        onActivateClick = null,
    )
}