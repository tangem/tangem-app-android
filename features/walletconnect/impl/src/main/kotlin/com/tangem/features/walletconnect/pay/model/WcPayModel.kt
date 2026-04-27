package com.tangem.features.walletconnect.pay.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.walletconnect.model.pay.*
import com.tangem.domain.walletconnect.usecase.pay.WcPayUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.features.walletconnect.pay.components.WcPayComponent
import com.tangem.features.walletconnect.pay.entity.PaymentOptionUM
import com.tangem.features.walletconnect.pay.entity.WcPayUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

@Stable
@ModelScoped
@Suppress("LongParameterList")
internal class WcPayModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val wcPayUseCase: WcPayUseCase,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: WcPayComponent.Params = paramsContainer.require()

    private var userWallet: UserWallet? = null
    private var paymentOptionsResponse: WcPaymentOptionsResponse? = null
    private var requiredActions: List<WcPayRequiredAction> = emptyList()
    private var signatures: MutableList<String> = mutableListOf()
    private var currentActionIndex = 0

    private val _uiState = MutableStateFlow(
        WcPayUM(
            state = WcPayUM.State.LOADING,
            merchantName = "",
            merchantIconUrl = null,
            paymentAmount = "",
            paymentCurrency = "",
            options = emptyList(),
            selectedOptionId = null,
            kycUrl = null,
            signingCurrent = 0,
            signingTotal = 0,
            onOptionSelected = ::onOptionSelected,
            onContinue = ::onContinue,
            onDismiss = ::onDismiss,
            errorMessage = null,
        ),
    )
    val uiState: StateFlow<WcPayUM> = _uiState

    private var onDismissCallback: (() -> Unit)? = null

    init {
        loadPaymentOptions()
    }

    fun setDismissCallback(callback: () -> Unit) {
        onDismissCallback = callback
    }

    private fun loadPaymentOptions() {
        modelScope.launch {
            val wallet = getSelectedWalletSyncUseCase().getOrNull() ?: run {
                showError("No wallet selected")
                return@launch
            }
            userWallet = wallet
            val accounts = wcPayUseCase.buildPayAccounts(wallet)

            wcPayUseCase.getPaymentOptions(params.request.uri, accounts)
                .onSuccess { response ->
                    paymentOptionsResponse = response
                    val info = response.info
                    val options = response.options.map { it.toUM(selectedId = null) }

                    _uiState.update { state ->
                        state.copy(
                            state = WcPayUM.State.OPTIONS,
                            merchantName = info?.merchant?.name.orEmpty(),
                            merchantIconUrl = info?.merchant?.iconUrl,
                            paymentAmount = info?.amount?.display
                                ?.let { "${it.assetSymbol} ${response.info?.amount?.value}" }
                                ?: response.info?.amount?.value.orEmpty(),
                            paymentCurrency = info?.amount?.display?.assetSymbol.orEmpty(),
                            options = options,
                            selectedOptionId = options.firstOrNull()?.id,
                        )
                    }
                }
                .onFailure { error ->
                    TangemLogger.e("WC Pay: getPaymentOptions failed", error)
                    showError(error.message ?: "Failed to load payment options")
                }
        }
    }

    private fun onOptionSelected(optionId: String) {
        _uiState.update { state ->
            state.copy(
                selectedOptionId = optionId,
                options = state.options.map { it.copy(isSelected = it.id == optionId) },
            )
        }
    }

    private fun onContinue() {
        val response = paymentOptionsResponse ?: return
        val selectedId = _uiState.value.selectedOptionId ?: return
        val selectedOption = response.options.find { it.id == selectedId } ?: return

        // Check if KYC is required
        val collectDataUrl = selectedOption.collectData?.url
            ?: response.collectDataAction?.url

        if (collectDataUrl != null && _uiState.value.state == WcPayUM.State.OPTIONS) {
            _uiState.update { it.copy(state = WcPayUM.State.KYC_WEBVIEW, kycUrl = collectDataUrl) }
            return
        }

        startPaymentFlow(response.paymentId, selectedId)
    }

    fun onKycCompleted() {
        val response = paymentOptionsResponse ?: return
        val selectedId = _uiState.value.selectedOptionId ?: return
        startPaymentFlow(response.paymentId, selectedId)
    }

    fun onKycFailed() {
        _uiState.update { it.copy(state = WcPayUM.State.OPTIONS) }
    }

    private fun startPaymentFlow(paymentId: String, optionId: String) {
        _uiState.update { it.copy(state = WcPayUM.State.SIGNING) }
        modelScope.launch {
            wcPayUseCase.getRequiredActions(paymentId, optionId)
                .onSuccess { actions ->
                    requiredActions = actions
                    signatures.clear()
                    currentActionIndex = 0
                    processNextAction()
                }
                .onFailure { error ->
                    TangemLogger.e("WC Pay: getRequiredActions failed", error)
                    showError(error.message ?: "Failed to get required actions")
                }
        }
    }

    private fun processNextAction() {
        if (currentActionIndex >= requiredActions.size) {
            _uiState.update { it.copy(signingCurrent = requiredActions.size, signingTotal = requiredActions.size) }
            confirmPayment()
            return
        }

        _uiState.update {
            it.copy(signingCurrent = currentActionIndex + 1, signingTotal = requiredActions.size)
        }

        val action = requiredActions[currentActionIndex]
        modelScope.launch {
            val signature = signAction(action)
            if (signature != null) {
                signatures.add(signature)
                currentActionIndex++
                processNextAction()
            } else {
                showError("Signing failed for action ${currentActionIndex + 1}")
            }
        }
    }

    private suspend fun signAction(action: WcPayRequiredAction): String? {
        val wallet = userWallet ?: return null
        return wcPayUseCase.signPayAction(action, wallet)
            .onFailure { TangemLogger.e("WC Pay: signAction failed", it) }
            .getOrNull()
    }

    private fun confirmPayment() {
        val response = paymentOptionsResponse ?: return
        val selectedId = _uiState.value.selectedOptionId ?: return

        modelScope.launch {
            wcPayUseCase.confirmPayment(response.paymentId, selectedId, signatures)
                .onSuccess { result ->
                    when (result.status) {
                        WcPaymentStatus.SUCCEEDED -> {
                            _uiState.update { it.copy(state = WcPayUM.State.SUCCESS, errorMessage = null) }
                        }
                        WcPaymentStatus.PROCESSING -> {
                            val pollMs = result.pollInMs
                            if (!result.isFinal && pollMs != null) {
                                pollPaymentStatus(response.paymentId, selectedId, pollMs)
                            } else {
                                _uiState.update { it.copy(state = WcPayUM.State.SUCCESS) }
                            }
                        }
                        else -> {
                            _uiState.update { it.copy(state = WcPayUM.State.FAILED) }
                        }
                    }
                }
                .onFailure { error ->
                    TangemLogger.e("WC Pay: confirmPayment failed", error)
                    showError(error.message ?: "Payment confirmation failed")
                }
        }
    }

    private fun pollPaymentStatus(paymentId: String, optionId: String, delayMs: Long, attempt: Int = 0) {
        if (attempt >= MAX_POLL_ATTEMPTS) {
            showError("Payment status check timed out")
            return
        }
        modelScope.launch {
            kotlinx.coroutines.delay(delayMs)
            wcPayUseCase.confirmPayment(paymentId, optionId, signatures)
                .onSuccess { result ->
                    if (result.isFinal) {
                        val state = if (result.status == WcPaymentStatus.SUCCEEDED) {
                            WcPayUM.State.SUCCESS
                        } else {
                            WcPayUM.State.FAILED
                        }
                        _uiState.update { it.copy(state = state) }
                    } else {
                        val nextPollMs = result.pollInMs
                        if (nextPollMs != null) {
                            pollPaymentStatus(paymentId, optionId, nextPollMs, attempt + 1)
                        }
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(state = WcPayUM.State.FAILED) }
                }
        }
    }

    private fun onDismiss() {
        onDismissCallback?.invoke()
    }

    private fun showError(message: String) {
        _uiState.update {
            it.copy(
                state = WcPayUM.State.FAILED,
                errorMessage = stringReference(message),
            )
        }
    }

    private fun WcPaymentOption.toUM(selectedId: String?) = PaymentOptionUM(
        id = id,
        networkName = amount.display?.networkName.orEmpty(),
        networkIconUrl = amount.display?.networkIconUrl,
        tokenSymbol = amount.display?.assetSymbol.orEmpty(),
        tokenIconUrl = amount.display?.iconUrl,
        amount = amount.value,
        estimatedTxs = estimatedTxs,
        requiresKyc = collectData != null,
        isSelected = id == selectedId,
    )

    private companion object {
        const val MAX_POLL_ATTEMPTS = 30
    }
}