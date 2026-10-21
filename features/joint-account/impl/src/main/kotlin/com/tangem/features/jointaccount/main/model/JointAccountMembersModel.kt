package com.tangem.features.jointaccount.main.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.account.supplier.SingleAccountSupplier
import com.tangem.domain.jointaccount.address.JointOwnerAddressResolver
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointAccountInvite
import com.tangem.domain.jointaccount.polling.JointAccountPollingCoordinator
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.usecase.GetJointAccountInvitesUseCase
import com.tangem.domain.jointaccount.usecase.GetSharedJointAccountInvitesUseCase
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.account.OwnerKeyIndex
import com.tangem.features.jointaccount.compromised.JointAccountCompromiseWatcher
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.features.jointaccount.main.converter.JointAccountToMembersUMConverter
import com.tangem.features.jointaccount.main.entity.JointAccountMembersSheetConfig
import com.tangem.features.jointaccount.main.entity.MemberCardConfig
import com.tangem.features.jointaccount.main.navigation.JointAccountMembersRoute
import com.tangem.features.jointaccount.main.state.JointAccountMembersStateController
import com.tangem.features.jointaccount.main.state.transformers.*
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.tangem.core.ui.R as CoreUiR

@OptIn(ExperimentalCoroutinesApi::class)
@Stable
@ModelScoped
@Suppress("LongParameterList")
internal class JointAccountMembersModel @Inject constructor(
    paramsContainer: ParamsContainer,
    compromiseWatcher: JointAccountCompromiseWatcher,
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
    private val stateController: JointAccountMembersStateController,
    private val inviteSharing: JointAccountInviteSharing,
) : Model() {

    private val params = paramsContainer.require<JointAccountMembersListParams>()

    val bottomSheetNavigation: SlotNavigation<JointAccountMembersSheetConfig> = SlotNavigation()

    private val converterCallbacks = JointAccountToMembersUMConverter.Callbacks(
        onMemberInfoClick = { avatar, name, address ->
            bottomSheetNavigation.activate(
                JointAccountMembersSheetConfig.MemberCard(
                    card = MemberCardConfig(avatar = avatar, name = name, address = address),
                ),
            )
        },
        onInviteClick = ::onInviteClick,
        onConfirmAddressClick = { cryptoAccountId ->
            router.push(JointAccountMembersRoute.ConfirmAddress(cryptoAccountId = cryptoAccountId))
        },
        onApprovalsInfoClick = { threshold, total ->
            bottomSheetNavigation.activate(
                JointAccountMembersSheetConfig.AboutSignatures(threshold = threshold, total = total),
            )
        },
        onCloseClick = ::onCloseClick,
    )

    val uiState: StateFlow<JointAccountMembersUM>
        get() = stateController.uiState

    private val isResumed = MutableStateFlow(false)

    private val resolvedAccountFlow: Flow<JointAccount?> = compromiseWatcher.verified(
        userWalletId = params.userWalletId,
        accounts = jointAccountListSupplier(userWalletId = params.userWalletId)
            .map { accounts ->
                accounts.firstOrNull { params.cryptoAccountId == null || it.cryptoAccountId == params.cryptoAccountId }
            }
            .distinctUntilChanged(),
        onCompromised = params.onCloseFlow,
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
                ) { ownAccount, shared ->
                    ResolvedAccount(
                        account = account,
                        invites = invites,
                        sharedInvites = shared,
                        ownAddress = ownAccount?.ownAddress,
                        name = ownAccount?.name,
                        icon = ownAccount?.icon,
                    )
                }
            }
            .onEach { resolved ->
                val transformer = resolved
                    ?.let { account ->
                        UpdateMembersAccountTransformer(
                            account = account.account,
                            invites = account.invites,
                            sharedInvites = account.sharedInvites,
                            ownAddress = account.ownAddress,
                            callbacks = converterCallbacks,
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

    private fun recordSharedInvites() {
        inviteSharing
            .recordShared(userWalletId = params.userWalletId, account = resolvedAccountFlow)
            .launchIn(modelScope)
    }

    private fun onInviteClick(inviteId: String, accountName: TextReference) {
        bottomSheetNavigation.activate(
            JointAccountMembersSheetConfig.InviteMember(
                message = inviteSharing.message(inviteId = inviteId, accountName = accountName),
            ),
        )
    }

    fun onShareInviteClick(message: String) {
        inviteSharing.share(message = message)
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
        params.onCloseFlow()
    }

    private data class ResolvedAccount(
        val account: JointAccount,
        val invites: List<JointAccountInvite>,
        val sharedInvites: Set<String>,
        val ownAddress: String?,
        val name: AccountName?,
        val icon: CryptoPortfolioIcon?,
    )

    private data class OwnJointAccount(
        val ownerKeyIndex: OwnerKeyIndex,
        val name: AccountName,
        val icon: CryptoPortfolioIcon,
        val ownAddress: String?,
    )
}