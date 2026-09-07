package com.tangem.features.jointaccount.main.state.transformers

import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf

internal class ClearMembersAccountTransformer : Transformer<JointAccountMembersUM> {

    override fun transform(prevState: JointAccountMembersUM): JointAccountMembersUM = prevState.copy(
        progress = TextReference.EMPTY,
        members = persistentListOf(),
        otherMembersLabel = null,
        canArchive = false,
        activation = null,
    )
}