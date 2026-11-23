package com.tangem.features.earn.flow.impl.model

import arrow.core.Either
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.router.stack.replaceAll
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.amountScreen.AmountScreenClickIntents
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.common.ui.userwallet.ext.walletInterationIcon
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isHotWallet
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.features.earn.api.ui.EarnHowItWorksUM
import com.tangem.features.earn.api.ui.EarnRowInfoUM
import com.tangem.features.earn.flow.api.EarnFlowComponent
import com.tangem.features.earn.flow.impl.EarnSessionFactories
import com.tangem.features.earn.flow.impl.analytics.EarnDisplayCurrency
import com.tangem.features.earn.flow.impl.analytics.EarnErrorPopupAction
import com.tangem.features.earn.flow.impl.analytics.EarnFlowAnalyticsEvent
import com.tangem.features.earn.flow.impl.analytics.EarnFlowNotice
import com.tangem.features.earn.flow.impl.analytics.EarnFlowNoticeAnalytics
import com.tangem.features.earn.flow.impl.analytics.EarnFlowScreen
import com.tangem.features.earn.flow.impl.analytics.EarnTxType
import com.tangem.features.earn.flow.impl.analytics.amountScreen
import com.tangem.features.earn.flow.impl.analytics.isLiquidityCapped
import com.tangem.features.earn.flow.impl.analytics.toResultDestination
import com.tangem.features.earn.flow.impl.analytics.toTxType
import com.tangem.features.earn.flow.impl.ui.state.EarnAmountUM
import com.tangem.features.earn.flow.impl.ui.state.EarnConfirmUM
import com.tangem.features.earn.flow.impl.ui.state.EarnResultUM
import com.tangem.features.earn.product.api.analytics.EarnAnalyticsPercent
import com.tangem.features.earn.product.api.analytics.EarnUsdValuation
import com.tangem.features.earn.product.api.analytics.toAnalyticsApy
import com.tangem.features.earn.product.api.product.EarnRowInfo
import com.tangem.features.earn.product.api.product.toUM
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
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
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
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val usdValuation: EarnUsdValuation,
) : Model(), AmountScreenClickIntents {

    private val params: EarnFlowComponent.Params = paramsContainer.require()

    private val foundSession: EarnSession? = sessionFactories.find(params.flowProduct.productId)
        ?.create(params.product)

    /** Only reached once the statuses load, which never starts without a session: see `init`. */
    private val session: EarnSession
        get() = checkNotNull(foundSession) { "Product ${params.flowProduct.productId} has no shared flow" }

    val action: EarnFlowAction = params.action

    private val amountSource: EarnActionInput.Amount.Source? = (action.input as? EarnActionInput.Amount)?.source

    private val txType: EarnTxType? = action.toTxType()

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
                onExit = ::onResultExit,
                onExploreClick = links::open,
                onShareClick = links::share,
            ),
            onFeeReload = ::onFeeReload,
        )
    }

    private val amountInput = EarnAmountInput(clickIntents = this)

    // A load after Loading or Error resets the selector to its default fee: only a loaded-to-loaded change is a pick
    private var lastFeeType: AnalyticsParam.FeeType? = null

    private var lastFeeCurrencyId: CryptoCurrency.ID? = null

    private val noticeAnalytics = EarnFlowNoticeAnalytics(
        analyticsEventHandler = analyticsEventHandler,
        category = params.flowProduct.analytics.category,
    )

    private var feeJob: Job? = null
    private var prepareJob: Job? = null
    private var executeJob: Job? = null
    private var pickedFeeToken: CryptoCurrencyStatus? = null

    /** Declared after everything the converters read: `Eagerly` on an immediate dispatcher maps right here. */
    val amount: StateFlow<EarnAmountUM?> = state.map(::toAmountUM).stateIn(modelScope, SharingStarted.Eagerly, null)

    val confirm: StateFlow<EarnConfirmUM?> = state.map(::toConfirmUM).stateIn(modelScope, SharingStarted.Eagerly, null)

    val result: StateFlow<EarnResultUM?> = state.map(::toResultUM).stateIn(modelScope, SharingStarted.Eagerly, null)

    val howItWorks: StateFlow<EarnHowItWorksUM?>
        field = MutableStateFlow(null)

    val rowInfo: StateFlow<EarnRowInfoUM?>
        field = MutableStateFlow(null)

    init {
        if (foundSession == null) {
            TangemLogger.e("Earn flow: product ${params.flowProduct.productId} has no shared flow")
            // Posted, not immediate: the parent is still building this component.
            modelScope.launch(dispatchers.main) { finish(EarnFlowExit.Done) }
        } else {
            subscribeOnStatuses()
            txType?.let(::subscribeOnNotices)
            if (amountSource != null) loadMaxAmount()
            val userWallet = wallet.userWallet(params.product)
            state.update { flowState ->
                flowState.copy(
                    walletIcon = userWallet?.let(::walletInterationIcon),
                    isHoldToConfirm = userWallet?.isHotWallet == true,
                )
            }
            if (amountSource == EarnActionInput.Amount.Source.Wallet) loadSource(userWallet)
            if (amountSource != null) sendAmountScreenOpened()
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
        session.loadFeeExtended(action, enteredSessionAmount(), feeCurrency ?: pickedFeeToken)

    fun onFeeResult(feeSelectorUM: FeeSelectorUM) {
        rememberFeeTokenPick(feeSelectorUM)
        feeJob?.cancel()
        val isPreviousFeeLoaded = state.value.fee is FeeSelectorUM.Content
        // A new fee on the confirmation outdates the plan at once, before the suspend calls below re-prepare it.
        state.update { current ->
            val isConfirming = current.step == EarnFlowStep.Confirm && !current.signing.isExecuting
            current.copy(
                fee = feeSelectorUM,
                isPreparing = prepareJob?.isActive == true || isConfirming && feeSelectorUM.isPrimaryButtonEnabled,
            )
        }
        sendFeeSpeedIfPicked(isPreviousFeeLoaded)
        val selection = state.value.feeSelection ?: return

        feeJob = modelScope.launch {
            val isSubtractAvailable = amountSource == EarnActionInput.Amount.Source.Wallet &&
                statuses.isAmountSubtractAvailable(params.product, selection)
            val deduction = selection.fee.amount.value?.takeIf { isSubtractAvailable } ?: BigDecimal.ZERO
            val enteredBefore = state.value.enteredAmount
            state.update { current ->
                val withSubtract = current.copy(isAmountSubtractAvailable = isSubtractAvailable)
                if (amountSource != null) amountInput.withFeeDeduction(withSubtract, deduction) else withSubtract
            }

            val current = state.value
            if (!isSameAmount(current.enteredAmount, enteredBefore)) refreshFeeEstimate()
            if (current.step == EarnFlowStep.Confirm && !current.signing.isExecuting) prepare()
        }
    }

    override fun onAmountValueChange(value: String) {
        state.update { it.copy(pendingSharePercent = null) }
        amountInput.change(state.value, value)?.let(::updateAmount)
    }

    override fun onMaxValueClick() {
        if (!deferShareUntilFee(EarnAmountInput.FULL_SHARE_PERCENT)) {
            val amountState = amountInput.max(state.value) ?: return
            updateAmount(amountState)
        }
        sendFlowEvent { category, type -> EarnFlowAnalyticsEvent.Max(category, currentScreen(type)) }
    }

    override fun onCurrencyChangeClick(isFiat: Boolean) {
        val amountState = amountInput.currency(state.value, isFiat) ?: return
        state.update { it.copy(amountState = amountState) }
        val currency = if (isFiat) EarnDisplayCurrency.Fiat else EarnDisplayCurrency.Crypto
        sendFlowEvent { category, type ->
            EarnFlowAnalyticsEvent.CurrencyToggle(category, currency, currentScreen(type))
        }
    }

    override fun onAmountPasteTriggerDismiss() {
        state.update { it.copy(amountState = amountInput.dismissPasteTrigger(it)) }
    }

    override fun onAmountNext() {
        onSubmit()
    }

    fun onPercentClick(percent: Int) {
        if (!deferShareUntilFee(percent)) {
            val amountState = amountInput.percent(state.value, percent) ?: return
            updateAmount(amountState)
        }
        sendFlowEvent { category, type -> EarnFlowAnalyticsEvent.Percent(category, percent, currentScreen(type)) }
    }

    private fun isSameAmount(first: BigDecimal?, second: BigDecimal?): Boolean =
        if (first == null || second == null) first == second else first.compareTo(second) == 0

    /**
     * A wallet Max leaves room for the fee, so a tap before the first fee estimate, the Max or the card is set once all
     * of them are there.
     */
    private fun deferShareUntilFee(percent: Int): Boolean {
        val isWaiting = amountSource == EarnActionInput.Amount.Source.Wallet && !amountInput.isShareReady(state.value)
        if (isWaiting) state.update { it.copy(pendingSharePercent = percent) }
        return isWaiting
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
        if (current.isAmountInvalid) return
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
                state.update { it.copy(step = EarnFlowStep.Amount, plan = null, sentPlan = null, isPreparing = false) }
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
        sendFlowEvent { category, _ -> EarnFlowAnalyticsEvent.HowItWorksOpened(category, params.source) }
        howItWorks.value = content.toUM(
            onLearnMoreClick = { post ->
                sendFlowEvent { category, _ -> EarnFlowAnalyticsEvent.LearnMore(category, params.source) }
                modelScope.launch { links.openBlogPost(post) }
            },
        )
    }

    private fun subscribeOnStatuses() {
        statuses.currency(params.product)
            .onEach { status -> updateWithInitialAmount { it.copy(status = status) } }
            .launchIn(modelScope)
        statuses.feeCurrency(params.product)
            .onEach { feeStatus -> state.update { it.copy(feeStatus = feeStatus) } }
            .launchIn(modelScope)
        statuses.appCurrency()
            .onEach { appCurrency -> updateWithInitialAmount { it.copy(appCurrency = appCurrency) } }
            .launchIn(modelScope)
    }

    private fun subscribeOnNotices(type: EarnTxType) {
        state
            .mapNotNull { current -> noticeFrame(type, current) }
            .onEach { frame -> noticeAnalytics.update(frame.screen, frame.notices, frame.isFeeInFlight) }
            .launchIn(modelScope)
    }

    /** The cards come from the same state: [amount] and [confirm] map it on their own and may lag a state behind. */
    private fun noticeFrame(type: EarnTxType, current: EarnFlowState): NoticeFrame? {
        val isFeeInFlight = current.fee is FeeSelectorUM.Loading
        return when (current.step) {
            EarnFlowStep.Amount -> NoticeFrame(
                screen = type.amountScreen(),
                notices = noticesOf(current, toAmountUM(current)?.notifications, checksAmount = true),
                isFeeInFlight = isFeeInFlight,
            )
            EarnFlowStep.Confirm -> NoticeFrame(
                screen = EarnFlowScreen.Confirm,
                notices = noticesOf(current, toConfirmUM(current)?.notifications, checksAmount = false),
                isFeeInFlight = isFeeInFlight,
            )
            EarnFlowStep.Result -> null
        }
    }

    private fun noticesOf(
        current: EarnFlowState,
        notifications: List<NotificationUM>?,
        checksAmount: Boolean,
    ): Set<EarnFlowNotice> = buildSet {
        if (checksAmount && current.isAmountAboveMax) add(EarnFlowNotice.InsufficientBalance)
        if (notifications.orEmpty().any(::isFeeShortage)) add(EarnFlowNotice.InsufficientFee)
    }

    private fun isFeeShortage(notification: NotificationUM): Boolean =
        notification is NotificationUM.Error.TotalExceedsBalance || notification is NotificationUM.Error.ExceedsBalance

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
    /** A Max or percent tap waiting for the card is set here, so the fee is estimated again for that amount. */
    private fun updateWithInitialAmount(transform: (EarnFlowState) -> EarnFlowState) {
        val enteredBefore = state.value.enteredAmount
        state.update { current ->
            val updated = transform(current)
            if (amountSource != null) amountInput.withInitial(updated) else updated
        }
        if (!isSameAmount(state.value.enteredAmount, enteredBefore)) refreshFeeEstimate()
    }

    private fun updateAmount(amountState: AmountState) {
        prepareJob?.cancel()
        state.update { it.copy(amountState = amountState, isPreparing = false) }
        reloadFee()
    }

    /**
     * The fee moved the amount, and the estimate follows it. Without the loading state the selector keeps the picked
     * speed, custom fee and nonce; the amount already fits with the selected fee, so nothing waits for the estimate.
     */
    private fun refreshFeeEstimate() {
        modelScope.launch { feeSelectorReloadTrigger.triggerUpdate() }
    }

    /** The selector forgets a picked fee token when a reload resets it to loading, so the pick is kept here */
    private fun rememberFeeTokenPick(feeSelectorUM: FeeSelectorUM) {
        val feeExtraInfo = (feeSelectorUM as? FeeSelectorUM.Content)?.feeExtraInfo ?: return
        if (!feeExtraInfo.isFeeTokenSelectedByUser) return
        pickedFeeToken = feeExtraInfo.feeCryptoCurrencyStatus.takeIf { it.currency is CryptoCurrency.Token }
    }

    private fun reloadFee() {
        modelScope.launch {
            feeSelectorReloadTrigger.triggerLoadingState()
            feeSelectorReloadTrigger.triggerUpdate()
        }
    }

    private fun enteredSessionAmount(): EarnAmount? {
        val current = state.value
        return current.enteredAmount?.let { current.toSessionAmount(it) }
    }

    /**
     * A position keeps accruing interest after [EarnFlowState.maxAmount] was read, so an exact max would fall short of
     * it by the time the transaction is built and leave dust behind. The session resolves [EarnAmount.Entire] from
     * its fresh state instead. A wallet max stays exact: the flow deducts the fee from it, the session does not.
     */
    private fun EarnFlowState.toSessionAmount(amount: BigDecimal): EarnAmount {
        val max = maxAmount
        val isEntirePosition = amountSource == EarnActionInput.Amount.Source.Position && max != null && amount >= max
        return if (isEntirePosition) EarnAmount.Entire else EarnAmount.Exact(amount)
    }

    /** A fee that moved the amount out of its limits leaves nothing to prepare: the amount step fixes it */
    private fun prepare(onPrepared: () -> Unit = {}) {
        val current = state.value
        if (current.isAmountInvalid) {
            state.update { it.copy(isPreparing = false) }
            return
        }
        val selection = current.feeSelection ?: return
        val amount = if (amountSource != null) current.toSessionAmount(current.sendingAmount ?: return) else null

        prepareJob?.cancel()
        state.update { it.copy(isPreparing = true) }
        prepareJob = modelScope.launch {
            session.prepare(action, amount, selection).fold(
                ifLeft = { error ->
                    state.update { it.copy(plan = null, planFee = null, sentPlan = null, isPreparing = false) }
                    onPrepareFailed(error)
                },
                ifRight = { plan ->
                    state.update { it.copy(plan = plan, planFee = selection, sentPlan = null, isPreparing = false) }
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
        // Taken from this execution: a retry after a partly sent attempt may carry a different amount.
        val expectedAmount = plan.plan.expectedAmount
        executeJob = session.execute(plan)
            .onEach { step ->
                when (step) {
                    is EarnSigningStep.AwaitingConfirmation -> state.update {
                        it.copy(signing = EarnSigningState(stepLabel = step.stepLabel, isExecuting = true))
                    }
                    // A sent transaction consumed the plan: confirming again needs a fresh prepare.
                    is EarnSigningStep.Sent -> state.update {
                        it.copy(plan = null, planFee = null, sentAmount = expectedAmount, sentPlan = plan.plan)
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
        sendSuccessScreenOpened()

        session.track(done.ref)
            .onEach { txStatus -> state.update { it.copy(txStatus = txStatus) } }
            .catch { error -> TangemLogger.e("Earn flow: tracking ${done.ref.id} failed", error) }
            .launchIn(modelScope)
    }

    private fun onSigningFailed(error: SendTransactionError) {
        state.update { it.copy(signing = EarnSigningState.Idle) }
        if (error is SendTransactionError.UserCancelledError) return

        TangemLogger.e("Earn flow: ${action.id} failed: $error")
        val dialog = support.transactionErrorDialog(
            error = error,
            product = params.product,
            state = state::value,
            scope = modelScope,
            onClose = { finish(EarnFlowExit.Done) },
            onSupportClick = { sendErrorPopupButton(EarnErrorPopupAction.Support) },
            onCancel = { sendErrorPopupButton(EarnErrorPopupAction.Close) },
        ) ?: return
        sendFlowEvent { category, type ->
            EarnFlowAnalyticsEvent.ErrorPopup(category, type, error.getAnalyticsDescription())
        }
        messageSender.send(dialog)
    }

    /** The stack is not saved, while this model outlives the activity: a recreated stack starts from the kept step. */
    fun restoredStack(): List<EarnFlowStep> = state.value.stack(hasAmountStep = amountSource != null)

    private fun moveTo(step: EarnFlowStep) {
        val isNewStep = state.value.step != step
        state.update { it.copy(step = step) }
        when (step) {
            EarnFlowStep.Result -> navigation.replaceAll(step)
            else -> navigation.pushNew(step)
        }
        if (step == EarnFlowStep.Confirm && isNewStep) sendConfirmScreenOpened()
    }

    private fun finish(exit: EarnFlowExit) = params.callback.onFinished(exit)

    private fun onResultExit(exit: EarnFlowExit) {
        exit.toResultDestination()?.let { destination ->
            sendFlowEvent { category, _ -> EarnFlowAnalyticsEvent.ResultAction(category, destination) }
        }
        finish(exit)
    }

    private fun sendAmountScreenOpened() {
        launchFlowEvent { category, type ->
            val screen = session.screenAnalytics(action)
            when (type) {
                EarnTxType.Deposit -> EarnFlowAnalyticsEvent.DepositScreenOpened(
                    category = category,
                    source = params.source,
                    currentApy = screen.rate?.toAnalyticsApy(),
                )
                EarnTxType.Withdraw -> EarnFlowAnalyticsEvent.WithdrawScreenOpened(
                    category = category,
                    positionBucket = usdValuation.bucket(params.product.currency, screen.position),
                    isLiquidityCapped = screen.isLiquidityCapped(),
                )
            }
        }
    }

    /** The amount Confirm shows: a Max withdrawal resolves the entire position into the plan. */
    private fun sendConfirmScreenOpened() {
        val amount = state.value.plan?.expectedAmount ?: state.value.sendingAmount
        launchFlowEvent { category, type ->
            val screen = session.screenAnalytics(action)
            EarnFlowAnalyticsEvent.ConfirmScreenOpened(
                category = category,
                type = type,
                amountBucket = usdValuation.bucket(params.product.currency, amount),
                currentApy = screen.rate?.toAnalyticsApy(),
                performanceFee = screen.performanceFee?.let(EarnAnalyticsPercent::of),
            )
        }
    }

    private fun sendSuccessScreenOpened() {
        launchFlowEvent { category, type ->
            val screen = session.screenAnalytics(action)
            EarnFlowAnalyticsEvent.SuccessScreenOpened(
                category = category,
                type = type,
                currentApy = screen.rate?.toAnalyticsApy(),
            )
        }
    }

    private fun sendFeeSpeedIfPicked(isPreviousFeeLoaded: Boolean) {
        val feeType = state.value.feeSelection?.feeType ?: return
        val feeCurrencyId = (state.value.fee as? FeeSelectorUM.Content)
            ?.feeExtraInfo
            ?.feeCryptoCurrencyStatus
            ?.currency
            ?.id
        val previous = lastFeeType
        val isSameFeeToken = feeCurrencyId == lastFeeCurrencyId
        val isSpeedPick = isPreviousFeeLoaded && isSameFeeToken
        lastFeeType = feeType
        lastFeeCurrencyId = feeCurrencyId
        if (isSpeedPick && previous != null && previous != feeType) {
            sendFlowEvent { category, type -> EarnFlowAnalyticsEvent.FeeSpeed(category, feeType, currentScreen(type)) }
        }
    }

    private fun sendErrorPopupButton(action: EarnErrorPopupAction) {
        sendFlowEvent { category, type -> EarnFlowAnalyticsEvent.ErrorPopupButton(category, type, action) }
    }

    private fun currentScreen(type: EarnTxType): EarnFlowScreen =
        if (state.value.step == EarnFlowStep.Confirm) EarnFlowScreen.Confirm else type.amountScreen()

    private fun sendFlowEvent(event: (category: String, type: EarnTxType) -> EarnFlowAnalyticsEvent) {
        val type = txType ?: return
        analyticsEventHandler.send(event(params.flowProduct.analytics.category, type))
    }

    private fun launchFlowEvent(event: suspend (category: String, type: EarnTxType) -> EarnFlowAnalyticsEvent) {
        val type = txType ?: return
        modelScope.launch { analyticsEventHandler.send(event(params.flowProduct.analytics.category, type)) }
    }

    private fun showSessionError(error: EarnSessionError) {
        TangemLogger.e("Earn flow: ${action.id} session error: $error")
        messageSender.send(EarnFlowDialogs.sessionError(error, params.product.currency))
    }

    private fun toAmountUM(current: EarnFlowState): EarnAmountUM? = converter.amount(current, screens)

    private fun toConfirmUM(current: EarnFlowState): EarnConfirmUM? = converter.confirm(current, screens)

    private fun toResultUM(current: EarnFlowState): EarnResultUM? = converter.result(current, screens)

    private fun showInfo(info: EarnRowInfo) {
        rowInfo.value = info.toUM()
    }

    fun onRowInfoDismiss() {
        rowInfo.value = null
    }
}

private data class NoticeFrame(
    val screen: EarnFlowScreen,
    val notices: Set<EarnFlowNotice>,
    val isFeeInFlight: Boolean,
)