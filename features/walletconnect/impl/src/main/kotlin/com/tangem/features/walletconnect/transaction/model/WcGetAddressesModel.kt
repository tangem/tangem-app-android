package com.tangem.features.walletconnect.transaction.model

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.domain.blockaid.models.dapp.CheckDAppResult
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.walletconnect.WcRequestUseCaseFactory
import com.tangem.domain.walletconnect.model.HandleMethodError
import com.tangem.domain.walletconnect.usecase.method.WcGetAddressesUseCase
import com.tangem.features.walletconnect.connections.entity.VerifiedDAppState
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.components.common.WcTransactionModelParams
import com.tangem.features.walletconnect.transaction.converter.WcHandleMethodErrorConverter
import com.tangem.features.walletconnect.transaction.entity.addresses.WcGetAddressesUM
import com.tangem.features.walletconnect.transaction.entity.common.WcNetworkInfoUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionAppInfoContentUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Model for Bitcoin getAccountAddresses WalletConnect method.
 *
 * Shows a confirmation dialog to the user before sharing wallet addresses.
 */
@Stable
@ModelScoped
internal class WcGetAddressesModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val useCaseFactory: WcRequestUseCaseFactory,
) : Model() {

    private val params = paramsContainer.require<WcTransactionModelParams>()

    private var useCase: WcGetAddressesUseCase? = null

    var uiState by mutableStateOf<WcGetAddressesUM?>(null)
        private set

    init {
        modelScope.launch {
            val createdUseCase = useCaseFactory.createUseCase<WcGetAddressesUseCase>(params.rawRequest)
                .onLeft { showErrorDialog(it) }
                .getOrNull() ?: return@launch

            useCase = createdUseCase
            initUiState(createdUseCase)
        }
    }

    private fun initUiState(useCase: WcGetAddressesUseCase) {
        val session = useCase.session
        val network = useCase.network

        uiState = WcGetAddressesUM(
            appInfo = WcTransactionAppInfoContentUM(
                appName = session.sdkModel.appMetaData.name,
                appIcon = session.sdkModel.appMetaData.icons.firstOrNull().orEmpty(),
                appSubtitle = session.sdkModel.appMetaData.url,
                verifiedState = if (session.securityStatus == CheckDAppResult.SAFE) {
                    VerifiedDAppState.Verified(onVerifiedClick = {})
                } else {
                    VerifiedDAppState.Unknown
                },
            ),
            networkInfo = WcNetworkInfoUM(
                name = network.name,
                iconRes = R.drawable.img_btc_22,
            ),
            addresses = emptyList(), // Addresses will be shown after approval
            isLoading = false,
            walletInteractionIcon = R.drawable.ic_tangem_24,
            onApprove = ::onApprove,
            onReject = ::onReject,
        )
    }

    private fun onApprove() {
        val currentUseCase = useCase ?: return
        uiState = uiState?.copy(isLoading = true)

        modelScope.launch {
            currentUseCase.invoke()
                .onLeft {
                    timber.log.Timber.e("WC GetAddresses: onApprove failed with error: $it")
                    showErrorDialog(it)
                }
                .onRight {
                    timber.log.Timber.d("WC GetAddresses: onApprove success, calling router.pop()")
                    router.pop()
                }
        }
    }

    private fun onReject() {
        timber.log.Timber.d("WC GetAddresses: onReject, calling router.pop()")
        useCase?.reject()
        router.pop()
    }

    private fun showErrorDialog(error: HandleMethodError) {
        router.push(WcHandleMethodErrorConverter.convert(error))
    }
}