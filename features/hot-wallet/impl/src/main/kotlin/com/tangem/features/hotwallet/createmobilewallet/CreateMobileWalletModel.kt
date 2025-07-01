package com.tangem.features.hotwallet.createmobilewallet

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.features.hotwallet.createmobilewallet.entity.CreateMobileWalletUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ModelScoped
internal class CreateMobileWalletModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
) : Model() {

    internal val uiState: StateFlow<CreateMobileWalletUM>
    field = MutableStateFlow(
        CreateMobileWalletUM(
            onBackClick = { router.pop() },
            onCreateClick = ::onCreateClick,
        ),
    )

    private fun onCreateClick() {
        // TODO create a wallet
    }
}