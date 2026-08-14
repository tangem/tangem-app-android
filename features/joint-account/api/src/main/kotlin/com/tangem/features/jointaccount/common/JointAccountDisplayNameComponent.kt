package com.tangem.features.jointaccount.common

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.wallet.UserWalletId

interface JointAccountDisplayNameComponent : ComposableContentComponent {

    /**
     * @property userWalletId wallet the member participates with; defines the wallet-interaction icon on the button
     * @property buttonText primary button label — "Create account" / "Join to account"
     * @property initialName pre-filled input when the step is re-entered after its part was already saved
     * @property onContinueClick the flow's own action for the validated entered name
     * @property onCloseClick closes the whole flow; back is handled by the component itself via the router
     */
    data class Params(
        val userWalletId: UserWalletId,
        val buttonText: TextReference,
        val initialName: String?,
        val onContinueClick: (name: String) -> Unit,
        val onCloseClick: () -> Unit,
    )

    interface Factory : ComponentFactory<Params, JointAccountDisplayNameComponent>
}