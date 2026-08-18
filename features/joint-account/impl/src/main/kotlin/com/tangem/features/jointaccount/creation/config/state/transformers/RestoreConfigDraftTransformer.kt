package com.tangem.features.jointaccount.creation.config.state.transformers

import com.tangem.features.jointaccount.creation.config.ui.state.JointAccountConfigUM
import com.tangem.features.jointaccount.creation.model.JointAccountCreationDraft
import com.tangem.utils.transformer.Transformer

internal class RestoreConfigDraftTransformer(
    private val draft: JointAccountCreationDraft.Config,
) : Transformer<JointAccountConfigUM> {

    override fun transform(prevState: JointAccountConfigUM): JointAccountConfigUM = prevState.copy(
        name = draft.name,
        icon = prevState.icon.copy(value = draft.icon, color = draft.color),
        isContinueEnabled = true,
    )
}