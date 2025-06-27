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
            onBackClick = { /* [REDACTED_TODO_COMMENT] */ },
            onMobileWalletClick = ::onMobileWalletClick,
            onHardwareWalletClick = ::onHardwareWalletClick,
            onScanClick = ::onScanClick,
        ),
    )

    private fun onMobileWalletClick() {
        // TODO navigate to mobile wallet creation
    }

    private fun onHardwareWalletClick() {
        // TODO open card order web page
    }

    private fun onScanClick() {
        // TODO open card scanning
    }
}