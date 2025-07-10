package com.tangem.features.hotwallet.createmobilewallet

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.features.hotwallet.createmobilewallet.entity.CreateMobileWalletUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class CreateMobileWalletModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<DefaultCreateMobileWalletComponent.Params>()

    internal val uiState: StateFlow<CreateMobileWalletUM>
    field = MutableStateFlow(
        CreateMobileWalletUM(
            onBackClick = { router.pop() },
            onCreateClick = ::onCreateClick,
        ),
    )

    private fun onCreateClick() {
        modelScope.launch {
            val tangemHotSdk = params.tangemHotSdk.instanceAccess.get() ?: return@launch
            // TODO
            // val hotWalletId = tangemHotSdk.generateWallet(HotAuth.NoAuth, mnemonicType = MnemonicType.Words12)
            // val hotUserWalletBuilder = hotUserWalletBuilderFactory.create(hotWalletId, tangemHotSdk)
            // saveUserWalletUseCase(
            //     hotUserWalletBuilder.build(),
            // )

            // router.push(AppRoute.Wallet)
        }
    }
}