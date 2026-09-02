package com.tangem.features.jointaccount.main

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Full-screen joint-account members component, shown as a modal over the wallet screen.
 *
 * The same component renders both the creator's working invite view and the reference view opened
 * from account settings — see [Params.mode]. Whether the invite/activate actions are offered is not
 * a parameter: only the creating device holds the account's invites, so the screen derives it from
 * their presence.
 */
interface JointAccountMembersComponent : ComposableContentComponent {

    /**
     * @property userWalletId wallet the joint account belongs to.
     * @property cryptoAccountId backend id of the joint account's own row in the wallet's accounts. `null` only
     * in navigation state saved before the field existed: the screen falls back to the wallet's first joint account.
     * @property mode         [Mode.Invite] for the working invite view, [Mode.Reference] for the settings view.
     */
    data class Params(
        val userWalletId: UserWalletId,
        val cryptoAccountId: String?,
        val mode: Mode,
    )

    enum class Mode { Invite, Reference }

    interface Factory : ComponentFactory<Params, JointAccountMembersComponent>
}