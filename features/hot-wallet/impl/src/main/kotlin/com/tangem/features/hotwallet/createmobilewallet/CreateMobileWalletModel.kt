package com.tangem.features.hotwallet.createmobilewallet

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.wallets.builder.HotUserWalletBuilder
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.features.hotwallet.createmobilewallet.entity.CreateMobileWalletUM
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.MnemonicType
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@ModelScoped
internal class CreateMobileWalletModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val hotUserWalletBuilderFactory: HotUserWalletBuilder.Factory,
    private val saveUserWalletUseCase: SaveWalletUseCase,
    private val router: Router,
    private val tangemHotSdk: TangemHotSdk,
) : Model() {

    internal val uiState: StateFlow<CreateMobileWalletUM>
        field = MutableStateFlow(
            CreateMobileWalletUM(
                onBackClick = { router.pop() },
                onCreateClick = ::onCreateClick,
                createButtonLoading = false,
            ),
        )

    private fun onCreateClick() {
        modelScope.launch {
            uiState.update {
                it.copy(createButtonLoading = true)
            }

            runCatching {
                val hotWalletId = tangemHotSdk.generateWallet(HotAuth.NoAuth, mnemonicType = MnemonicType.Words12)
                val hotUserWalletBuilder = hotUserWalletBuilderFactory.create(hotWalletId)
                val userWallet = hotUserWalletBuilder.build()
                saveUserWalletUseCase(userWallet)
                router.replaceAll(AppRoute.Wallet)
            }.onFailure {
                Timber.e(it)

                uiState.update {
                    it.copy(createButtonLoading = false)
                }
            }
        }
    }
}