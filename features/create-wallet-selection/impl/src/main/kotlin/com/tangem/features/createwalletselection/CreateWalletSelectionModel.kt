package com.tangem.features.createwalletselection

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.features.createwalletselection.entity.CreateWalletSelectionUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ModelScoped
internal class CreateWalletSelectionModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    internal val uiState: StateFlow<CreateWalletSelectionUM>
    field = MutableStateFlow(
        CreateWalletSelectionUM(
            onBackClick = { /* TODO implement */ },
            onMobileWalletClick = ::onMobileWalletClick,
            onHardwareWalletClick = ::onHardwareWalletClick,
            onScanClick = ::onScanClick,
        ),
    )

    private fun onMobileWalletClick() {
// [REDACTED_TODO_COMMENT]
    }

    private fun onHardwareWalletClick() {
// [REDACTED_TODO_COMMENT]
    }

    private fun onScanClick() {
// [REDACTED_TODO_COMMENT]
    }
}
