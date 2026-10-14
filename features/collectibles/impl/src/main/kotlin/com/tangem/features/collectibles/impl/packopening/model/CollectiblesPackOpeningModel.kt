package com.tangem.features.collectibles.impl.packopening.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.collectibles.account.covers
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusSupplier
import com.tangem.domain.collectibles.models.CollectibleGachaMachine
import com.tangem.domain.collectibles.models.OpenPackRequest
import com.tangem.domain.collectibles.models.OpenPackStep
import com.tangem.domain.collectibles.models.OpenedCollectiblePack
import com.tangem.domain.collectibles.usecase.GetCollectibleGachaMachineUseCase
import com.tangem.domain.collectibles.usecase.OpenCollectiblePackUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncOrNull
import com.tangem.domain.models.account.CollectiblesVaultBalance
import com.tangem.features.collectibles.impl.CollectiblesRoute
import com.tangem.features.collectibles.impl.packopening.CollectiblesConfirmOrderBottomSheetConfig
import com.tangem.features.collectibles.impl.packopening.CollectiblesPackOpeningComponent
import com.tangem.features.collectibles.impl.packopening.model.converter.CollectiblesPackOpeningConverter
import com.tangem.features.collectibles.impl.packopening.model.converter.CollectiblesRewardCardConverter
import com.tangem.features.collectibles.impl.packopening.ui.state.CollectiblesPackOpeningUM
import com.tangem.features.collectibles.impl.packopening.ui.state.CollectiblesPackOpeningUM.Stage
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class CollectiblesPackOpeningModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val getMachineUseCase: GetCollectibleGachaMachineUseCase,
    private val accountStatusSupplier: CollectiblesAccountStatusSupplier,
    private val openCollectiblePackUseCase: OpenCollectiblePackUseCase,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val messageSender: UiMessageSender,
    private val router: Router,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<CollectiblesPackOpeningComponent.Params>()

    val uiState: StateFlow<CollectiblesPackOpeningUM>
        field = MutableStateFlow<CollectiblesPackOpeningUM>(
            CollectiblesPackOpeningUM.Loading(onBackClick = ::onBackClick, onCloseClick = ::onCloseClick),
        )

    val bottomSheetNavigation = SlotNavigation<CollectiblesConfirmOrderBottomSheetConfig>()

    private val openPackStep = MutableStateFlow<OpenPackStep>(OpenPackStep.NotStarted)
    private var openPackJob: Job? = null

    private var machine: CollectibleGachaMachine? = null
    private var vaultBalance: CollectiblesVaultBalance? = null

    private val reveal = MutableStateFlow(RevealState())

    private val orderConverter = CollectiblesPackOpeningConverter(
        onBackClick = ::onBackClick,
        onCloseClick = ::onCloseClick,
        onApproveClick = ::onApproveClick,
        reveal = CollectiblesPackOpeningUM.RevealUM(
            card = null,
            onKeepCardClick = ::onKeepCardClick,
            onSellBackClick = ::onSellBackClick,
            onPackAnimationFinished = ::onPackAnimationFinished,
        ),
    )

    init {
        loadMachine()
    }

    private fun loadMachine(skipCache: Boolean = false) {
        modelScope.launch {
            getMachineUseCase(externalId = params.externalId, skipCache = skipCache)
                .fold(
                    ifLeft = {
                        uiState.value = CollectiblesPackOpeningUM.Error(
                            onBackClick = ::onBackClick,
                            onCloseClick = ::onCloseClick,
                            message = resourceReference(R.string.collectibles_machine_load_error),
                            onReloadClick = ::reloadMachine,
                        )
                    },
                    ifRight = { machine -> observeBalance(machine) },
                )
        }
    }

    private fun reloadMachine() {
        val error = uiState.value as? CollectiblesPackOpeningUM.Error ?: return
        if (error.isReloading) return

        uiState.value = error.copy(isReloading = true)
        loadMachine(skipCache = true)
    }

    // The catalogue below in the stack keeps this flow subscribed, so the balance is replayed without a request
    private suspend fun observeBalance(machine: CollectibleGachaMachine) {
        this.machine = machine

        val vaultBalanceFlow = accountStatusSupplier(params.userWalletId)
            .mapNotNull { status -> status.balance }
            .distinctUntilChanged()
            .onEach { balance -> vaultBalance = balance }

        combine(vaultBalanceFlow, openPackStep, reveal) { vaultBalance, step, reveal ->
            val content = orderConverter.convert(
                CollectiblesPackOpeningConverter.Input(
                    machine = machine,
                    vaultBalance = vaultBalance,
                    step = step,
                ),
            )
            content.copy(
                reveal = content.reveal.copy(
                    card = reveal.pack?.let(CollectiblesRewardCardConverter::convert),
                    stage = reveal.stage,
                ),
            )
        }.collect { content -> uiState.value = content }
    }

    private fun onApproveClick() {
        // Re-checked on tap, the same rule the button is drawn by
        if (!hasEnoughFunds()) {
            showSnackbar(resourceReference(R.string.collectibles_confirm_order_not_enough_funds_button))
            return
        }

        runOpenPack(OpenPackRequest.Buy(machineId = params.externalId, isTurboEnabled = params.isTurboEnabled))
    }

    private fun runOpenPack(request: OpenPackRequest) {
        if (openPackJob?.isActive == true) return

        openPackJob = modelScope.launch {
            val userWallet = userWalletsListRepository.getSyncOrNull(params.userWalletId)
                ?: return@launch showOpenPackError(OpenPackStep.Failed.PurchaseFailed)

            openCollectiblePackUseCase(userWallet, request).collect { step ->
                if (step is OpenPackStep.Failed) showOpenPackError(step)

                openPackStep.value = step

                // Thrown once the card is known: an earlier throw could not be taken back if the purchase then failed
                if (step is OpenPackStep.Opened) launchPack(step.pack)
            }
        }
    }

    private fun launchPack(pack: OpenedCollectiblePack) {
        reveal.update { it.copy(stage = Stage.Launching, pack = pack) }
    }

    private fun hasEnoughFunds(): Boolean {
        val machine = machine ?: return false
        val balance = vaultBalance ?: return false

        return balance.covers(machine)
    }

    private fun showOpenPackError(failure: OpenPackStep.Failed) {
        when (failure) {
            is OpenPackStep.Failed.PackUnopened -> bottomSheetNavigation.activate(
                CollectiblesConfirmOrderBottomSheetConfig.PackUnopened(failure.txId),
            )
            OpenPackStep.Failed.LowInventory -> bottomSheetNavigation.activate(
                CollectiblesConfirmOrderBottomSheetConfig.LowInventory,
            )
            // TODO an expired and a refunded purchase will likely open screens of their own; not localized yet
            OpenPackStep.Failed.Expired -> showSnackbar(stringReference("Your purchase has expired"))
            OpenPackStep.Failed.Refunded -> showSnackbar(stringReference("Your payment has been refunded"))
            OpenPackStep.Failed.PurchaseFailed,
            OpenPackStep.Failed.Unexpected,
            -> showSnackbar(resourceReference(R.string.common_something_went_wrong))
        }
    }

    private fun showSnackbar(message: TextReference) {
        messageSender.send(SnackbarMessage(message = message))
    }

    fun onReopenPack(txId: String) {
        bottomSheetNavigation.dismiss()
        runOpenPack(OpenPackRequest.Reopen(txId))
    }

    fun onEnableTurboMode() {
        bottomSheetNavigation.dismiss()
        router.pop()
    }

    fun onExplorePacks() {
        bottomSheetNavigation.dismiss()
        router.popTo(CollectiblesRoute.Main(params.userWalletId))
    }

    fun onDismissBottomSheet() {
        bottomSheetNavigation.dismiss()
    }

    private fun onPackAnimationFinished() {
        reveal.update { it.copy(stage = Stage.Reward) }
    }

    @Suppress("EmptyFunctionBlock")
    private fun onKeepCardClick() {
        // TODO([REDACTED_TASK_KEY]): keeping a card needs the collection screen, which is not built yet
    }

    @Suppress("EmptyFunctionBlock")
    private fun onSellBackClick() {
        // TODO([REDACTED_TASK_KEY]): instant buy-back needs the tx pipeline
    }

    private fun onBackClick() {
        if (isPurchaseRunning()) return

        router.pop()
    }

    private fun onCloseClick() {
        if (isPurchaseRunning()) return

        router.popTo(CollectiblesRoute.Main(params.userWalletId))
    }

    private fun isPurchaseRunning(): Boolean = when (openPackStep.value) {
        OpenPackStep.Generating, OpenPackStep.Confirming, OpenPackStep.Opening -> true
        else -> false
    }

    private data class RevealState(
        val stage: Stage = Stage.Order,
        val pack: OpenedCollectiblePack? = null,
    )
}