package com.tangem.features.tangempay.withdraw.bank.preview.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderResult
import com.tangem.domain.pay.usecase.bankofframp.CreateBankOfframpOrderUseCase
import com.tangem.features.tangempay.account.TangemPayAccountDetailsInnerRoute
import com.tangem.features.tangempay.common.TangemPayDetailsErrorType
import com.tangem.features.tangempay.common.TangemPayMessagesFactory
import com.tangem.features.tangempay.withdraw.bank.navigation.BankWithdrawEditTarget
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import com.tangem.features.tangempay.withdraw.bank.preview.TangemPayBankWithdrawPreviewComponent
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUMFactory
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayBankWithdrawPreviewModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val uiMessageSender: UiMessageSender,
    private val createBankOfframpOrderUseCase: CreateBankOfframpOrderUseCase,
) : Model() {

    private val params = paramsContainer.require<TangemPayBankWithdrawPreviewComponent.Params>()

    private val umFactory = TangemPayBankWithdrawPreviewUMFactory(
        callbacks = TangemPayBankWithdrawPreviewUMFactory.Callbacks(
            onBackClick = ::onBackClick,
            onCloseClick = ::onCloseClick,
            onSendClick = ::send,
            onRetryClick = ::send,
            onEditClick = ::onEditClick,
        ),
    )

    private val state = MutableStateFlow(PreviewState())

    /**
     * Generated on the first Send and kept while Retry is offered, so a retry can never create a second order.
     * Cleared once the attempt is settled — an order exists, or the backend refused a concurrent withdrawal — so the
     * next Send is a new attempt with a new key.
     */
    private var idempotencyKey: String? = null

    val uiState: StateFlow<TangemPayBankWithdrawPreviewUM>
        field = MutableStateFlow(umFactory.create(draft = params.draft.value, state = state.value))

    init {
        combine(params.draft, state) { draft, previewState -> umFactory.create(draft = draft, state = previewState) }
            .onEach { uiState.value = it }
            .launchIn(modelScope)
    }

    private fun send() {
        if (state.value.isSending) return
        val request = params.draft.value.toOrderRequest() ?: return
        val key = idempotencyKey ?: UUID.randomUUID().toString().also { idempotencyKey = it }
        state.update { it.copy(isSending = true, failure = null) }
        modelScope.launch {
            createBankOfframpOrderUseCase(userWalletId = params.userWalletId, request = request, idempotencyKey = key)
                .fold(ifLeft = ::onSendFailed, ifRight = ::onOrderCreated)
        }
    }

    /** The screen keeps its sending state: the wizard takes over from here with the order processing. */
    private fun onOrderCreated(result: BankOfframpOrderResult) {
        idempotencyKey = null
        params.onOrderCreated(result)
    }

    private fun onSendFailed(error: BankOfframpError) {
        TangemLogger.withTag(TAG).e("Bank withdraw order creation failed: $error")
        return when (error) {
            BankOfframpError.WithdrawalInProgress -> {
                idempotencyKey = null
                state.update { it.copy(isSending = false) }
                val message = TangemPayMessagesFactory.createErrorMessage(
                    errorType = TangemPayDetailsErrorType.WithdrawInProgress,
                )
                uiMessageSender.send(message)
            }
            BankOfframpError.RecipientRegionUnsupported -> fail(PreviewFailure.RecipientRegion)
            BankOfframpError.SenderRegionUnsupported -> fail(PreviewFailure.SenderRegion)
            BankOfframpError.BusinessUnsupported -> fail(PreviewFailure.Business)
            BankOfframpError.Validation,
            BankOfframpError.PollingTimeout,
            is BankOfframpError.Unknown,
            -> fail(PreviewFailure.General)
        }
    }

    private fun fail(failure: PreviewFailure) {
        state.update { it.copy(isSending = false, failure = failure) }
    }

    private fun onBackClick() {
        if (state.value.isSending) return
        router.pop()
    }

    private fun onCloseClick() {
        if (state.value.isSending) return
        router.popTo(TangemPayAccountDetailsInnerRoute.AccountDetails)
    }

    private fun onEditClick(target: BankWithdrawEditTarget) {
        if (state.value.isSending) return
        router.push(TangemPayBankWithdrawInnerRoute.Edit(target))
    }
}

private const val TAG = "TangemPayBankWithdrawPreviewModel"