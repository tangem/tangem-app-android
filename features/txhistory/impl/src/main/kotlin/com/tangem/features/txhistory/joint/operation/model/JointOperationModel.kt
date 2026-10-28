package com.tangem.features.txhistory.joint.operation.model

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.JointOperationRejectError
import com.tangem.domain.jointaccount.model.JointOperationSignError
import com.tangem.domain.jointaccount.model.JointOperationSignerState.Answer
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.txhistory.model.JointOperationTx
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent.ErrorSource
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent.JointAccountError
import com.tangem.features.txhistory.converter.FiatConversion
import com.tangem.features.txhistory.converter.JointOperationTxToDetailsUMConverter
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.features.txhistory.joint.operation.JointOperationComponent
import com.tangem.features.txhistory.joint.operation.converter.JointOperationUMConverter
import com.tangem.features.txhistory.joint.operation.route.JointOperationRoutes
import com.tangem.features.txhistory.joint.operation.signers.converter.JointOperationSignersUMConverter
import com.tangem.features.txhistory.joint.operation.signers.withCosign
import com.tangem.features.txhistory.joint.operation.ui.state.JointOperationUM
import com.tangem.features.txhistory.joint.operation.model.JointOperationState.Role
import com.tangem.features.txhistory.model.TxDetailsMenuActions
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.tangem.core.ui.R as CoreUiR

@Suppress("LongParameterList")
@ModelScoped
internal class JointOperationModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val delegate: JointOperationDelegate,
    private val poller: JointOperationPoller,
    private val uiMessageSender: UiMessageSender,
    private val clipboardManager: ClipboardManager,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val menuActions: TxDetailsMenuActions,
    getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
) : Model() {

    private val params: JointOperationComponent.Params = paramsContainer.require()

    private val isColdWallet = delegate.isColdWallet(userWalletId = params.userWalletId)

    private val snapshot: SharedFlow<JointOperationSnapshot> = delegate
        .observe(
            userWalletId = params.userWalletId,
            currency = params.currency,
            operationId = params.operationId,
        )
        .flowOn(dispatchers.io)
        .shareIn(modelScope, SharingStarted.Eagerly, replay = 1)

    /** What the sheet shows right now; the actions act on it, not on a cache of their own */
    private val currentState: JointOperationState?
        get() = (snapshot.replayCache.firstOrNull() as? JointOperationSnapshot.InFlight)?.state

    private val isBalanceHidden = getBalanceHidingSettingsUseCase()
        .map { it.isBalanceHidden }
        .distinctUntilChanged()

    private val isResumed = MutableStateFlow(false)

    private val isOutcomeAwaited = snapshot
        .map { it == JointOperationSnapshot.NotSynced || it is JointOperationSnapshot.InFlight && it.isHeld }
        .distinctUntilChanged()

    private val isSigning = MutableStateFlow(false)
    private val isRefusing = MutableStateFlow(false)
    private val isAwaitingCancellation = MutableStateFlow(false)

    /** The use cases tolerate a failed refresh, so the cached operation may still miss this member's answer */
    private val localAnswer = MutableStateFlow<Answer?>(null)

    private val isGoneLocally = MutableStateFlow(false)

    private val userActions = combine(
        flow = isSigning,
        flow2 = isRefusing,
        flow3 = localAnswer,
        flow4 = isGoneLocally,
        flow5 = isAwaitingCancellation,
        transform = ::UserActions,
    )

    private val isHeld: Boolean
        get() = (snapshot.replayCache.firstOrNull() as? JointOperationSnapshot.InFlight)?.isHeld == true

    private val isBusy: Boolean
        get() = isSigning.value || isRefusing.value || isAwaitingCancellation.value || isHeld

    private val recipient = snapshot
        .map { snapshot ->
            when (snapshot) {
                is JointOperationSnapshot.InFlight -> snapshot.state.displayed.recipient
                is JointOperationSnapshot.Finished -> snapshot.tx.transfer.intent.recipient
                JointOperationSnapshot.NotSynced, JointOperationSnapshot.Unreadable -> null
            }
        }
        .distinctUntilChanged()
        .flatMapLatest { address ->
            if (address == null) return@flatMapLatest flowOf(null)

            delegate.recipient(
                userWalletId = params.userWalletId,
                recipient = address,
                network = params.currency.network,
            )
        }

    private val operationConverter = JointOperationUMConverter(
        currency = params.currency,
        onSignClick = ::onSignClick,
        onSignersClick = ::onSignersClick,
        onCopyRecipientClick = ::onCopyRecipientClick,
        onCreateContactClick = ::onCreateContactClick,
        onRefuseClick = ::onRefuseClick,
    )

    private val refusalMessages = JointOperationRefusalMessages(onConfirm = ::refuse)

    private val signersConverter = JointOperationSignersUMConverter()

    val stackNavigation = StackNavigation<JointOperationRoutes>()

    val uiState: StateFlow<JointOperationUM> = combine(
        flow = snapshot,
        flow2 = userActions,
        flow3 = recipient,
        flow4 = isBalanceHidden,
        flow5 = delegate.fiat(userWalletId = params.userWalletId, currency = params.currency),
    ) { snapshot, actions, recipient, isBalanceHidden, fiat ->
        if (actions.isGone) return@combine JointOperationUM.Gone

        val state = when (snapshot) {
            JointOperationSnapshot.NotSynced -> return@combine JointOperationUM.Loading
            JointOperationSnapshot.Unreadable -> return@combine JointOperationUM.Gone
            is JointOperationSnapshot.Finished -> {
                if (recipient == null) return@combine JointOperationUM.Loading
                return@combine snapshot.toContent(
                    currency = params.currency,
                    recipient = recipient,
                    menu = finishedMenu(tx = snapshot.tx, recipient = recipient),
                    isBalanceHidden = isBalanceHidden,
                    fiat = fiat,
                    onCopyAddress = ::copyAddress,
                    onSignersClick = ::onSignersClick,
                )
            }
            is JointOperationSnapshot.InFlight -> snapshot.state
        }
        val answered = state.withOwnAnswer(answeredLocally = actions.localAnswer)
        val isHeld = (snapshot as? JointOperationSnapshot.InFlight)?.isHeld == true
        val isAwaitingCancellation = actions.isAwaitingCancellation || isHeld ||
            !answered.isReachable && !answered.isSending
        if (recipient == null) return@combine JointOperationUM.Loading

        JointOperationUM.Content(
            operation = operationConverter.convert(
                value = JointOperationUMConverter.Input(
                    state = answered,
                    isSigning = actions.isSigning,
                    isRefusing = actions.isRefusing || isAwaitingCancellation,
                    hasAnswered = actions.localAnswer != null,
                    recipient = recipient,
                    isBalanceHidden = isBalanceHidden,
                    isColdWallet = isColdWallet,
                    fiat = fiat,
                ),
            ),
            signers = signersConverter.convert(
                value = JointOperationSignersUMConverter.Input(
                    membersCount = answered.membersCount,
                    signers = answered.signers,
                ),
            ),
        )
    }
        .stateIn(modelScope, SharingStarted.WhileSubscribed(), initialValue = JointOperationUM.Loading)

    init {
        modelScope.launch {
            val end = combine(isResumed, isOutcomeAwaited) { resumed, awaited -> resumed && awaited }
                .distinctUntilChanged()
                .transformLatest { shouldPoll ->
                    if (shouldPoll) {
                        emit(
                            poller.poll(
                                userWalletId = params.userWalletId,
                                operationId = params.operationId,
                            ),
                        )
                    }
                }
                .first()
            if (end == JointOperationPoller.End.GONE) isGoneLocally.value = true
        }
    }

    fun onResume() {
        isResumed.value = true
    }

    fun onPause() {
        isResumed.value = false
    }

    fun onSignClick() {
        val operation = currentState ?: return
        if (!operation.canSign || isBusy) return

        modelScope.launch {
            isSigning.value = true
            try {
                delegate.sign(
                    state = operation,
                    network = params.currency.network,
                    operationId = params.operationId,
                )
                    .onRight { signedOperation ->
                        localAnswer.value = Answer.SIGNED
                        poller.refresh()
                        signedOperation?.let { sendOperationSigned(state = operation, signedOperation = it) }
                    }
                    .onLeft { error ->
                        if (error.isStaleOperation()) poller.refresh()
                        showSignError(error)
                    }
            } finally {
                isSigning.value = false
            }
        }
    }

    fun onRefuseClick() {
        val operation = currentState ?: return
        if (isBusy || !operation.canRefuse || localAnswer.value == Answer.DECLINED) return

        uiMessageSender.send(refusalMessages.confirmation(isInitiator = operation.role == Role.INITIATOR))
    }

    fun onCreateContactClick() {
        currentState?.displayed?.recipient?.let(::createContact)
    }

    private fun createContact(address: String) {
        delegate.openContactCreation(address = address, network = params.currency.network)
    }

    fun onSignersClick() {
        stackNavigation.pushNew(JointOperationRoutes.Signers)
    }

    fun onBackClick() {
        stackNavigation.pop()
    }

    fun onCopyRecipientClick() {
        val recipient = currentState?.displayed?.recipient ?: return

        copyAddress(recipient)
    }

    private fun copyAddress(address: String) {
        clipboardManager.setText(text = address, isSensitive = false)
    }

    private fun finishedMenu(tx: JointOperationTx, recipient: JointOperationRecipient) = menuActions.menu(
        txInfo = tx,
        currency = params.currency,
        onAddContact = { createContact(tx.transfer.intent.recipient) }.takeIf { recipient.canCreateContact },
    )

    /** The use case tolerates a failed refresh, so a deletion (or an operation already gone) is recorded locally too */
    private fun refuse() {
        val operation = currentState ?: return
        if (isBusy) return

        modelScope.launch {
            isRefusing.value = true
            try {
                delegate.reject(
                    state = operation,
                    network = params.currency.network,
                    operationId = params.operationId,
                )
                    .onRight {
                        onRefused(operation)
                        poller.refresh()
                    }
                    .onLeft { error ->
                        if (error.isStaleOperation()) poller.refresh()
                        showRejectError(error = error, role = operation.role)
                    }
            } finally {
                isRefusing.value = false
            }
        }
    }

    private fun onRefused(operation: JointOperationState) {
        when (operation.role) {
            Role.INITIATOR -> {
                isAwaitingCancellation.value = true
                showDeleted()
            }
            Role.MEMBER -> localAnswer.value = Answer.DECLINED
            Role.UNKNOWN -> Unit
        }
    }

    private fun sendOperationSigned(state: JointOperationState, signedOperation: JointOperation) {
        val signedBy = signedOperation.signatures
            .filter { it.state == JointOperation.MemberSignature.State.SIGNED }
            .map(JointOperation.MemberSignature::address)
            .plus(listOfNotNull(signedOperation.initiator ?: state.initiator))
            .distinct()

        analyticsEventHandler.send(
            JointAccountAnalyticsEvent.OperationSigned(
                signaturesCollected = signedBy.size,
                membersCount = state.membersCount,
                threshold = state.threshold,
                // An operation executed or failed right after this signature comes back with an unknown status
                isThresholdReached = signedBy.size >= state.threshold,
                blockchain = params.currency.network.name,
                token = params.currency.symbol,
            ),
        )
    }

    private fun showDeleted() {
        uiMessageSender.send(
            SnackbarMessage(
                message = resourceReference(CoreUiR.string.joint_operation_deleted_snackbar),
                startIconId = CoreUiR.drawable.ic_trash_24,
            ),
        )
    }

    private fun showSignError(error: JointOperationSignError) {
        val message = JointOperationErrorMessage(onSupportClick = ::onSupportClick).convert(error)
            ?: return

        TangemLogger.e("Unable to sign the joint operation: $error")
        val analyticsEvent = when (error) {
            is JointOperationSignError.Failed -> JointAccountError(source = ErrorSource.Sign, cause = error.cause)
            else -> JointAccountError(source = ErrorSource.Sign, errorMessage = error.toString())
        }
        analyticsEventHandler.send(analyticsEvent)
        uiMessageSender.send(message)
    }

    private fun onSupportClick() {
        modelScope.launch { delegate.openSupport(userWalletId = params.userWalletId) }
    }

    private fun showRejectError(error: JointOperationRejectError, role: Role) {
        val message = refusalMessages.error(error) ?: return

        TangemLogger.e("Unable to reject the joint operation: $error")
        val source = if (role == Role.INITIATOR) ErrorSource.Delete else ErrorSource.Decline
        val analyticsEvent = when (error) {
            is JointOperationRejectError.Failed -> JointAccountError(source = source, cause = error.cause)
            else -> JointAccountError(source = source, errorMessage = error.toString())
        }
        analyticsEventHandler.send(analyticsEvent)
        uiMessageSender.send(message)
    }

    /** What this member is doing or has done on the sheet, on top of what the backend reports */
    private data class UserActions(
        val isSigning: Boolean,
        val isRefusing: Boolean,
        val localAnswer: Answer?,
        val isGone: Boolean,
        val isAwaitingCancellation: Boolean,
    )
}

@Suppress("LongParameterList")
private fun JointOperationSnapshot.Finished.toContent(
    currency: CryptoCurrency,
    recipient: JointOperationRecipient,
    menu: ImmutableList<TxHistoryDetailsUM.MenuItemUM>,
    isBalanceHidden: Boolean,
    fiat: JointOperationFiat,
    onCopyAddress: (String) -> Unit,
    onSignersClick: () -> Unit,
): JointOperationUM.Content = JointOperationUM.Content(
    operation = JointOperationTxToDetailsUMConverter(
        currency = currency,
        onCopyAddress = onCopyAddress,
        menu = menu,
        isBalanceHidden = isBalanceHidden,
        resolveRecipient = { recipient.owner },
        fiatConversion = FiatConversion(rate = fiat.rate, appCurrency = fiat.appCurrency),
    )
        .convert(tx)
        .withCosign(signatures = signatures, onSignersClick = onSignersClick),
    signers = signatures.signers,
)

private fun JointOperationSignError.isStaleOperation(): Boolean =
    this == JointOperationSignError.OperationNotFound || this == JointOperationSignError.ThresholdReached

private fun JointOperationRejectError.isStaleOperation(): Boolean =
    this == JointOperationRejectError.OperationNotFound || this == JointOperationRejectError.ThresholdReached