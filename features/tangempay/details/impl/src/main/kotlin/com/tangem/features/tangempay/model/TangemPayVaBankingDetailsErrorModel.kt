package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.models.account.VirtualAccountOnramp
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.features.tangempay.components.TangemPayVaBankingDetailsErrorComponent
import com.tangem.features.tangempay.entity.TangemPayVaBankingDetailsErrorUM
import com.tangem.features.tangempay.utils.ifLoadedOrNull
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayVaBankingDetailsErrorModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
) : Model() {

    private val params = paramsContainer.require<TangemPayVaBankingDetailsErrorComponent.Params>()

    val uiState: StateFlow<TangemPayVaBankingDetailsErrorUM>
        field = MutableStateFlow(
            TangemPayVaBankingDetailsErrorUM(
                isRetryLoading = false,
                onRetryClick = ::onRetryClick,
                onContactSupportClick = params.onContactSupport,
                onDismiss = ::onDismiss,
            ),
        )

    fun onDismiss() {
        params.onDismiss()
    }

    private fun onRetryClick() {
        if (uiState.value.isRetryLoading) return
        uiState.update { it.copy(isRetryLoading = true) }
        modelScope.launch {
            paymentAccountStatusFetcher.invoke(params.userWalletId)
            val onramp = paymentAccountStatusSupplier.invoke(params.userWalletId)
                .first()
                .ifLoadedOrNull { it.virtualAccount }
            when (onramp) {
                is VirtualAccountOnramp.Available,
                VirtualAccountOnramp.Eligible,
                -> params.onResolved(onramp)
                // Still failing (BankCredentialsError) or unavailable — keep the sheet, clear the loader.
                else -> uiState.update { it.copy(isRetryLoading = false) }
            }
        }
    }
}