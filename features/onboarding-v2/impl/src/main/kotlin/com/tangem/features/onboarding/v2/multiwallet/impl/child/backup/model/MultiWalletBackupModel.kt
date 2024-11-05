package com.tangem.features.onboarding.v2.multiwallet.impl.child.backup.model

import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.multiwallet.impl.child.backup.ui.state.MultiWalletBackupUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ComponentScoped
class MultiWalletBackupModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    @Suppress("UnusedPrivateMember")
    private val params = paramsContainer.require<MultiWalletChildParams>()

    private val _uiState = MutableStateFlow(
        MultiWalletBackupUM(
            finalizeButtonEnabled = false,
            onAddBackupClick = {},
            onFinalizeButtonClick = {},
        ),
    )

    val uiState: StateFlow<MultiWalletBackupUM> = _uiState
}
