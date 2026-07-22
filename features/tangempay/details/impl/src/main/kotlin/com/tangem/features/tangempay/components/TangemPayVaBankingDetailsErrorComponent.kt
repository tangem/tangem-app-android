package com.tangem.features.tangempay.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.account.VirtualAccountOnramp
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.tangempay.model.TangemPayVaBankingDetailsErrorModel
import com.tangem.features.tangempay.ui.TangemPayVaBankingDetailsErrorBottomSheet

/**
 * Error bottom sheet shown when VA bank credentials fail to load ([VirtualAccountOnramp.BankCredentialsError]).
 *
 * "Try again" re-fetches the payment account status while showing a loader on the button; on success the
 * resolved on-ramp is handed back via [Params.onResolved] (the parent opens the bank-transfer sheet), otherwise
 * the error stays visible with the loader cleared.
 */
internal class TangemPayVaBankingDetailsErrorComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayVaBankingDetailsErrorModel = getOrCreateModel(params = params)

    override fun dismiss() {
        model.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()
        TangemPayVaBankingDetailsErrorBottomSheet(state = state)
    }

    data class Params(
        val userWalletId: UserWalletId,
        val onDismiss: () -> Unit,
        val onContactSupport: () -> Unit,
        val onResolved: (VirtualAccountOnramp) -> Unit,
    )
}