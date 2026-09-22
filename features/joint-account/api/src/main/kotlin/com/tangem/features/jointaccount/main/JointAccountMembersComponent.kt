package com.tangem.features.jointaccount.main

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Full-screen joint-account members component, shown as a modal over the wallet screen.
 *
 * The wallet row, account settings and the push-notification deep link all push the same route;
 * the screen renders whatever the account's current data says — see `MembersScreenPhase` for how
 * [com.tangem.domain.jointaccount.model.JointAccount.Status] and "does this device hold the
 * account's invites" (only the creating device ever does) drive the title, progress line,
 * share-safely banner, archive menu and activation CTA.
 */
interface JointAccountMembersComponent : ComposableContentComponent {

    /**
     * @property userWalletId wallet the joint account belongs to.
     * @property cryptoAccountId backend id of the joint account's own row in the wallet's accounts. `null` only
     * in navigation state saved before the field existed: the screen falls back to the wallet's first joint account.
     */
    data class Params(
        val userWalletId: UserWalletId,
        val cryptoAccountId: String?,
    )

    interface Factory : ComponentFactory<Params, JointAccountMembersComponent>
}