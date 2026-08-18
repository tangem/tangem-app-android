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
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isLocked
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.features.jointaccount.join.confirmation.ui.state.JointAccountJoinConfirmationUM
import com.tangem.features.jointaccount.join.invitepreview.state.JointAccountInvitePreviewStateController
import com.tangem.features.jointaccount.join.invitepreview.state.transformers.SetJoinConfirmationTransformer
import com.tangem.features.jointaccount.join.invitepreview.state.transformers.UpdateInvitePreviewInitialStateTransformer
import com.tangem.features.jointaccount.join.invitepreview.state.transformers.UpdateInvitePreviewWalletsTransformer
import com.tangem.features.jointaccount.join.invitepreview.ui.state.JointAccountInvitePreviewUM
import com.tangem.features.jointaccount.join.model.JointAccountJoinChildParams
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.features.jointaccount.main.entity.MemberCardConfig
import com.tangem.features.jointaccount.join.navigation.JointAccountJoinRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
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
    userWalletsListRepository: UserWalletsListRepository,
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

    private val unlockedWallets: Flow<List<UserWallet>> = userWalletsListRepository.userWallets
        .map { wallets -> wallets.orEmpty().filterNot(UserWallet::isLocked) }

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

    init {
        updateInitialState()
        observeWallets()
        dismissSelectorOnPick()
    }

    private fun updateInitialState() {
        stateController.update(
            UpdateInvitePreviewInitialStateTransformer(
                onCreatorInfoClick = ::onCreatorInfoClick,
                onContinueClick = ::onContinueClick,
                onCloseClick = ::onCloseClick,
            ),
        )
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
        val state = uiState.value

        memberCardNavigation.activate(
            MemberCardConfig(
                avatar = JointAccountMembersUM.MemberAvatarUM(
                    monogram = state.creatorName.take(1).uppercase(),
                    color = state.accountIcon.color,
                ),
                name = stringReference(state.creatorName),
                address = STUB_CREATOR_ADDRESS,
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

    private companion object {
        // TODO: replace the stubbed creator address once the domain integration lands
        const val STUB_CREATOR_ADDRESS = "0xBef7B368aac4e6752A9cE0xBef7B36A9cE"
    }
}