package com.tangem.features.jointaccount.main.state.transformers

import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf

/**
 * The account left the supplier (refreshed away, removed, or a transient upstream error emitted the
 * producer's empty fallback): stale members must not survive it. The top bar is left untouched — its
 * callbacks (including [JointAccountMembersUM.TopBarUM.Dismissible.onArchiveClick], which already closes
 * over the last known account id) stay valid, and resetting it here would flip a `Titled` bar back to
 * `Dismissible` for a state that is expected to be transient.
 */
internal class ClearMembersAccountTransformer : Transformer<JointAccountMembersUM> {

    override fun transform(prevState: JointAccountMembersUM): JointAccountMembersUM = prevState.copy(
        header = null,
        shareSafely = null,
        joined = persistentListOf(),
        freeSlots = persistentListOf(),
        otherMembersLabel = null,
        onActivateClick = null,
    )
}