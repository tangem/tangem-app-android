package com.tangem.features.jointaccount.main

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Full-screen joint-account members component, shown as a modal over the wallet screen.
 *
 * The same component renders both the creator's working invite view and the reference view opened
 * from account settings — see [Params.mode] and [Params.isCreator].
 */
interface JointAccountMembersComponent : ComposableContentComponent {

    /**
     * @property userWalletId wallet the joint account belongs to.
     * @property mode         [Mode.Invite] for the working invite view, [Mode.Reference] for the settings view.

     */
    data class Params(
        val userWalletId: UserWalletId,
        val mode: Mode,
        val isCreator: Boolean,
    )

    enum class Mode { Invite, Reference }

    interface Factory : ComponentFactory<Params, JointAccountMembersComponent>
}