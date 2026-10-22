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
import com.tangem.features.jointaccount.main.state.transformers.ClearMembersAccountTransformer
import com.tangem.features.jointaccount.main.state.transformers.UpdateMembersAccountTransformer
import com.tangem.features.jointaccount.main.state.transformers.UpdateMembersInitialStateTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.launch
import javax.inject.Inject
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

    val uiState: StateFlow<JointAccountMembersUM>
        get() = stateController.uiState

    val bottomSheetNavigation: SlotNavigation<JointAccountMembersSheetConfig> = SlotNavigation()

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

    private val converterCallbacks = JointAccountToMembersUMConverter.Callbacks(
        onMemberInfoClick = ::onMemberInfoClick,
        onInviteClick = ::onInviteClick,
        onConfirmAddressClick = ::onConfirmAddressClick,
        onApprovalsInfoClick = ::onApprovalsInfoClick,
        onCloseClick = ::onCloseClick,
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

    // region Subscriptions
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
            .onEach { resolved -> stateController.update(resolved.toTransformer()) }
            .launchIn(modelScope)
    }

    private fun ResolvedAccount?.toTransformer() = if (this == null) {
        ClearMembersAccountTransformer()
    } else {
        UpdateMembersAccountTransformer(
            account = account,
            invites = invites,
            sharedInvites = sharedInvites,
            ownAddress = ownAddress,
            callbacks = converterCallbacks,
            accountName = name,
            accountIcon = icon,
        )
    }

    /**
     * The account's `/accounts` record as this wallet sees it. Starts with `null` so the screen renders before the
     * record arrives (the create and join flows refresh the two lists concurrently); the own address is derived
     * once per key index.
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

    private fun recordSharedInvites() {
        inviteSharing
            .recordShared(userWalletId = params.userWalletId, account = resolvedAccountFlow)
            .launchIn(modelScope)
    }

    /**
     * The backend does not push joint-account changes, so the screen polls while on top. An empty store (offline,
     * cold start, a failed refresh) must poll too, or it could never recover; [pollingCoordinator] paces itself.
     */
    private fun subscribePolling() {
        isResumed
            .flatMapLatest { resumed ->
                if (resumed) pollingCoordinator.poll(walletId = params.userWalletId) else emptyFlow()
            }
            .launchIn(modelScope)
    }

    private fun refreshAccounts() {
        modelScope.launch {
            jointAccountListFetcher(
                params = SingleJointAccountListFetcher.Params(userWalletId = params.userWalletId),
            )
        }
    }
    // endregion

    // region Clicks
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

    private fun onMemberInfoClick(avatar: JointAccountMembersUM.MemberAvatarUM, name: TextReference, address: String) {
        bottomSheetNavigation.activate(
            JointAccountMembersSheetConfig.MemberCard(
                card = MemberCardConfig(avatar = avatar, name = name, address = address),
            ),
        )
    }

    private fun onInviteClick(inviteId: String, accountName: TextReference) {
        bottomSheetNavigation.activate(
            JointAccountMembersSheetConfig.InviteMember(
                message = inviteSharing.message(inviteId = inviteId, accountName = accountName),
            ),
        )
    }

    private fun onConfirmAddressClick(cryptoAccountId: String) {
        router.push(JointAccountMembersRoute.ConfirmAddress(cryptoAccountId = cryptoAccountId))
    }

    private fun onApprovalsInfoClick(threshold: Int, total: Int) {
        bottomSheetNavigation.activate(
            JointAccountMembersSheetConfig.AboutSignatures(threshold = threshold, total = total),
        )
    }

    private fun onCloseClick() {
        params.onCloseFlow()
    }
    // endregion

    // region Models
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
    // endregion
}