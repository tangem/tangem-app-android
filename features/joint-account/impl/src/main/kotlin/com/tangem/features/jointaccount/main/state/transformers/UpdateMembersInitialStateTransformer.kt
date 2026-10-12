package com.tangem.features.jointaccount.main.state.transformers

import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.utils.transformer.Transformer

/** Wires the real [onCloseClick] into the initial state before the first account emission arrives. */
internal class UpdateMembersInitialStateTransformer(
    private val onCloseClick: () -> Unit,
) : Transformer<JointAccountMembersUM> {

    override fun transform(prevState: JointAccountMembersUM): JointAccountMembersUM = prevState.copy(
        topBar = JointAccountMembersUM.TopBarUM.Dismissible(
            canArchive = false,
            onArchiveClick = {},
            onCloseClick = onCloseClick,
        ),
    )
}