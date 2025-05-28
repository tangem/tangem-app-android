package com.tangem.features.walletconnect.transaction.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.features.walletconnect.transaction.entity.send.WcSendTransactionUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Stable
@ModelScoped
internal class WcSendTransactionModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val _uiState = MutableStateFlow<WcSendTransactionUM?>(null)
    val uiState: StateFlow<WcSendTransactionUM?> = _uiState

    init {
        TODO("Finish model in this task [REDACTED_JIRA]")
    }
}