package com.tangem.features.hotwallet.upgradewallet

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.wallets.usecase.GenerateBuyTangemCardLinkUseCase
import com.tangem.features.hotwallet.upgradewallet.entity.UpgradeWalletUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class UpgradeWalletModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val generateBuyTangemCardLinkUseCase: GenerateBuyTangemCardLinkUseCase,
    private val urlOpener: UrlOpener,
) : Model() {

    internal val uiState: StateFlow<UpgradeWalletUM>
    field = MutableStateFlow(
        UpgradeWalletUM(
            onBackClick = { router.pop() },
            onBuyTangemWalletClick = ::onBuyTangemWalletClick,
            onScanDeviceClick = ::onScanDeviceClick,
        ),
    )

    private fun onBuyTangemWalletClick() {
        modelScope.launch {
            generateBuyTangemCardLinkUseCase.invoke().let { urlOpener.openUrl(it) }
        }
    }

    private fun onScanDeviceClick() {
        // TODO [REDACTED_TASK_KEY]
    }
}