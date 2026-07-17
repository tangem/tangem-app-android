package com.tangem.features.tangempay.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.account.VirtualAccountOnramp
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.tangempay.model.TangemPayVirtualAccountDepositModel
import com.tangem.features.tangempay.ui.TangemPayVirtualAccountDepositBottomSheet

/**
 * Bank-transfer deposit bottom sheet (VA MVP0, TWI-1638). Opened from the add-funds "Bank transfer" option.
 * Renders the on-ramp intro; the [VirtualAccountOnramp.Eligible] state additionally shows a T&C consent footer.
 */
internal class TangemPayVirtualAccountDepositComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayVirtualAccountDepositModel = getOrCreateModel(params = params)

    override fun dismiss() {
        model.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()
        TangemPayVirtualAccountDepositBottomSheet(state = state)
    }

    data class Params(
        val virtualAccountOnramp: VirtualAccountOnramp,
        val userWalletId: UserWalletId,
        val paymentAccountAddress: String,
        val onDismiss: () -> Unit,
        val onShowDetails: (VirtualAccountOnramp.Available) -> Unit,
        val onShowBankingDetailsError: () -> Unit,
        val onOrderCreated: () -> Unit,
    )
}