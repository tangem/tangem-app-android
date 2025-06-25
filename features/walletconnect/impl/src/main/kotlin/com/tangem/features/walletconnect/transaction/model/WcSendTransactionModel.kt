package com.tangem.features.walletconnect.transaction.model

import androidx.compose.runtime.Stable
import arrow.core.Either
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.domain.blockaid.models.transaction.ValidationResult
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.models.network.Network
import com.tangem.domain.tokens.GetNetworkCoinStatusUseCase
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.walletconnect.WcRequestUseCaseFactory
import com.tangem.domain.walletconnect.usecase.method.*
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.send.v2.api.params.FeeSelectorParams
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
import java.math.BigDecimal
import javax.inject.Inject
import kotlin.properties.Delegates

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
    private val getFeeUseCase: GetFeeUseCase,
    private val getNetworkCoinUseCase: GetNetworkCoinStatusUseCase,
) : Model(), WcCommonTransactionModel {

    private val params = paramsContainer.require<WcTransactionModelParams>()

    private val _uiState = MutableStateFlow<WcSendTransactionUM?>(null)
    override val uiState: StateFlow<WcSendTransactionUM?> = _uiState

    val stackNavigation = StackNavigation<WcTransactionRoutes>()

    internal var useCase: WcSignUseCase<*> by Delegates.notNull()
    internal var cryptoCurrencyStatus: CryptoCurrencyStatus by Delegates.notNull()
    internal var suggestedFeeState: FeeSelectorParams.SuggestedFeeState = FeeSelectorParams.SuggestedFeeState.None
    private var signState: WcSignState<*> by Delegates.notNull()
    private var wcApproval: WcApproval? = null
    private var sign: () -> Unit = {}

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
                -> {
                    this@WcSendTransactionModel.cryptoCurrencyStatus =
                        getCryptoCurrencyStatus(userWallet = useCase.wallet, network = useCase.network)
                            .onLeft { unknownMethodRunnable() }
                            .getOrNull() ?: return@launch
                    this@WcSendTransactionModel.useCase = useCase
                    (useCase as? WcMutableFee)
                        ?.dAppFee()
                        ?.let { dAppFee ->
                            suggestedFeeState = FeeSelectorParams.SuggestedFeeState.Suggestion(
                                title = stringReference(useCase.session.sdkModel.appMetaData.name),
                                fee = dAppFee,
                            )
                        }
                    combine(
                        useCase.invoke(),
                        useCase.securityStatus,
                    ) { signState, securityCheck -> signState to securityCheck }
                        .distinctUntilChanged()
                        .collectLatest { (signState, securityCheck) ->
                            if (signingIsDone(signState, useCase)) return@collectLatest

                            this@WcSendTransactionModel.signState = signState
                            val isSecurityCheckContent = securityCheck is Lce.Content
                            var isApprovalMethod = isSecurityCheckContent &&
                                securityCheck.content is BlockAidTransactionCheck.Result.Approval
                            wcApproval = useCase as? WcApproval
                            sign = { useCase.sign() }
                            buildUiState(securityCheck, useCase, signState)
                        }
                }
                else -> unknownMethodRunnable()
            }
        }
    }

    suspend fun loadFee(): Either<GetFeeError, TransactionFee> {
        val signModel = signState.signModel
        val transactionData = signModel as? TransactionData.Uncompiled ?: error("TransactionData must be Uncompiled")
        return getFeeUseCase.invoke(
            userWallet = useCase.wallet,
            network = useCase.network,
            transactionData = transactionData,
        )
    }

    private suspend fun getCryptoCurrencyStatus(
        userWallet: UserWallet,
        network: Network,
    ): Either<CurrencyStatusError, CryptoCurrencyStatus> {
        return getNetworkCoinUseCase.invokeSync(
            userWalletId = userWallet.walletId,
            networkId = network.id,
            derivationPath = network.derivationPath,
            isSingleWalletWithTokens = userWallet is UserWallet.Cold &&
                userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken(),
        )
    }

    private suspend fun buildUiState(
        securityCheck: Lce<Throwable, BlockAidTransactionCheck.Result>,
        useCase: WcSignUseCase<*>,
        signState: WcSignState<*>,
    ) {
        val blockAidState = when (securityCheck) {
            is Lce.Content -> blockAidUiConverter.convert(
                WcSendAndReceiveBlockAidUiConverter.Input(securityCheck.content.result, wcApproval?.getAmount()),
            )
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
                    onSign = { onSign(securityCheck.getOrNull()) },
                    onCopy = { copyData(useCase.rawSdkRequest.request.params) },
                ),
            ),
        ) as? WcSendTransactionUM
        transactionUM = transactionUM?.copy(
            transaction = transactionUM.transaction.copy(estimatedWalletChanges = blockAidState),
            spendAllowance = blockAidState.spendAllowance,
        )
        _uiState.emit(transactionUM)
    }

    override fun dismiss() {
        _uiState.value?.transaction?.onDismiss?.invoke() ?: router.pop()
    }

    fun showTransactionRequest() {
        stackNavigation.pushNew(WcTransactionRoutes.TransactionRequestInfo)
    }

    private fun onSign(securityCheck: BlockAidTransactionCheck.Result?) {
        if (securityCheck?.result?.validation == ValidationResult.UNSAFE) {
            showMaliciousAlert(securityCheck.result.description)
        } else {
            sign()
        }
    }

    fun onClickDoneCustomAllowance(value: BigDecimal, isUnlimited: Boolean) {
        val maxValue = if (isUnlimited) Double.MAX_VALUE.toBigDecimal() else value
        wcApproval?.getAmount()?.let { currentAmount ->
            wcApproval?.updateAmount(currentAmount.copy(maxValue = maxValue))
        }
    }

    fun onClickAllowToSpend() {
        wcApproval?.let { stackNavigation.pushNew(WcTransactionRoutes.CustomAllowance) }
    }

    private fun showVerifiedAlert(appName: String) {
        stackNavigation.pushNew(WcTransactionRoutes.Alert(WcTransactionRoutes.Alert.Type.Verified(appName)))
    }

    private fun showMaliciousAlert(description: String?) {
        val type = WcTransactionRoutes.Alert.Type.MaliciousInfo(description = description, onClick = ::signFromAlert)
        stackNavigation.pushNew(WcTransactionRoutes.Alert(type))
    }

    private fun signFromAlert() {
        stackNavigation.pop()
        sign()
    }

    private fun signingIsDone(signState: WcSignState<*>, useCase: WcSignUseCase<*>): Boolean {
        (signState.domainStep as? WcSignStep.Result)?.result?.let {
            handleSigningError(it, useCase)
            return true
        }
        return false
    }

    private fun handleSigningError(result: Either<Throwable, Unit>, useCase: WcSignUseCase<*>) {
        if (result.isLeft()) {
            val error = WcTransactionRoutes.Alert.Type.UnknownError(
                errorMessage = result.leftOrNull()?.message,
                onDismiss = { cancel(useCase) },
            )
            stackNavigation.pushNew(WcTransactionRoutes.Alert(error))
        } else {
            router.pop()
        }
    }

    private fun cancel(useCase: WcSignUseCase<*>) {
        useCase.cancel()
        router.pop()
    }

    private fun copyData(text: String) {
        clipboardManager.setText(text = text, isSensitive = true)
    }
}