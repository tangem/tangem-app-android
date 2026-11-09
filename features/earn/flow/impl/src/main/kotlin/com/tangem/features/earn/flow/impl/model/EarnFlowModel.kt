package com.tangem.features.earn.flow.impl.model

import arrow.core.Either
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.router.stack.replaceAll
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.amountScreen.AmountScreenClickIntents
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.userwallet.ext.walletInterationIcon
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.features.earn.api.ui.EarnHowItWorksUM
import com.tangem.features.earn.flow.api.EarnFlowComponent
import com.tangem.features.earn.flow.impl.EarnSessionFactories
import com.tangem.features.earn.flow.impl.ui.state.EarnAmountUM
import com.tangem.features.earn.flow.impl.ui.state.EarnConfirmUM
import com.tangem.features.earn.flow.impl.ui.state.EarnResultUM
import com.tangem.features.earn.product.api.product.EarnTextBlock
import com.tangem.features.earn.product.api.session.EarnActionInput
import com.tangem.features.earn.product.api.session.EarnAmount
import com.tangem.features.earn.product.api.session.EarnFlowAction
import com.tangem.features.earn.product.api.session.EarnFlowExit
import com.tangem.features.earn.product.api.session.EarnPlan
import com.tangem.features.earn.product.api.session.EarnSession
import com.tangem.features.earn.product.api.session.EarnSessionError
import com.tangem.features.earn.product.api.session.EarnSigningStep
import com.tangem.features.earn.product.api.session.EarnTxRef
import com.tangem.features.earn.product.api.session.EarnTxStatus
import com.tangem.features.earn.product.api.session.EarnVerification
import com.tangem.features.earn.product.api.session.VerifiedPlan
import com.tangem.features.send.api.subcomponents.feeSelector.FeeSelectorReloadTrigger
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Runs one [EarnFlowAction] of a product through its [EarnSession]: amount, confirmation and result. The flow knows
 * neither the product's transactions nor the number of signing rounds: those live in the session's `execute`.
 */
@Suppress("LongParameterList", "TooManyFunctions", "LargeClass")
@ModelScoped
internal class EarnFlowModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    sessionFactories: EarnSessionFactories,
    private val statuses: EarnFlowStatuses,
    private val wallet: EarnFlowWallet,
    private val feeSelectorReloadTrigger: FeeSelectorReloadTrigger,
    private val messageSender: UiMessageSender,
    private val converter: EarnFlowConverter,
    private val support: EarnFlowSupport,
    private val links: EarnFlowLinks,
) : Model(), AmountScreenClickIntents {

    private val params: EarnFlowComponent.Params = paramsContainer.require()

    private val foundSession: EarnSession? = sessionFactories.find(params.flowProduct.productId)
        ?.create(params.product)

    /** Only reached once the statuses load, which never starts without a session: see `init`. */
    private val session: EarnSession
        get() = checkNotNull(foundSession) { "Product ${params.flowProduct.productId} has no shared flow" }

    val action: EarnFlowAction = params.action

    private val amountSource: EarnActionInput.Amount.Source? = (action.input as? EarnActionInput.Amount)?.source

    val initialStep: EarnFlowStep = if (amountSource != null) EarnFlowStep.Amount else EarnFlowStep.Confirm

    val navigation = StackNavigation<EarnFlowStep>()

    val state: StateFlow<EarnFlowState>
        field = MutableStateFlow(EarnFlowState(step = initialStep))

    private val screens by lazy(LazyThreadSafetyMode.NONE) {
        EarnFlowScreens(
            action = action,
            product = params.flowProduct,
            clickIntents = this,
            amountCallbacks = EarnAmountCallbacks(
                onPercentClick = ::onPercentClick,
                onSubmit = ::onSubmit,
                onHowItWorksClick = ::onHowItWorksClick,
            ),
            confirmCallbacks = EarnConfirmCallbacks(
                onInfoClick = ::showInfo,
                onConfirm = ::onConfirmClick,
                onLegalClick = links::open,
            ),
            resultCallbacks = EarnResultCallbacks(
                onInfoClick = ::showInfo,
                onExit = ::finish,
                onExploreClick = links::open,
                onShareClick = links::share,
            ),
            onFeeReload = ::onFeeReload,
        )
    }

    private val amountInput = EarnAmountInput(clickIntents = this)

    private var feeJob: Job? = null
    private var feeStatusJob: Job? = null
    private var prepareJob: Job? = null
    private var executeJob: Job? = null

    /** Declared after everything the converters read: `Eagerly` on an immediate dispatcher maps right here. */
    val amount: StateFlow<EarnAmountUM?> = state.map(::toAmountUM).stateIn(modelScope, SharingStarted.Eagerly, null)

    val confirm: StateFlow<EarnConfirmUM?> = state.map(::toConfirmUM).stateIn(modelScope, SharingStarted.Eagerly, null)

    val result: StateFlow<EarnResultUM?> = state.map(::toResultUM).stateIn(modelScope, SharingStarted.Eagerly, null)

    val howItWorks: StateFlow<EarnHowItWorksUM?>
        field = MutableStateFlow(null)

    init {
        if (foundSession == null) {
            TangemLogger.e("Earn flow: product ${params.flowProduct.productId} has no shared flow")
            // Posted, not immediate: the parent is still building this component.
            modelScope.launch(dispatchers.main) { finish(EarnFlowExit.Done) }
        } else {
            subscribeOnStatuses()
            if (amountSource != null) loadMaxAmount()
            val userWallet = wallet.userWallet(params.product)
            state.update { it.copy(walletIcon = userWallet?.let(::walletInterationIcon)) }
            if (amountSource == EarnActionInput.Amount.Source.Wallet) loadSource(userWallet)
        }
    }

    private fun loadSource(userWallet: UserWallet?) {
        modelScope.launch {
            val source = wallet.source(params.product, userWallet)
            state.update { it.copy(source = source) }
        }
    }

    suspend fun loadFee(): Either<GetFeeError, TransactionFee> = session.loadFee(action, enteredSessionAmount())

    suspend fun loadFeeExtended(feeCurrency: CryptoCurrencyStatus?): Either<GetFeeError, TransactionFeeExtended> =
        session.loadFeeExtended(action, enteredSessionAmount(), feeCurrency)

    fun onFeeResult(feeSelectorUM: FeeSelectorUM) {
        feeJob?.cancel()
        // A new fee on the confirmation outdates the plan at once, before the suspend calls below re-prepare it.
        state.update { current ->
            val isConfirming = current.step == EarnFlowStep.Confirm && !current.signing.isExecuting
            current.copy(
                fee = feeSelectorUM,
                isPreparing = prepareJob?.isActive == true || isConfirming && feeSelectorUM.isPrimaryButtonEnabled,
            )
        }
        val selection = state.value.feeSelection ?: return

        feeJob = modelScope.launch {
            val isSubtractAvailable = amountSource == EarnActionInput.Amount.Source.Wallet &&
                statuses.isAmountSubtractAvailable(params.product, selection)
            state.update { it.copy(isAmountSubtractAvailable = isSubtractAvailable) }

            val current = state.value
            if (current.step == EarnFlowStep.Confirm && !current.signing.isExecuting) prepare()
        }
    }

    override fun onAmountValueChange(value: String) {
        amountInput.change(state.value, value)?.let(::updateAmount)
    }

    override fun onMaxValueClick() {
        amountInput.max(state.value)?.let(::updateAmount)
    }

    override fun onCurrencyChangeClick(isFiat: Boolean) {
        val amountState = amountInput.currency(state.value, isFiat) ?: return
        state.update { it.copy(amountState = amountState) }
    }

    override fun onAmountPasteTriggerDismiss() {
        state.update { it.copy(amountState = amountInput.dismissPasteTrigger(it)) }
    }

    override fun onAmountNext() {
        onSubmit()
    }

    fun onPercentClick(percent: Int) {
        amountInput.percent(state.value, percent)?.let(::updateAmount)
    }

    fun onSubmit() {
        // The keyboard's Done reaches here too, so the button state is checked rather than trusted.
        // A running fee job is still computing the subtraction for the new fee.
        if (toAmountUM(state.value)?.primary?.isEnabled != true || feeJob?.isActive == true) return
        prepare(onPrepared = { moveTo(EarnFlowStep.Confirm) })
    }

    /** Without a plan for the selected fee (a failed or outdated `prepare`) the button prepares again. */
    fun onConfirmClick() {
        val current = state.value
        if (current.signing.isExecuting || current.isPreparing || feeJob?.isActive == true) return
        val plan = current.plan?.takeIf { current.isPlanCurrent } ?: return prepare()

        state.update { it.copy(signing = EarnSigningState(stepLabel = null, isExecuting = true)) }
        modelScope.launch {
            when (val verification = session.verify(plan)) {
                is EarnVerification.Verified -> executeIfPlanCurrent(verification.plan)
                is EarnVerification.Changed -> onPlanChanged(verification.newPlan)
                is EarnVerification.Rejected -> {
                    state.update { it.copy(signing = EarnSigningState.Idle) }
                    showSessionError(verification.error)
                }
            }
        }
    }

    fun onBack() {
        val current = state.value
        when {
            current.signing.isExecuting -> Unit
            current.step == EarnFlowStep.Confirm && amountSource != null -> {
                prepareJob?.cancel()
                state.update { it.copy(step = EarnFlowStep.Amount, plan = null, isPreparing = false) }
                navigation.pop()
            }
            else -> finish(EarnFlowExit.Done)
        }
    }

    fun onClose() {
        if (!state.value.signing.isExecuting) finish(EarnFlowExit.Done)
    }

    fun onFeeReload() = modelScope.launch { feeSelectorReloadTrigger.triggerUpdate() }

    fun onHowItWorksDismiss() {
        howItWorks.value = null
    }

    private fun onHowItWorksClick() {
        val content = params.flowProduct.howItWorks ?: return
        howItWorks.value = converter.howItWorks(content, onLearnMoreClick = links::open)
    }

    private fun subscribeOnStatuses() {
        statuses.currency(params.product)
            .onEach { status ->
                updateWithInitialAmount { it.copy(status = status) }
                if (feeStatusJob == null && status.value !is CryptoCurrencyStatus.Loading) {
                    feeStatusJob = modelScope.launch {
                        val feeStatus = statuses.feeCurrency(params.product, status)
                        state.update { it.copy(feeStatus = feeStatus) }
                    }
                }
            }
            .launchIn(modelScope)
        statuses.appCurrency()
            .onEach { appCurrency -> updateWithInitialAmount { it.copy(appCurrency = appCurrency) } }
            .launchIn(modelScope)
    }

    private fun loadMaxAmount() {
        modelScope.launch {
            session.maxAmount(action).fold(
                ifLeft = { error ->
                    TangemLogger.e("Earn flow: ${action.id} max amount failed: $error")
                    messageSender.send(
                        EarnFlowDialogs.sessionError(
                            error = error,
                            currency = params.product.currency,
                            onRetry = ::loadMaxAmount,
                            onClose = { finish(EarnFlowExit.Done) },
                        ),
                    )
                },
                ifRight = { max -> updateWithInitialAmount { it.copy(maxAmount = max) } },
            )
        }
    }

    /** The amount card leaves its shimmer once balance, currency status and app currency are all known. */
    private fun updateWithInitialAmount(transform: (EarnFlowState) -> EarnFlowState) {
        state.update { current ->
            val updated = transform(current)
            if (amountSource != null) amountInput.withInitial(updated) else updated
        }
    }

    private fun updateAmount(amountState: AmountState) {
        prepareJob?.cancel()
        state.update { it.copy(amountState = amountState, isPreparing = false) }
        reloadFee()
    }

    private fun reloadFee() {
        modelScope.launch {
            feeSelectorReloadTrigger.triggerLoadingState()
            feeSelectorReloadTrigger.triggerUpdate()
        }
    }

    private fun enteredSessionAmount(): EarnAmount? = state.value.enteredAmount?.let(EarnAmount::Exact)

    private fun prepare(onPrepared: () -> Unit = {}) {
        val current = state.value
        val selection = current.feeSelection ?: return
        val amount = if (amountSource != null) EarnAmount.Exact(current.sendingAmount ?: return) else null

        prepareJob?.cancel()
        state.update { it.copy(isPreparing = true) }
        prepareJob = modelScope.launch {
            session.prepare(action, amount, selection).fold(
                ifLeft = { error ->
                    state.update { it.copy(plan = null, planFee = null, isPreparing = false) }
                    onPrepareFailed(error)
                },
                ifRight = { plan ->
                    state.update { it.copy(plan = plan, planFee = selection, isPreparing = false) }
                    onPrepared()
                },
            )
        }
    }

    /**
     * A minimum the field could not know beforehand is shown under the field, the way Send and Staking show it. The
     * field checks the entered amount, so a minimum missed only after the fee deduction goes to a dialog.
     */
    private fun onPrepareFailed(error: EarnSessionError) {
        if (error is EarnSessionError.BelowMinimum && state.value.step == EarnFlowStep.Amount) {
            state.update { amountInput.withMinimum(it, error.min) }
            val field = (state.value.amountState as? AmountState.Data)?.amountTextField
            if (field?.isError == true) return
        }
        showSessionError(error)
    }

    /** The session moved the numbers: the fee block reloads, and the new fee prepares a plan to confirm again. */
    private fun onPlanChanged(newPlan: EarnPlan) {
        state.update { it.copy(plan = newPlan, signing = EarnSigningState.Idle) }
        reloadFee()
    }

    /** The fee block stays open during `verify`: a fee picked meanwhile outdates the verified plan. */
    private fun executeIfPlanCurrent(plan: VerifiedPlan) {
        if (state.value.isPlanCurrent) {
            execute(plan)
        } else {
            state.update { it.copy(signing = EarnSigningState.Idle) }
            prepare()
        }
    }

    private fun execute(plan: VerifiedPlan) {
        executeJob?.cancel()
        executeJob = session.execute(plan)
            .onEach { step ->
                when (step) {
                    is EarnSigningStep.AwaitingConfirmation -> state.update {
                        it.copy(signing = EarnSigningState(stepLabel = step.stepLabel, isExecuting = true))
                    }
                    // A sent transaction consumed the plan: confirming again needs a fresh prepare.
                    is EarnSigningStep.Sent -> state.update {
                        it.copy(plan = null, planFee = null, sentAmount = it.sentAmount ?: it.plan?.expectedAmount)
                    }
                    is EarnSigningStep.Done -> showResult(step)
                    is EarnSigningStep.Failed -> onSigningFailed(step.error)
                }
            }
            .onUnfinished(::onSigningUnfinished)
            .launchIn(modelScope)
    }

    /**
     * Once the transaction is in the network, a failure dialog would be false and a second confirmation would send it
     * twice: the flow closes and the token history shows the transaction.
     */
    private fun onSigningUnfinished(sentRef: EarnTxRef?, error: Throwable?) {
        when {
            sentRef != null -> {
                TangemLogger.e("Earn flow: ${action.id} ended without a result after ${sentRef.id} was sent", error)
                state.update { it.copy(signing = EarnSigningState.Idle) }
                finish(EarnFlowExit.Done)
            }
            error != null -> onSigningFailed(SendTransactionError.UnknownError(error as? Exception))
            else -> state.update { it.copy(signing = EarnSigningState.Idle) }
        }
    }

    private fun showResult(done: EarnSigningStep.Done) {
        state.update { current ->
            current.copy(
                signing = EarnSigningState.Idle,
                result = done.result,
                resultTimestamp = System.currentTimeMillis(),
                txStatus = EarnTxStatus(TxInfo.TransactionStatus.Unconfirmed),
                explorerUrl = if (done.result.isExplorerShown) {
                    links.explorerUrl(done.ref, params.product.currency)
                } else {
                    null
                },
            )
        }
        moveTo(EarnFlowStep.Result)

        session.track(done.ref)
            .onEach { txStatus -> state.update { it.copy(txStatus = txStatus) } }
            .catch { error -> TangemLogger.e("Earn flow: tracking ${done.ref.id} failed", error) }
            .launchIn(modelScope)
    }

    private fun onSigningFailed(error: SendTransactionError) {
        state.update { it.copy(signing = EarnSigningState.Idle) }
        if (error is SendTransactionError.UserCancelledError) return

        TangemLogger.e("Earn flow: ${action.id} failed: $error")
        support.transactionErrorDialog(
            error = error,
            product = params.product,
            state = state::value,
            scope = modelScope,
            onClose = { finish(EarnFlowExit.Done) },
        )?.let(messageSender::send)
    }

    /** The stack is not saved, while this model outlives the activity: a recreated stack starts from the kept step. */
    fun restoredStack(): List<EarnFlowStep> = state.value.stack(hasAmountStep = amountSource != null)

    private fun moveTo(step: EarnFlowStep) {
        state.update { it.copy(step = step) }
        when (step) {
            EarnFlowStep.Result -> navigation.replaceAll(step)
            else -> navigation.pushNew(step)
        }
    }

    private fun finish(exit: EarnFlowExit) = params.callback.onFinished(exit)

    private fun showSessionError(error: EarnSessionError) {
        TangemLogger.e("Earn flow: ${action.id} session error: $error")
        messageSender.send(EarnFlowDialogs.sessionError(error, params.product.currency))
    }

    private fun toAmountUM(current: EarnFlowState): EarnAmountUM? = converter.amount(current, screens)

    private fun toConfirmUM(current: EarnFlowState): EarnConfirmUM? = converter.confirm(current, screens)

    private fun toResultUM(current: EarnFlowState): EarnResultUM? = converter.result(current, screens)

    private fun showInfo(block: EarnTextBlock) {
        messageSender.send(EarnFlowDialogs.info(block))
    }
}