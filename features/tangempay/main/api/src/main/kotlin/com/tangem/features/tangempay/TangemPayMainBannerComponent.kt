package com.tangem.features.tangempay

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.StateFlow

@Stable
interface TangemPayMainBannerComponent {

    val state: StateFlow<TangemPayMainBannerState>

    fun LazyListScope.tangemPayMainBannerContent(
        state: TangemPayMainBannerState,
        isHidingMode: Boolean,
        modifier: Modifier,
    )

    data class Params(val selectedUserWalletId: UserWalletId)

    interface Factory : ComponentFactory<Params, TangemPayMainBannerComponent>
}