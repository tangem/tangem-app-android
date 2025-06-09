package com.tangem.features.walletconnect.transaction.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pushNew
import com.tangem.blockchain.common.TransactionData
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.walletconnect.WcRequestUseCaseFactory
import com.tangem.domain.walletconnect.usecase.method.*
import com.tangem.features.walletconnect.connections.routing.WcInnerRoute
import com.tangem.features.walletconnect.transaction.components.common.WcTransactionModelParams
import com.tangem.features.walletconnect.transaction.converter.WcCommonTransactionUMConverter
import com.tangem.features.walletconnect.transaction.entity.blockaid.WcSendReceiveTransactionCheckResultsUM
import com.tangem.features.walletconnect.transaction.entity.common.WcCommonTransactionModel
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionActionsUM
import com.tangem.features.walletconnect.transaction.entity.send.WcSendTransactionUM
import com.tangem.features.walletconnect.transaction.routes.WcTransactionRoutes
import com.tangem.features.walletconnect.transaction.ui.blockaid.WcSendAndReceiveBlockAidUiConverter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class WcSendTransactionModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val clipboardManager: ClipboardManager,
    private val useCaseFactory: WcRequestUseCaseFactory,
    private val converter: WcCommonTransactionUMConverter,
    private val blockAidUiConverter: WcSendAndReceiveBlockAidUiConverter,
) : Model(), WcCommonTransactionModel {

    private val params = paramsContainer.require<WcTransactionModelParams>()

    private val _uiState = MutableStateFlow<WcSendTransactionUM?>(null)
    override val uiState: StateFlow<WcSendTransactionUM?> = _uiState

    val stackNavigation = StackNavigation<WcTransactionRoutes>()

    init {
        @Suppress("UnusedPrivateMember")
        modelScope.launch {
            val unknownMethodRunnable = { router.push(WcInnerRoute.UnsupportedMethodAlert(params.rawRequest)) }
            val useCase = useCaseFactory.createUseCase<WcSignUseCase<*>>(params.rawRequest)
                .onLeft { unknownMethodRunnable() }
                .getOrNull() ?: return@launch
            when (useCase) {
                is WcListTransactionUseCase,
                is WcTransactionUseCase,
                -> combine(
                    useCase.invoke(),
                    useCase.securityStatus,
                ) { signState, securityCheck -> signState to securityCheck }
                    .distinctUntilChanged()
                    .collectLatest {
                        val (signState, securityCheck) = it
                        if (signingIsDone(signState)) return@collectLatest
                        val signModel: Any = signState.signModel
                        val isMutableFee = useCase is WcMutableFee
                        val dAppFee = if (isMutableFee) useCase.dAppFee() else null
                        val selectedFee = when (signModel) {
                            is TransactionData.Compiled -> signModel.fee
                            is TransactionData.Uncompiled -> signModel.fee
                            else -> null
                        }
                        val isDAppFeeSelected = dAppFee != null && dAppFee == selectedFee

                        val isSecurityCheckContent = securityCheck is Lce.Content
                        var isApprovalMethod = isSecurityCheckContent &&
                            securityCheck.content is BlockAidTransactionCheck.Result.Approval
                        buildUiState(securityCheck, useCase, signState)
                    }
                else -> unknownMethodRunnable()
            }
        }
    }

    private suspend fun buildUiState(
        securityCheck: Lce<Throwable, BlockAidTransactionCheck.Result>,
        useCase: WcSignUseCase<*>,
        signState: WcSignState<*>,
    ) {
        val blockAidState = when (securityCheck) {
            is Lce.Content -> blockAidUiConverter.convert(securityCheck.content.result)
            is Lce.Error -> WcSendReceiveTransactionCheckResultsUM(isLoading = false)
            is Lce.Loading -> WcSendReceiveTransactionCheckResultsUM(isLoading = true)
        }
        var transactionUM = converter.convert(
            WcCommonTransactionUMConverter.Input(
                useCase = useCase,
                signState = signState,
                actions = WcTransactionActionsUM(
                    onShowVerifiedAlert = ::showVerifiedAlert,
                    onDismiss = { cancel(useCase) },
                    onSign = useCase::sign,
                    onCopy = { copyData(useCase.rawSdkRequest.request.params) },
                ),
            ),
        ) as? WcSendTransactionUM
        transactionUM = transactionUM?.copy(
            transaction = transactionUM.transaction.copy(estimatedWalletChanges = blockAidState),
        )
        _uiState.emit(transactionUM)
    }

    override fun dismiss() {
        _uiState.value?.transaction?.onDismiss?.invoke() ?: router.pop()
    }

    fun showTransactionRequest() {
        stackNavigation.pushNew(WcTransactionRoutes.TransactionRequestInfo)
    }

    private fun showVerifiedAlert(appName: String) {
        stackNavigation.pushNew(WcTransactionRoutes.Alert(WcTransactionRoutes.Alert.Type.Verified(appName)))
    }

    private fun signingIsDone(signState: WcSignState<*>): Boolean {
        (signState.domainStep as? WcSignStep.Result)?.result?.let {
            router.pop()
            return true
        }
        return false
    }

    private fun cancel(useCase: WcSignUseCase<*>) {
        useCase.cancel()
        router.pop()
    }

    private fun copyData(text: String) {
        clipboardManager.setText(text = text, isSensitive = true)
    }
}