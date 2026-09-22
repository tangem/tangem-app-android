package com.tangem.features.txhistory.component

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Stable
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.txhistory.entity.JointCosignRequestsUM
import kotlinx.coroutines.flow.StateFlow

/**
 * The joint account operations in [Params.currency] still collecting signatures, listed above the token history.
 * They are not transactions yet: a tap opens the operation sheet hosted in [Content], not an explorer.
 */
@Stable
interface JointCosignRequestsComponent : ComposableContentComponent {

    val state: StateFlow<JointCosignRequestsUM>

    fun LazyListScope.jointCosignRequestsContent(state: JointCosignRequestsUM)

    data class Params(
        val userWalletId: UserWalletId,
        val currency: CryptoCurrency,
        /** Operation to open on landing, e.g. from a `joint_tx` push notification. Consumed once. */
        val pendingOperationId: String? = null,
    )

    interface Factory : ComponentFactory<Params, JointCosignRequestsComponent>
}