package com.tangem.features.jointaccount.main.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.ui.userwallet.ext.walletInterationIcon
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.message.GlobalLoadingMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.core.ui.message.ToastMessage
import com.tangem.domain.account.status.usecase.ArchiveCryptoPortfolioUseCase
import com.tangem.domain.account.supplier.SingleAccountSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncOrNull
import com.tangem.domain.jointaccount.address.JointOwnerAddressResolver
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointAccountActivationError
import com.tangem.domain.jointaccount.model.JointAccountFlowError
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.jointaccount.polling.JointAccountPollingCoordinator
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.usecase.ActivateJointAccountUseCase
import com.tangem.domain.jointaccount.usecase.GetJointAccountInvitesUseCase
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.features.jointaccount.deeplink.JointAccountInviteLinkBuilder
import com.tangem.features.jointaccount.main.JointAccountMembersComponent
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.features.jointaccount.main.converter.JointAccountToMembersUMConverter
import com.tangem.features.jointaccount.main.entity.ActivationConfirmationConfig
import com.tangem.features.jointaccount.main.entity.MemberCardConfig
import com.tangem.features.jointaccount.main.state.JointAccountMembersStateController
import com.tangem.features.jointaccount.main.state.transformers.*
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import com.tangem.core.ui.R as CoreUiR

@OptIn(ExperimentalCoroutinesApi::class)
@Stable
@ModelScoped
@Suppress("LongParameterList")
internal class JointAccountMembersModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val clipboardManager: ClipboardManager,
    private val uiMessageSender: UiMessageSender,
    private val shareManager: ShareManager,
    private val jointAccountListSupplier: SingleJointAccountListSupplier,
    private val jointAccountListFetcher: SingleJointAccountListFetcher,
    private val pollingCoordinator: JointAccountPollingCoordinator,
    private val singleAccountSupplier: SingleAccountSupplier,
    private val getJointAccountInvitesUseCase: GetJointAccountInvitesUseCase,
    private val ownerAddressResolver: JointOwnerAddressResolver,
    private val activateJointAccountUseCase: ActivateJointAccountUseCase,
    private val archiveCryptoPortfolioUseCase: ArchiveCryptoPortfolioUseCase,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val stateController: JointAccountMembersStateController,
) : Model() {

    private var activationJob: Job? = null

    private val params = paramsContainer.require<JointAccountMembersComponent.Params>()

    private val converterCallbacks = JointAccountToMembersUMConverter.Callbacks(
        onMemberInfoClick = { avatar, name, address ->
            onMemberInfoClick(MemberCardConfig(avatar = avatar, name = name, address = address))
        },
        onInviteClick = ::shareInvite,
        onActivateClick = ::onActivateClick,
        onShareSafelyClick = ::onShareSafelyClick,
        onArchiveClick = ::onArchiveClick,
        onCloseClick = ::onCloseClick,
    )

    val bottomSheetNavigation: SlotNavigation<MemberCardConfig> = SlotNavigation()
    val shareSafelyNavigation: SlotNavigation<Unit> = SlotNavigation()
    val activationConfirmationNavigation: SlotNavigation<ActivationConfirmationConfig> = SlotNavigation()

    val isConfirmingActivation: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val uiState: StateFlow<JointAccountMembersUM>
        get() = stateController.uiState

    /** The account a currently open [activationConfirmationNavigation] sheet, if any, was opened for. */
    private var confirmedAccountId: String? = null

    private val isResumed = MutableStateFlow(false)

    private val resolvedAccountFlow: Flow<JointAccount?> = jointAccountListSupplier(userWalletId = params.userWalletId)
        .map { accounts ->
            accounts.firstOrNull { params.cryptoAccountId == null || it.cryptoAccountId == params.cryptoAccountId }
        }
        .distinctUntilChanged()

    init {
        stateController.update(UpdateMembersInitialStateTransformer(onCloseClick = ::onCloseClick))
        observeAccount()
        subscribePolling()
        refreshAccounts()
    }

    fun onResume() {
        isResumed.value = true
    }

    fun onPause() {
        isResumed.value = false
    }

    private fun refreshAccounts() {
        modelScope.launch {
            jointAccountListFetcher(
                params = SingleJointAccountListFetcher.Params(userWalletId = params.userWalletId),
            )
        }
    }

    private fun observeAccount() {
        resolvedAccountFlow
            .flatMapLatest { account ->
                if (account == null) return@flatMapLatest flowOf(null)

                val invites = getJointAccountInvitesUseCase(
                    userWalletId = params.userWalletId,
                    cryptoAccountId = account.cryptoAccountId,
                )

                ownAddressFlow(cryptoAccountId = account.cryptoAccountId).map { ownAddress ->
                    ResolvedAccount(account = account, invites = invites, ownAddress = ownAddress)
                }
            }
            .onEach { resolved ->
                dismissStaleActivationConfirmation(resolvedAccountId = resolved?.account?.cryptoAccountId)

                val transformer = resolved
                    ?.let { (account, invites, ownAddress) ->
                        UpdateMembersAccountTransformer(
                            account = account,
                            invites = invites,
                            ownAddress = ownAddress,
                            callbacks = converterCallbacks,
                        )
                    }
                    ?: ClearMembersAccountTransformer()

                stateController.update(transformer)
            }
            .launchIn(modelScope)
    }

    /**
     * The wallet's own owner address in the account, derived from the key index its `/accounts` record carries.
     * Starts with null so the screen renders before that record arrives (the create and join flows refresh the two
     * lists concurrently) and settles once it does; the key is derived once per index.
     */
    private fun ownAddressFlow(cryptoAccountId: String): Flow<String?> {
        val accountId = AccountId
            .forJointAccount(userWalletId = params.userWalletId, value = cryptoAccountId)
            .getOrNull()
            ?: return flowOf(null)

        return singleAccountSupplier.filterJointAccount(accountId = accountId)
            .map { account -> account.ownerKeyIndex }
            .distinctUntilChanged()
            .map { ownerKeyIndex ->
                ownerAddressResolver
                    .resolve(userWalletId = params.userWalletId, ownerKeyIndex = ownerKeyIndex)
                    .onLeft { TangemLogger.e("Unable to resolve the own owner address of the joint account", it) }
                    .getOrNull()
            }
            .onStart { emit(null) }
    }

    /**
     * The backend won't silent-push joint-account state changes (a slot filling, activation, an operation awaiting
     * a co-signature), so while this screen is on top a 60-second poll keeps it fresh — the current status can't
     * tell us whether it's stale, so it isn't used to gate this, and neither is whether an account is currently
     * resolved: an empty store (offline, cold start, a failed [refreshAccounts]) must still poll, or it could
     * never recover on its own. [pollingCoordinator] paces itself — this only decides whether to collect.
     */
    private fun subscribePolling() {
        isResumed
            .flatMapLatest { resumed ->
                if (resumed) pollingCoordinator.poll(walletId = params.userWalletId) else emptyFlow()
            }
            .launchIn(modelScope)
    }

    /**
     * A confirmation sheet only applies to the account it was opened for — if a fresh emission resolves to
     * a different account (or none), its onConfirmClick would activate the wrong one, so the sheet is
     * dropped. `params.cryptoAccountId == null` is the only case where the resolved account can change
     * identity between emissions (legacy nav state falling back to "the wallet's first joint account").
     */
    private fun dismissStaleActivationConfirmation(resolvedAccountId: String?) {
        val openConfirmationId = confirmedAccountId ?: return

        if (openConfirmationId != resolvedAccountId) {
            confirmedAccountId = null
            isConfirmingActivation.value = false
            activationConfirmationNavigation.dismiss()
        }
    }

    /** The slot's own invite: a link issued for one slot cannot be redeemed for another */
    private fun shareInvite(inviteId: String) {
        shareManager.shareText(text = JointAccountInviteLinkBuilder.build(inviteId = inviteId))
    }

    private fun onActivateClick(cryptoAccountId: String) {
        val userWallet = userWalletsListRepository.getSyncOrNull(id = params.userWalletId)

        confirmedAccountId = cryptoAccountId
        isConfirmingActivation.value = false
        activationConfirmationNavigation.activate(
            ActivationConfirmationConfig(
                cryptoAccountId = cryptoAccountId,
                walletInteractionIconRes = userWallet?.let(::walletInterationIcon),
            ),
        )
    }

    fun onConfirmActivationClick(cryptoAccountId: String) {
        // The loader shows even when a signing is already in flight — a re-opened sheet joins the running
        // attempt instead of silently ignoring the tap; its completion closes the sheet either way
        isConfirmingActivation.value = true

        if (activationJob?.isActive == true) return

        activationJob = modelScope.launch {
            val account = findAccount(cryptoAccountId = cryptoAccountId)

            if (account == null) {
                TangemLogger.e("Joint account activation failed: no account behind the shown members list")
                activationConfirmationNavigation.dismiss()
                showActivationError()
                return@launch
            }

            val result = activateJointAccountUseCase(account = account)
            activationConfirmationNavigation.dismiss()
            result
                .onLeft(::onActivationError)
                .onRight { onActivated() }
        }
    }

    private fun onActivated() {
        uiMessageSender.send(
            SnackbarMessage(
                message = resourceReference(CoreUiR.string.joint_account_activated_snackbar),
                startIconId = CoreUiR.drawable.ic_success_20,
                isIconTinted = false,
            ),
        )
        router.pop()
    }

    private suspend fun findAccount(cryptoAccountId: String): Account.Joint? {
        val accountId = AccountId
            .forJointAccount(userWalletId = params.userWalletId, value = cryptoAccountId)
            .getOrNull()
            ?: return null

        return withTimeoutOrNull(ACCOUNT_LOOKUP_TIMEOUT_MILLIS.milliseconds) {
            singleAccountSupplier.filterJointAccount(accountId = accountId).firstOrNull()
        }
    }

    private fun onActivationError(error: JointAccountActivationError) {
        when (error) {
            // Dismissing the signing is not a failure: the screen stays as it was
            JointAccountFlowError.UserCancelled -> Unit
            JointAccountActivationError.AccountNotFound,
            JointAccountActivationError.NotReadyForActivation,
            JointAccountActivationError.AddressMismatch,
            JointAccountActivationError.NotCreator,
            is JointAccountFlowError.Failed,
            -> showActivationError()
        }
    }

    private fun showActivationError() {
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(CoreUiR.string.common_error),
                message = resourceReference(CoreUiR.string.common_something_went_wrong),
            ),
        )
    }

    private fun onShareSafelyClick() {
        shareSafelyNavigation.activate(Unit)
    }

    private fun onMemberInfoClick(config: MemberCardConfig) {
        bottomSheetNavigation.activate(config)
    }

    fun onCopyAddressClick(address: String) {
        clipboardManager.setText(text = address, isSensitive = false)
        uiMessageSender.send(
            SnackbarMessage(
                message = resourceReference(CoreUiR.string.wallet_notification_address_copied),
                startIconId = CoreUiR.drawable.ic_check_24,
            ),
        )
    }

    private fun onArchiveClick(cryptoAccountId: String) {
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(CoreUiR.string.account_details_archive),
                message = resourceReference(CoreUiR.string.account_details_archive_joint_description),
                firstActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(CoreUiR.string.account_details_archive_action),
                        isWarning = true,
                        onClick = { archiveAccount(cryptoAccountId) },
                    )
                },
                secondActionBuilder = { cancelAction() },
            ),
        )
    }

    private fun archiveAccount(cryptoAccountId: String) {
        modelScope.launch {
            uiMessageSender.send(GlobalLoadingMessage(isShow = true))

            AccountId.forJointAccount(userWalletId = params.userWalletId, value = cryptoAccountId).fold(
                ifLeft = { failedArchiveDialog(error = null) },
                ifRight = { accountId ->
                    archiveCryptoPortfolioUseCase(accountId)
                        .onLeft { error -> failedArchiveDialog(error = error) }
                        .onRight {
                            val toast = ToastMessage(
                                message = resourceReference(CoreUiR.string.account_archive_success_message),
                            )
                            uiMessageSender.send(toast)
                            router.pop()
                        }
                },
            )

            uiMessageSender.send(GlobalLoadingMessage(isShow = false))
        }
    }

    private fun failedArchiveDialog(error: ArchiveCryptoPortfolioUseCase.Error?) {
        val titleRes: Int
        val messageRes: Int
        if (error is ArchiveCryptoPortfolioUseCase.Error.ActiveReferralStatus) {
            titleRes = CoreUiR.string.account_could_not_archive_referral_program_title
            messageRes = CoreUiR.string.account_could_not_archive_referral_program_message
        } else {
            titleRes = CoreUiR.string.common_something_went_wrong
            messageRes = CoreUiR.string.account_generic_error_dialog_message
        }

        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(titleRes),
                message = resourceReference(messageRes),
            ),
        )
    }

    private fun onCloseClick() {
        router.pop()
    }

    private data class ResolvedAccount(val account: JointAccount, val invites: List<String>, val ownAddress: String?)

    private companion object {
        const val ACCOUNT_LOOKUP_TIMEOUT_MILLIS = 3_000L
    }
}