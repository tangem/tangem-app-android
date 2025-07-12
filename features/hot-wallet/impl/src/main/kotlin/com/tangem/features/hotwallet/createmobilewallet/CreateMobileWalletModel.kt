package com.tangem.features.hotwallet.createmobilewallet

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.features.hotwallet.createmobilewallet.entity.CreateMobileWalletUM
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.MnemonicType
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class CreateMobileWalletModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val tangemHotSdk: TangemHotSdk,
) : Model() {

    internal val uiState: StateFlow<CreateMobileWalletUM>
    field = MutableStateFlow(
        CreateMobileWalletUM(
            onBackClick = { router.pop() },
            onCreateClick = ::onCreateClick,
        ),
    )

    private fun onCreateClick() {
        modelScope.launch {
            tangemHotSdk.generateWallet(HotAuth.NoAuth, mnemonicType = MnemonicType.Words12)
            // TODO
            // val hotUserWalletBuilder = hotUserWalletBuilderFactory.create(hotWalletId, tangemHotSdk)
            // saveUserWalletUseCase(
            //     hotUserWalletBuilder.build(),
            // )

            // router.push(AppRoute.Wallet)
        }
    }
}