package com.tangem.features.jointaccount.join.invitepreview.model

import androidx.compose.runtime.Stable
import arrow.core.Either
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.router.slot.navigate
import com.tangem.core.analytics.api.AnalyticsEventHandler
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
import com.tangem.domain.jointaccount.model.JointAccountInvitePreview
import com.tangem.domain.jointaccount.model.JointAccountInvitePreviewError
import com.tangem.domain.jointaccount.usecase.GetJointAccountInvitePreviewUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent.ErrorSource
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent.JointAccountError
import com.tangem.features.jointaccount.join.invitepreview.entity.JointAccountInvitePreviewSheetConfig
import com.tangem.features.jointaccount.join.invitepreview.state.JointAccountInvitePreviewStateController
import com.tangem.features.jointaccount.join.invitepreview.state.transformers.SetContentTransformer
import com.tangem.features.jointaccount.join.invitepreview.state.transformers.SetFailedTransformer
import com.tangem.features.jointaccount.join.invitepreview.state.transformers.SetInvalidLinkTransformer
import com.tangem.features.jointaccount.join.invitepreview.state.transformers.SetLoadingTransformer
import com.tangem.features.jointaccount.join.invitepreview.state.transformers.UpdateInvitePreviewWalletsTransformer
import com.tangem.features.jointaccount.join.invitepreview.ui.state.JointAccountInvitePreviewUM
import com.tangem.features.jointaccount.join.model.JointAccountJoinChildParams
import com.tangem.features.jointaccount.join.navigation.JointAccountJoinRoute
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.features.jointaccount.main.entity.MemberCardConfig
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import com.tangem.core.ui.R as CoreUiR

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class JointAccountInvitePreviewModel @Inject constructor(
    paramsContainer: ParamsContainer,
    portfolioFetcherFactory: PortfolioFetcher.Factory,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val stateController: JointAccountInvitePreviewStateController,
    private val getJointAccountInvitePreviewUseCase: GetJointAccountInvitePreviewUseCase,
    private val userWalletsListRepository: UserWalletsListRepository,
    val portfolioSelectorController: PortfolioSelectorController,
    private val clipboardManager: ClipboardManager,
    private val uiMessageSender: UiMessageSender,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model() {

    private val params = paramsContainer.require<JointAccountJoinChildParams>()

    val uiState: StateFlow<JointAccountInvitePreviewUM>
        get() = stateController.uiState

    val bottomSheetNavigation: SlotNavigation<JointAccountInvitePreviewSheetConfig> = SlotNavigation()

    val portfolioFetcher: PortfolioFetcher by lazy {
        portfolioFetcherFactory.create(
            mode = PortfolioFetcher.Mode.All(isOnlyMultiCurrency = false),
            scope = modelScope,
        )
    }

    val portfolioSelectorCallback = object : PortfolioSelectorComponent.BottomSheetCallback {
        override val onDismiss: () -> Unit = { bottomSheetNavigation.dismiss() }
        override val onBack: () -> Unit = { bottomSheetNavigation.dismiss() }
    }

    private val pickedWallet: StateFlow<UserWallet?> = portfolioSelectorController
        .selectedAccountWithData(portfolioFetcher)
        .map { it?.first }
        .stateIn(modelScope, SharingStarted.Eagerly, null)

    private val unlockedWallets: StateFlow<List<UserWallet>> = userWalletsListRepository.userWallets
        .map { wallets -> wallets.orEmpty().filterNot(UserWallet::isLocked) }
        .stateIn(modelScope, SharingStarted.Eagerly, emptyList())

    /** Once the selector has every unlocked wallet it stays ready: a later balance refresh must not hide the content */
    private val isSelectorReady: StateFlow<Boolean> = combine(portfolioFetcher.data, unlockedWallets) { data, wallets ->
        wallets.isNotEmpty() && wallets.all { wallet -> wallet.walletId in data.balances }
    }
        .filter { isReady -> isReady }
        .stateIn(modelScope, SharingStarted.Eagerly, false)

    /** A pick in the selector wins; otherwise the draft's wallet, then the app's selected one, then the first unlocked */
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

    private var loadJob: Job? = null

    init {
        loadForSelectedWallet()
        observeWallets()
        dismissSelectorOnPick()
    }

    // region Subscriptions
    private fun loadForSelectedWallet() {
        combine(
            userWalletsListRepository.userWallets.filterNotNull(),
            selectedWallet,
        ) { _, selected -> selected?.walletId }
            .distinctUntilChanged()
            .onEach(::load)
            .launchIn(modelScope)
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

    private fun dismissSelectorOnPick() {
        pickedWallet
            .filterNotNull()
            .onEach {
                bottomSheetNavigation.navigate { config ->
                    config.takeUnless { it is JointAccountInvitePreviewSheetConfig.WalletSelector }
                }
            }
            .launchIn(modelScope)
    }
    // endregion

    // region Loading
    private fun load(walletId: UserWalletId?) {
        loadJob?.cancel()
        loadJob = modelScope.launch {
            stateController.update(SetLoadingTransformer(onCloseClick = ::onCloseClick))

            if (walletId == null) {
                // On a cold start the wallets load before the selection derived from them: wait for it to catch up
                val hasUnlockedWallet = userWalletsListRepository.userWallets.value.orEmpty().any { !it.isLocked }
                if (hasUnlockedWallet) return@launch

                analyticsEventHandler.send(JointAccountError(source = ErrorSource.Join, errorMessage = "No wallet"))
                stateController.update(
                    SetFailedTransformer(onRetryClick = ::onRetryClick, onCloseClick = ::onCloseClick),
                )
                return@launch
            }

            loadPreview(walletId).fold(
                ifLeft = ::onLoadError,
                ifRight = { preview -> onLoaded(preview) },
            )
        }
    }

    private suspend fun loadPreview(
        walletId: UserWalletId,
    ): Either<JointAccountInvitePreviewError, JointAccountInvitePreview> {
        UNREACHABLE_RETRY_DELAYS.forEach { retryDelay ->
            val result = getJointAccountInvitePreviewUseCase(walletId = walletId, inviteId = params.inviteId)
            if (result.leftOrNull() !is JointAccountInvitePreviewError.Unreachable) return result

            delay(retryDelay)
        }

        return getJointAccountInvitePreviewUseCase(walletId = walletId, inviteId = params.inviteId)
    }

    private suspend fun onLoaded(preview: JointAccountInvitePreview) {
        params.draftHolder.setConfig(config = preview.config)
        awaitSelectorReady()
        stateController.update(
            SetContentTransformer(
                preview = preview,
                wallets = unlockedWallets.value,
                selectedWallet = selectedWallet.value,
                onRequiredApprovalsClick = ::onRequiredApprovalsClick,
                onCreatorInfoClick = ::onCreatorInfoClick,
                onContinueClick = ::onContinueClick,
                onWalletRowClick = ::onWalletRowClick,
                onCloseClick = ::onCloseClick,
            ),
        )
    }

    private fun onLoadError(error: JointAccountInvitePreviewError) {
        val cause = when (error) {
            is JointAccountInvitePreviewError.InvalidLink -> error.cause
            is JointAccountInvitePreviewError.Unreachable -> error.cause
            is JointAccountInvitePreviewError.LoadFailed -> error.cause
        }
        analyticsEventHandler.send(JointAccountError(source = ErrorSource.Join, cause = cause))

        when (error) {
            is JointAccountInvitePreviewError.InvalidLink ->
                stateController.update(SetInvalidLinkTransformer(onGotItClick = ::onCloseClick))
            is JointAccountInvitePreviewError.Unreachable,
            is JointAccountInvitePreviewError.LoadFailed,
            -> stateController.update(
                SetFailedTransformer(onRetryClick = ::onRetryClick, onCloseClick = ::onCloseClick),
            )
        }
    }

    private suspend fun awaitSelectorReady() {
        val isReady = withTimeoutOrNull(SELECTOR_READY_TIMEOUT) { isSelectorReady.first { it } }

        if (isReady == null) {
            TangemLogger.i("Joint account invite: the wallet selector data is not ready in time, showing the content")
        }
    }
    // endregion

    // region Clicks
    fun onCopyAddressClick(address: String) {
        clipboardManager.setText(text = address, isSensitive = false)
        uiMessageSender.send(
            SnackbarMessage(
                message = resourceReference(CoreUiR.string.wallet_notification_address_copied),
                startIconId = CoreUiR.drawable.ic_check_24,
            ),
        )
    }

    private fun onRetryClick() {
        load(walletId = selectedWallet.value?.walletId)
    }

    private fun onRequiredApprovalsClick() {
        val state = uiState.value as? JointAccountInvitePreviewUM.Content ?: return

        bottomSheetNavigation.activate(
            JointAccountInvitePreviewSheetConfig.AboutSignatures(
                threshold = state.requiredToSign,
                total = state.totalMembers,
            ),
        )
    }

    private fun onCreatorInfoClick() {
        val state = uiState.value as? JointAccountInvitePreviewUM.Content ?: return

        bottomSheetNavigation.activate(
            JointAccountInvitePreviewSheetConfig.CreatorCard(
                card = MemberCardConfig(
                    avatar = JointAccountMembersUM.MemberAvatarUM(
                        monogram = state.creatorName.take(1).uppercase(),
                        color = state.accountIcon.color,
                    ),
                    name = stringReference(state.creatorName),
                    address = state.creatorAddress,
                ),
            ),
        )
    }

    private fun onWalletRowClick() {
        bottomSheetNavigation.activate(JointAccountInvitePreviewSheetConfig.WalletSelector)
    }

    private fun onContinueClick() {
        val walletId = selectedWallet.value?.walletId ?: return

        params.draftHolder.setSelectedWallet(walletId)
        router.push(JointAccountJoinRoute.DisplayName)
    }

    /** Closing does not join: the slot stays free and the invitation is not consumed */
    private fun onCloseClick() {
        router.pop()
    }
    // endregion

    private companion object {
        val UNREACHABLE_RETRY_DELAYS = listOf(1.seconds, 2.seconds, 3.seconds)
        val SELECTOR_READY_TIMEOUT = 10.seconds
    }
}