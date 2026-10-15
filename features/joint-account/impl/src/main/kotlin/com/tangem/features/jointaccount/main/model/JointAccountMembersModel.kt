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
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.account.supplier.SingleAccountSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncOrNull
import com.tangem.domain.jointaccount.address.JointOwnerAddressResolver
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointAccountActivationError
import com.tangem.domain.jointaccount.model.JointAccountFlowError
import com.tangem.domain.jointaccount.polling.JointAccountPollingCoordinator
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.usecase.ActivateJointAccountUseCase
import com.tangem.domain.jointaccount.usecase.GetJointAccountInvitesUseCase
import com.tangem.domain.jointaccount.usecase.GetSharedJointAccountInvitesUseCase
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.account.OwnerKeyIndex
import com.tangem.features.jointaccount.compromised.JointAccountCompromiseWatcher
import com.tangem.features.jointaccount.main.JointAccountMembersComponent
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.features.jointaccount.main.converter.JointAccountToMembersUMConverter
import com.tangem.features.jointaccount.main.entity.AboutSignaturesConfig
import com.tangem.features.jointaccount.main.entity.ActivationConfirmationConfig
import com.tangem.features.jointaccount.main.entity.MemberCardConfig
import com.tangem.features.jointaccount.main.state.JointAccountMembersStateController
import com.tangem.features.jointaccount.main.state.transformers.*
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.tangem.core.ui.R as CoreUiR

@OptIn(ExperimentalCoroutinesApi::class)
@Stable
@ModelScoped
@Suppress("LongParameterList", "LargeClass")
internal class JointAccountMembersModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val clipboardManager: ClipboardManager,
    private val uiMessageSender: UiMessageSender,
    private val jointAccountListSupplier: SingleJointAccountListSupplier,
    private val jointAccountListFetcher: SingleJointAccountListFetcher,
    private val pollingCoordinator: JointAccountPollingCoordinator,
    private val singleAccountSupplier: SingleAccountSupplier,
    private val getJointAccountInvitesUseCase: GetJointAccountInvitesUseCase,
    private val getSharedJointAccountInvitesUseCase: GetSharedJointAccountInvitesUseCase,
    private val ownerAddressResolver: JointOwnerAddressResolver,
    private val activateJointAccountUseCase: ActivateJointAccountUseCase,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val stateController: JointAccountMembersStateController,
    private val inviteSharing: JointAccountInviteSharing,
    private val compromiseWatcher: JointAccountCompromiseWatcher,
) : Model() {

    private var activationJob: Job? = null

    private val params = paramsContainer.require<JointAccountMembersComponent.Params>()

    private val converterCallbacks = JointAccountToMembersUMConverter.Callbacks(
        onMemberInfoClick = { avatar, name, address ->
            onMemberInfoClick(MemberCardConfig(avatar = avatar, name = name, address = address))
        },
        onInviteClick = { inviteId -> inviteSharing.share(inviteId) },
        onActivateClick = ::onActivateClick,
        onApprovalsInfoClick = { threshold, total ->
            aboutSignaturesNavigation.activate(AboutSignaturesConfig(threshold = threshold, total = total))
        },
        onCloseClick = ::onCloseClick,
    )

    val bottomSheetNavigation: SlotNavigation<MemberCardConfig> = SlotNavigation()
    val activationConfirmationNavigation: SlotNavigation<ActivationConfirmationConfig> = SlotNavigation()
    val aboutSignaturesNavigation: SlotNavigation<AboutSignaturesConfig> = SlotNavigation()

    val isConfirmingActivation: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val uiState: StateFlow<JointAccountMembersUM>
        get() = stateController.uiState

    /** The account a currently open [activationConfirmationNavigation] sheet, if any, was opened for. */
    private var confirmedAccountId: String? = null

    private val isResumed = MutableStateFlow(false)

    private val resolvedAccountFlow: Flow<JointAccount?> = compromiseWatcher.verified(
        userWalletId = params.userWalletId,
        accounts = jointAccountListSupplier(userWalletId = params.userWalletId)
            .map { accounts ->
                accounts.firstOrNull { params.cryptoAccountId == null || it.cryptoAccountId == params.cryptoAccountId }
            }
            .distinctUntilChanged(),
        onCompromised = router::pop,
    )

    init {
        stateController.update(UpdateMembersInitialStateTransformer(onCloseClick = ::onCloseClick))
        observeAccount()
        recordSharedInvites()
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
                combine(
                    flow = ownJointAccountFlow(cryptoAccountId = account.cryptoAccountId),
                    flow2 = getSharedJointAccountInvitesUseCase(
                        userWalletId = params.userWalletId,
                        cryptoAccountId = account.cryptoAccountId,
                    ),
                    flow3 = isConfirmingActivation,
                ) { ownAccount, shared, isActivating ->
                    ResolvedAccount(
                        account = account,
                        invites = invites,
                        sharedInvites = shared,
                        ownAddress = ownAccount?.ownAddress,
                        isActivating = isActivating,
                        name = ownAccount?.name,
                        icon = ownAccount?.icon,
                    )
                }
            }
            .onEach { resolved ->
                dismissStaleActivationConfirmation(resolvedAccountId = resolved?.account?.cryptoAccountId)

                val walletIconRes = userWalletsListRepository
                    .getSyncOrNull(id = params.userWalletId)
                    ?.let(::walletInterationIcon)
                val transformer = resolved
                    ?.let { account ->
                        UpdateMembersAccountTransformer(
                            account = account.account,
                            invites = account.invites,
                            sharedInvites = account.sharedInvites,
                            ownAddress = account.ownAddress,
                            callbacks = converterCallbacks,
                            walletIconRes = walletIconRes,
                            isActivating = account.isActivating,
                            accountName = account.name,
                            accountIcon = account.icon,
                        )
                    }
                    ?: ClearMembersAccountTransformer()

                stateController.update(transformer)
            }
            .launchIn(modelScope)
    }

    /**
     * The account's `/accounts` record as this wallet sees it: its name, icon and the wallet's own owner address,
     * derived from the key index the record carries. Starts with null so the screen renders before that record
     * arrives (the create and join flows refresh the two lists concurrently) and settles once it does; the key is
     * derived once per index.
     */
    private fun ownJointAccountFlow(cryptoAccountId: String): Flow<OwnJointAccount?> {
        val accountId = AccountId
            .forJointAccount(userWalletId = params.userWalletId, value = cryptoAccountId)
            .getOrNull()
            ?: return flowOf(null)

        return singleAccountSupplier.filterJointAccount(accountId = accountId)
            .map { account ->
                OwnJointAccount(
                    ownerKeyIndex = account.ownerKeyIndex,
                    name = account.accountName,
                    icon = account.icon,
                    ownAddress = null,
                )
            }
            .distinctUntilChanged()
            .runningFold<OwnJointAccount, OwnJointAccount?>(initial = null) { previous, next ->
                val ownAddress = if (previous?.ownerKeyIndex == next.ownerKeyIndex) {
                    previous.ownAddress
                } else {
                    resolveOwnAddress(ownerKeyIndex = next.ownerKeyIndex)
                }
                next.copy(ownAddress = ownAddress)
            }
    }

    private suspend fun resolveOwnAddress(ownerKeyIndex: OwnerKeyIndex): String? = ownerAddressResolver
        .resolve(userWalletId = params.userWalletId, ownerKeyIndex = ownerKeyIndex)
        .onLeft { TangemLogger.e("Unable to resolve the own owner address of the joint account", it) }
        .getOrNull()

    /**
     * The backend won't silent-push joint-account state changes (a slot filling, activation, an operation awaiting
     * a co-signature), so while this screen is on top a 5-second poll keeps it fresh — the current status can't
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

    private fun recordSharedInvites() {
        inviteSharing
            .recordShared(userWalletId = params.userWalletId, account = resolvedAccountFlow)
            .launchIn(modelScope)
    }

    private fun onActivateClick(cryptoAccountId: String) {
        if (!IS_ACTIVATION_CONFIRMATION_SHOWN) {
            onConfirmActivationClick(cryptoAccountId = cryptoAccountId)
            return
        }

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
                isConfirmingActivation.value = false
                activationConfirmationNavigation.dismiss()
                showActivationError()
                return@launch
            }

            val result = activateJointAccountUseCase(account = account)
            activationConfirmationNavigation.dismiss()
            result
                .onLeft { error ->
                    // On success the loader stays until the activated account takes the button away
                    isConfirmingActivation.value = false
                    onActivationError(error)
                }
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

    private fun onCloseClick() {
        router.pop()
    }

    private data class ResolvedAccount(
        val account: JointAccount,
        val invites: List<String>,
        val sharedInvites: Set<String>,
        val ownAddress: String?,
        val isActivating: Boolean,
        val name: AccountName?,
        val icon: CryptoPortfolioIcon?,
    )

    private data class OwnJointAccount(
        val ownerKeyIndex: OwnerKeyIndex,
        val name: AccountName,
        val icon: CryptoPortfolioIcon,
        val ownAddress: String?,
    )

    private companion object {
        const val ACCOUNT_LOOKUP_TIMEOUT_MILLIS = 3_000L

        /** The activation sheet is left out for now, activation starts at once; the step comes back by flipping this */
        const val IS_ACTIVATION_CONFIRMATION_SHOWN = false
    }
}