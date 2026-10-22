package com.tangem.features.jointaccount.confirmaddress.model

import androidx.compose.runtime.Stable
import com.tangem.common.ui.userwallet.ext.walletInterationIcon
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.account.supplier.SingleAccountSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncOrNull
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.jointaccount.model.JointAccountActivationError
import com.tangem.domain.jointaccount.model.JointAccountFlowError
import com.tangem.domain.jointaccount.polling.JointAccountPollingCoordinator
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.usecase.ActivateJointAccountUseCase
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.features.jointaccount.confirmaddress.JointAccountConfirmAddressComponent
import com.tangem.features.jointaccount.confirmaddress.ui.state.JointAccountConfirmAddressUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
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
    private val activateJointAccountUseCase: ActivateJointAccountUseCase,
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val pollingCoordinator: JointAccountPollingCoordinator,
) : Model() {

    private val params = paramsContainer.require<JointAccountConfirmAddressComponent.Params>()

    val uiState: StateFlow<JointAccountConfirmAddressUM>
        field = MutableStateFlow(
            JointAccountConfirmAddressUM(
                address = null,
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

    init {
        observeAddress()
        subscribePolling()
    }

    fun onResume() {
        isResumed.value = true
    }

    fun onPause() {
        isResumed.value = false
    }

    private fun observeAddress() {
        jointAccountListSupplier(userWalletId = params.userWalletId)
            .map { accounts -> accounts.firstOrNull { it.cryptoAccountId == params.cryptoAccountId }?.safeAddress }
            .distinctUntilChanged()
            .onEach { address -> uiState.update { it.copy(address = address) } }
            .launchIn(modelScope)
    }

    private fun subscribePolling() {
        isResumed
            .flatMapLatest { resumed ->
                if (resumed) pollingCoordinator.poll(walletId = params.userWalletId) else emptyFlow()
            }
            .launchIn(modelScope)
    }

    private fun onShareClick() {
        val address = uiState.value.address ?: return

        shareManager.shareText(text = address)
    }

    private fun onMismatchClick() {
        modelScope.launch {
            val metaInfo = getWalletMetaInfoUseCase(params.userWalletId).getOrNull() ?: return@launch

            sendFeedbackEmailUseCase(type = FeedbackEmailType.DirectUserRequest(walletMetaInfo = metaInfo))
        }
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
            showError()
            return
        }

        activateJointAccountUseCase(account = account)
            .onLeft(::onConfirmError)
            .onRight { onConfirmed() }
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

    private fun onConfirmed() {
        uiMessageSender.send(
            SnackbarMessage(
                message = resourceReference(CoreUiR.string.joint_account_activated_snackbar),
                startIconId = CoreUiR.drawable.ic_success_20,
                isIconTinted = false,
            ),
        )
        router.pop()
    }

    private fun onConfirmError(error: JointAccountActivationError) {
        when (error) {
            JointAccountFlowError.UserCancelled -> Unit
            JointAccountActivationError.AccountNotFound,
            JointAccountActivationError.NotReadyForActivation,
            JointAccountActivationError.AddressMismatch,
            JointAccountActivationError.NotCreator,
            is JointAccountFlowError.Failed,
            -> showError()
        }
    }

    private fun showError() {
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