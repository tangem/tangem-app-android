package com.tangem.features.tangempay.addfunds.va.bank

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.tangempay.components.TangemPayBankingDetailsErrorComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayBankingDetailsErrorModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<TangemPayBankingDetailsErrorComponent.Params>()

    val uiState: StateFlow<TangemPayBankingDetailsErrorUM>
        field = MutableStateFlow(
            TangemPayBankingDetailsErrorUM(
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
            params.retry()
                .onRight { credentials -> params.onResolved(credentials) }
                .onLeft { uiState.update { state -> state.copy(isRetryLoading = false) } }
        }
    }
}