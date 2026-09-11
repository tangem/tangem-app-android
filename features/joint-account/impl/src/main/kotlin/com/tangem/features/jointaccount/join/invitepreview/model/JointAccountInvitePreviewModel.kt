package com.tangem.features.jointaccount.join.invitepreview.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.jointaccount.model.JointAccountInvitePreviewError
import com.tangem.domain.jointaccount.usecase.GetJointAccountInvitePreviewUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.features.jointaccount.join.confirmation.ui.state.JointAccountJoinConfirmationUM
import com.tangem.features.jointaccount.join.invitepreview.state.JointAccountInvitePreviewStateController
import com.tangem.features.jointaccount.join.invitepreview.state.transformers.SetContentTransformer
import com.tangem.features.jointaccount.join.invitepreview.state.transformers.SetFailedTransformer
import com.tangem.features.jointaccount.join.invitepreview.state.transformers.SetInvalidLinkTransformer
import com.tangem.features.jointaccount.join.invitepreview.state.transformers.SetJoinConfirmationTransformer
import com.tangem.features.jointaccount.join.invitepreview.state.transformers.SetLoadingTransformer
import com.tangem.features.jointaccount.join.invitepreview.state.transformers.UpdateInvitePreviewWalletsTransformer
import com.tangem.features.jointaccount.join.invitepreview.ui.state.JointAccountInvitePreviewUM
import com.tangem.features.jointaccount.join.model.JointAccountJoinChildParams
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.features.jointaccount.main.entity.MemberCardConfig
import com.tangem.features.jointaccount.join.navigation.JointAccountJoinRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.tangem.core.ui.R as CoreUiR

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class JointAccountInvitePreviewModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val stateController: JointAccountInvitePreviewStateController,
    private val getJointAccountInvitePreviewUseCase: GetJointAccountInvitePreviewUseCase,
    private val userWalletsListRepository: UserWalletsListRepository,
    val portfolioSelectorController: PortfolioSelectorController,
    portfolioFetcherFactory: PortfolioFetcher.Factory,
    private val clipboardManager: ClipboardManager,
    private val uiMessageSender: UiMessageSender,
) : Model() {

    private val params = paramsContainer.require<JointAccountJoinChildParams>()

    val portfolioSelectorNavigation = SlotNavigation<Unit>()
    val memberCardNavigation: SlotNavigation<MemberCardConfig> = SlotNavigation()

    val portfolioFetcher: PortfolioFetcher by lazy {
        portfolioFetcherFactory.create(
            mode = PortfolioFetcher.Mode.All(isOnlyMultiCurrency = false),
            scope = modelScope,
        )
    }

    val portfolioSelectorCallback = object : PortfolioSelectorComponent.BottomSheetCallback {
        override val onDismiss: () -> Unit = { portfolioSelectorNavigation.dismiss() }
        override val onBack: () -> Unit = { portfolioSelectorNavigation.dismiss() }
    }

    /** The wallet picked in the selector, if any */
    private val pickedWallet: StateFlow<UserWallet?> =
        portfolioSelectorController.selectedAccountWithData(portfolioFetcher)
            .map { it?.first }
            .stateIn(modelScope, SharingStarted.Eagerly, null)

    private val unlockedWallets: StateFlow<List<UserWallet>> = userWalletsListRepository.userWallets
        .map { wallets -> wallets.orEmpty().filterNot(UserWallet::isLocked) }
        .stateIn(modelScope, SharingStarted.Eagerly, emptyList())

    /**
     * The wallet that joins the account. A pick in the selector always wins; with no pick yet it defaults to
     * the wallet saved in the draft, then to the wallet selected in the app, then to the first unlocked one.
     */
    private val selectedWallet: StateFlow<UserWallet?> = combine(
        pickedWallet,
        unlockedWallets,
        userWalletsListRepository.selectedUserWallet,
    ) { picked, wallets, currentSelected ->
        val defaultWalletId = params.draftHolder.draft.value.selectedWalletId ?: currentSelected?.walletId

        picked
            ?: wallets.firstOrNull { it.walletId == defaultWalletId }
            ?: wallets.firstOrNull()
    }.stateIn(modelScope, SharingStarted.Eagerly, null)

    val uiState: StateFlow<JointAccountInvitePreviewUM>
        get() = stateController.uiState

    private var loadJob: Job? = null

    init {
        loadForSelectedWallet()
        observeWallets()
        dismissSelectorOnPick()
    }

    private fun loadForSelectedWallet() {
        combine(
            userWalletsListRepository.userWallets.filterNotNull(),
            selectedWallet,
        ) { _, selected -> selected?.walletId }
            .distinctUntilChanged()
            .onEach(::load)
            .launchIn(modelScope)
    }

    private fun load(walletId: UserWalletId?) {
        loadJob?.cancel()
        loadJob = modelScope.launch {
            stateController.update(SetLoadingTransformer(onCloseClick = ::onCloseClick))

            if (walletId == null) {
                stateController.update(SetFailedTransformer(onRetryClick = ::retry, onCloseClick = ::onCloseClick))
                return@launch
            }

            getJointAccountInvitePreviewUseCase(walletId = walletId, inviteId = params.inviteId).fold(
                ifLeft = { error ->
                    when (error) {
                        is JointAccountInvitePreviewError.InvalidLink ->
                            stateController.update(SetInvalidLinkTransformer(onGotItClick = ::onCloseClick))
                        is JointAccountInvitePreviewError.LoadFailed ->
                            stateController.update(
                                SetFailedTransformer(onRetryClick = ::retry, onCloseClick = ::onCloseClick),
                            )
                    }
                },
                ifRight = { preview ->
                    params.draftHolder.setConfig(config = preview.config)
                    stateController.update(
                        SetContentTransformer(
                            preview = preview,
                            wallets = unlockedWallets.value,
                            selectedWallet = selectedWallet.value,
                            onCreatorInfoClick = ::onCreatorInfoClick,
                            onContinueClick = ::onContinueClick,
                            onWalletRowClick = ::onWalletRowClick,
                            onCloseClick = ::onCloseClick,
                        ),
                    )
                },
            )
        }
    }

    private fun retry() {
        load(walletId = selectedWallet.value?.walletId)
    }

    private fun observeWallets() {
        combine(unlockedWallets, selectedWallet) { wallets, selected ->
            UpdateInvitePreviewWalletsTransformer(
                wallets = wallets,
                selectedWallet = selected,
                onWalletRowClick = ::onWalletRowClick,
            )
        }
            .onEach(stateController::update)
            .launchIn(modelScope)
    }

    /** Closes the wallet selector as soon as the user picks a wallet in it */
    private fun dismissSelectorOnPick() {
        pickedWallet
            .filterNotNull()
            .onEach { portfolioSelectorNavigation.dismiss() }
            .launchIn(modelScope)
    }

    private fun onCreatorInfoClick() {
        val state = uiState.value as? JointAccountInvitePreviewUM.Content ?: return

        memberCardNavigation.activate(
            MemberCardConfig(
                avatar = JointAccountMembersUM.MemberAvatarUM(
                    monogram = state.creatorName.take(1).uppercase(),
                    color = state.accountIcon.color,
                ),
                name = stringReference(state.creatorName),
                address = state.creatorAddress,
            ),
        )
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

    private fun onWalletRowClick() {
        portfolioSelectorNavigation.activate(Unit)
    }

    private fun onContinueClick() {
        stateController.update(
            SetJoinConfirmationTransformer(
                confirmation = JointAccountJoinConfirmationUM(
                    onContinueClick = ::onConfirmationContinue,
                    onCancelClick = ::onConfirmationCancel,
                ),
            ),
        )
    }

    private fun onConfirmationContinue() {
        stateController.update(SetJoinConfirmationTransformer(confirmation = null))

        val walletId = selectedWallet.value?.walletId ?: return

        params.draftHolder.setSelectedWallet(walletId)
        router.push(JointAccountJoinRoute.DisplayName)
    }

    private fun onConfirmationCancel() {
        stateController.update(SetJoinConfirmationTransformer(confirmation = null))
    }

    /** The close icon does not join: the slot stays free and the invitation is not consumed */
    private fun onCloseClick() {
        router.pop()
    }
}