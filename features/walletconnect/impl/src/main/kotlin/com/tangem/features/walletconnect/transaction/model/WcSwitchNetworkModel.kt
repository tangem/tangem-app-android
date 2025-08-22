package com.tangem.features.walletconnect.transaction.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.walletconnect.WcRequestUseCaseFactory
import com.tangem.domain.walletconnect.model.HandleMethodError
import com.tangem.domain.walletconnect.usecase.method.WcSwitchNetworkUseCase
import com.tangem.features.walletconnect.transaction.components.common.WcTransactionModelParams
import com.tangem.features.walletconnect.transaction.converter.WcHandleMethodErrorConverter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class WcSwitchNetworkModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val useCaseFactory: WcRequestUseCaseFactory,
) : Model() {

    private val params = paramsContainer.require<WcTransactionModelParams>()

    init {
        modelScope.launch {
            val useCase = useCaseFactory.createUseCase<WcSwitchNetworkUseCase>(params.rawRequest)
                .onLeft { showErrorDialog(it) }
                .getOrNull() ?: return@launch
            val either = useCase.invoke()
            useCase.reject()
            either
                .onLeft { showErrorDialog(it) }
                .map {
                    if (it.isExistInWcSession) {
                        router.pop()
                    } else {
                        showErrorDialog(HandleMethodError.RequiredNetwork(it.network.name))
                    }
                }
        }
    }

    private fun showErrorDialog(error: HandleMethodError) {
        router.push(WcHandleMethodErrorConverter.convert(error))
    }
}