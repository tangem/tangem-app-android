package com.tangem.features.jointaccount.confirmaddress.model

import androidx.compose.runtime.Stable
import com.tangem.common.routing.AppRoute
import com.tangem.common.ui.userwallet.ext.walletInterationIcon
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.account.supplier.SingleAccountSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncOrNull
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointAccountConfirmationError
import com.tangem.domain.jointaccount.model.JointAccountFlowError
import com.tangem.domain.jointaccount.polling.JointAccountPollingCoordinator
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.usecase.ConfirmJointAccountUseCase
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.wallets.usecase.EnsureWalletLinkedUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent.ErrorSource
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent.JointAccountError
import com.tangem.features.jointaccount.confirmaddress.JointAccountConfirmAddressComponent
import com.tangem.features.jointaccount.confirmaddress.ui.state.JointAccountConfirmAddressUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import com.tangem.core.ui.R as CoreUiR

@OptIn(ExperimentalCoroutinesApi::class)
@Stable
@ModelScoped
@Suppress("LongParameterList")
internal class JointAccountConfirmAddressModel @Inject constructor(
    paramsContainer: ParamsContainer,
    userWalletsListRepository: UserWalletsListRepository,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val uiMessageSender: UiMessageSender,
    private val shareManager: ShareManager,
    private val jointAccountListSupplier: SingleJointAccountListSupplier,
    private val singleAccountSupplier: SingleAccountSupplier,
    private val confirmJointAccountUseCase: ConfirmJointAccountUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val ensureWalletLinkedUseCase: EnsureWalletLinkedUseCase,
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
    private val pollingCoordinator: JointAccountPollingCoordinator,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model() {

    private val params = paramsContainer.require<JointAccountConfirmAddressComponent.Params>()

    val uiState: StateFlow<JointAccountConfirmAddressUM>
        field = MutableStateFlow(
            JointAccountConfirmAddressUM(
                address = null,
                shareMessage = null,
                buttonIcon = userWalletsListRepository
                    .getSyncOrNull(id = params.userWalletId)
                    ?.let(::walletInterationIcon)
                    ?.let { iconRes -> TangemIconUM.Icon(iconRes = iconRes) },
                isConfirming = false,
                onShareClick = ::onShareClick,
                onMismatchClick = ::onMismatchClick,
                onConfirmClick = ::onConfirmClick,
                onBackClick = router::pop,
            ),
        )

    private val isResumed = MutableStateFlow(false)

    private val jointAccount: StateFlow<JointAccount?> = jointAccountListSupplier(userWalletId = params.userWalletId)
        .map { accounts -> accounts.firstOrNull { it.cryptoAccountId == params.cryptoAccountId } }
        .stateIn(scope = modelScope, started = SharingStarted.Eagerly, initialValue = null)

    init {
        observeAddress()
        subscribePolling()
        sendScreenOpened()
    }

    fun onResume() {
        isResumed.value = true
    }

    fun onPause() {
        isResumed.value = false
    }

    private fun observeAddress() {
        jointAccount
            .map { account -> account?.safeAddress }
            .distinctUntilChanged()
            .onEach { address ->
                uiState.update { it.copy(address = address, shareMessage = address?.let(::shareMessage)) }
            }
            .launchIn(modelScope)
    }

    /** The event carries the account's size, so it waits for the account to be read */
    private fun sendScreenOpened() {
        modelScope.launch {
            val account = jointAccount.filterNotNull().first()
            analyticsEventHandler.send(
                JointAccountAnalyticsEvent.ConfirmAddressScreenOpened(
                    membersCount = account.membersCount,
                    threshold = account.threshold,
                ),
            )
        }
    }

    private fun subscribePolling() {
        isResumed
            .flatMapLatest { resumed ->
                if (resumed) pollingCoordinator.poll(walletId = params.userWalletId) else emptyFlow()
            }
            .launchIn(modelScope)
    }

    private fun shareMessage(address: String) = resourceReference(
        id = CoreUiR.string.joint_account_confirm_address_share_message,
        formatArgs = wrappedList(address),
    )

    private fun onShareClick(message: String) {
        shareManager.shareText(text = message)
    }

    private fun onMismatchClick() {
        if (uiState.value.isConfirming) return

        jointAccount.value?.let { account ->
            analyticsEventHandler.send(
                JointAccountAnalyticsEvent.ButtonAddressDoesntMatch(
                    membersCount = account.membersCount,
                    threshold = account.threshold,
                ),
            )
        }
        modelScope.launch { openSupportChat() }
    }

    private suspend fun openSupportChat() {
        val metaInfo = getWalletMetaInfoUseCase(params.userWalletId).getOrNull() ?: return
        if (uiState.value.isConfirming) return

        router.push(AppRoute.Usedesk(walletMetaInfo = metaInfo))
    }

    private fun onConfirmClick() {
        if (uiState.value.isConfirming) return

        uiState.update { it.copy(isConfirming = true) }
        modelScope.launch {
            try {
                confirm()
            } finally {
                uiState.update { it.copy(isConfirming = false) }
            }
        }
    }

    private suspend fun confirm() {
        val account = findAccount()

        if (account == null) {
            TangemLogger.e("Joint account confirmation failed: no account behind the shown address")
            showError(JointAccountError(source = ErrorSource.Confirm, errorMessage = "No account"))
            return
        }

        if (!ensureWalletLinked()) return

        confirmJointAccountUseCase(account = account)
            .onLeft(::onConfirmError)
            .onRight(::onConfirmed)
    }

    private suspend fun ensureWalletLinked(): Boolean {
        val userWallet = getUserWalletUseCase(params.userWalletId).getOrNull() ?: return true

        return ensureWalletLinkedUseCase(userWallet)
    }

    private suspend fun findAccount(): Account.Joint? {
        val accountId = AccountId
            .forJointAccount(userWalletId = params.userWalletId, value = params.cryptoAccountId)
            .getOrNull()
            ?: return null

        return withTimeoutOrNull(ACCOUNT_LOOKUP_TIMEOUT) {
            singleAccountSupplier.filterJointAccount(accountId = accountId).firstOrNull()
        }
    }

    /** Only the last signature activates the account; until then the members screen shows who is still due */
    private fun onConfirmed(account: JointAccount) {
        val isActivated = account.status == JointAccount.Status.ACTIVE
        analyticsEventHandler.send(
            JointAccountAnalyticsEvent.AccountConfirmed(
                membersCount = account.membersCount,
                threshold = account.threshold,
                isAccountActivated = isActivated,
            ),
        )
        if (isActivated) {
            params.onActivated()
        } else {
            router.pop()
        }
    }

    private fun onConfirmError(error: JointAccountConfirmationError) {
        when (error) {
            JointAccountFlowError.UserCancelled -> Unit
            JointAccountConfirmationError.AccountNotFound,
            JointAccountConfirmationError.NotReadyForConfirmation,
            JointAccountConfirmationError.AddressMismatch,
            JointAccountConfirmationError.NotMember,
            -> showError(JointAccountError(source = ErrorSource.Confirm, errorMessage = error.toString()))
            is JointAccountFlowError.Failed -> {
                showError(JointAccountError(source = ErrorSource.Confirm, cause = error.cause))
            }
        }
    }

    private fun showError(event: JointAccountError) {
        analyticsEventHandler.send(event)
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(CoreUiR.string.common_error),
                message = resourceReference(CoreUiR.string.common_something_went_wrong),
            ),
        )
    }

    private companion object {
        val ACCOUNT_LOOKUP_TIMEOUT = 3.seconds
    }
}