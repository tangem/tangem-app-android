package com.tangem.features.jointaccount.main.state.transformers

import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf

/** The top bar is kept: resetting it would flip a `Titled` bar back to `Dismissible` for a transient state */
internal class ClearMembersAccountTransformer : Transformer<JointAccountMembersUM> {
    override fun transform(prevState: JointAccountMembersUM): JointAccountMembersUM = prevState.copy(
        header = null,
        summaryCard = null,
        joined = persistentListOf(),
        freeSlots = persistentListOf(),
        membersLabel = null,
        waitingLabel = null,
        footer = null,
    )
}