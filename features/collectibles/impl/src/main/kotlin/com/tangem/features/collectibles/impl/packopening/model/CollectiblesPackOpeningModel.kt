package com.tangem.features.collectibles.impl.packopening.model

import coil.request.Disposable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.coil.ImagePreloader
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.collectibles.account.covers
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusSupplier
import com.tangem.domain.collectibles.models.BuybackError
import com.tangem.domain.collectibles.models.CollectibleBuybackOffer
import com.tangem.domain.collectibles.models.CollectibleGachaMachine
import com.tangem.domain.collectibles.models.OpenPackRequest
import com.tangem.domain.collectibles.models.OpenPackStep
import com.tangem.domain.collectibles.models.OpenedCollectiblePack
import com.tangem.domain.collectibles.usecase.BuybackCollectibleUseCase
import com.tangem.domain.collectibles.usecase.GetCollectibleBuybackOfferUseCase
import com.tangem.domain.collectibles.usecase.GetCollectibleGachaMachineUseCase
import com.tangem.domain.collectibles.usecase.OpenCollectiblePackUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncOrNull
import com.tangem.domain.models.account.CollectiblesVaultBalance
import com.tangem.features.collectibles.impl.CollectiblesRoute
import com.tangem.features.collectibles.impl.common.formatter.formatUsd
import com.tangem.features.collectibles.impl.packopening.CollectiblesConfirmOrderBottomSheetConfig
import com.tangem.features.collectibles.impl.packopening.CollectiblesPackOpeningComponent
import com.tangem.features.collectibles.impl.packopening.PackOpeningConst
import com.tangem.features.collectibles.impl.packopening.model.converter.CollectiblesPackOpeningConverter
import com.tangem.features.collectibles.impl.packopening.model.converter.CollectiblesRewardCardConverter
import com.tangem.features.collectibles.impl.packopening.model.converter.CollectiblesSellBackButtonConverter
import com.tangem.features.collectibles.impl.packopening.ui.state.CollectiblesPackOpeningUM
import com.tangem.features.collectibles.impl.packopening.ui.state.CollectiblesPackOpeningUM.Stage
import com.tangem.features.collectibles.impl.packopening.ui.state.RewardCardUM
import com.tangem.features.collectibles.impl.packopening.video.PackRevealVideoPlayer
import com.tangem.features.collectibles.impl.packopening.video.PackRevealVideoPlayer.VideoLoadState
import com.tangem.features.collectibles.impl.packopening.video.source.PackRevealAsset
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.PeriodicTask
import com.tangem.utils.coroutines.SingleTaskScheduler
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@Suppress("LongParameterList", "LargeClass")
@ModelScoped
internal class CollectiblesPackOpeningModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val getMachineUseCase: GetCollectibleGachaMachineUseCase,
    private val accountStatusSupplier: CollectiblesAccountStatusSupplier,
    private val openCollectiblePackUseCase: OpenCollectiblePackUseCase,
    private val getBuybackOfferUseCase: GetCollectibleBuybackOfferUseCase,
    private val buybackCollectibleUseCase: BuybackCollectibleUseCase,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val messageSender: UiMessageSender,
    private val router: Router,
    private val imagePreloader: ImagePreloader,
    playerFactory: PackRevealVideoPlayer.Factory,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<CollectiblesPackOpeningComponent.Params>()

    val uiState: StateFlow<CollectiblesPackOpeningUM>
        field = MutableStateFlow<CollectiblesPackOpeningUM>(
            CollectiblesPackOpeningUM.Loading(onBackClick = ::onBackClick, onCloseClick = ::onCloseClick),
        )

    val bottomSheetNavigation = SlotNavigation<CollectiblesConfirmOrderBottomSheetConfig>()

    private val openPackStep = MutableStateFlow<OpenPackStep>(OpenPackStep.NotStarted)
    private val reveal = MutableStateFlow(RevealState())

    private var machine: CollectibleGachaMachine? = null
    private var vaultBalance: CollectiblesVaultBalance? = null

    private val buyback = MutableStateFlow<BuybackState>(BuybackState.Unavailable)
    private val buybackOfferPolling = SingleTaskScheduler<CollectibleBuybackOffer?>()

    private val isComponentResumed = MutableStateFlow(false)
    private val isDarkTheme = MutableStateFlow<Boolean?>(null)

    private val player = playerFactory.create(
        listener = object : PackRevealVideoPlayer.Listener {
            override fun onCardRevealReached() = onCardReveal()

            override fun onError(error: Throwable) {
                TangemLogger.e("Pack reveal video failed", error)
            }
        },
    )

    private val orderConverter = CollectiblesPackOpeningConverter(
        onBackClick = ::onBackClick,
        onCloseClick = ::onCloseClick,
        onApproveClick = ::onApproveClick,
        reveal = CollectiblesPackOpeningUM.RevealUM(
            card = null,
            onKeepCardClick = ::onKeepCardClick,
            onSellBackClick = ::onSellBackClick,
            sellBack = CollectiblesSellBackButtonConverter.convert(BuybackState.Unavailable),
            onPackAnimationFinished = ::onPackAnimationFinished,
            onRevealTap = ::onRevealTap,
            onSurfaceAttached = player::attachSurface,
            onSurfaceDetached = player::detachSurface,
        ),
    )

    init {
        loadMachine()
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }

    fun onResume() {
        isComponentResumed.value = true
    }

    fun onPause() {
        isComponentResumed.value = false
    }

    fun onThemeChanged(isDarkTheme: Boolean) {
        this.isDarkTheme.value = isDarkTheme
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
                    ifRight = { machine ->
                        preloadReveal(machine.revealAssets)
                        observeBalance(machine)
                    },
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

        combine(
            flow = vaultBalanceFlow,
            flow2 = openPackStep,
            flow3 = reveal,
            flow4 = buyback,
        ) { vaultBalance, step, reveal, buyback ->
            val content = orderConverter.convert(
                CollectiblesPackOpeningConverter.Input(
                    machine = machine,
                    vaultBalance = vaultBalance,
                    step = step,
                ),
            )
            content.copy(
                reveal = content.reveal.copy(
                    card = reveal.card,
                    stage = reveal.stage,
                    isFullReveal = reveal.isFullReveal,
                    isSkipped = reveal.isSkipped,
                    sellBack = CollectiblesSellBackButtonConverter.convert(buyback),
                    isKeepCardEnabled = buyback !is BuybackState.InProgress,
                ),
            )
        }.collect { content -> uiState.value = content }
    }

    private fun preloadReveal(assets: CollectibleGachaMachine.RevealAssets?) {
        modelScope.launch { player.preloadSkipSound(assets?.skipAudioUrl) }
        modelScope.launch {
            combine(isDarkTheme.filterNotNull(), reveal.map { it.stage == Stage.Order }, ::Pair)
                .distinctUntilChanged()
                .collectLatest { (isDark, isOrder) ->
                    if (!isOrder) return@collectLatest

                    val videoUrl = if (isDark) assets?.darkVideoUrl else assets?.lightVideoUrl
                    player.preloadVideo(videoUrl?.let { PackRevealAsset(videoUrl = it) })
                }
        }
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
        val step = openPackStep.value
        if (step !is OpenPackStep.NotStarted && step !is OpenPackStep.Failed) return

        val userWallet = userWalletsListRepository.getSyncOrNull(params.userWalletId) ?: run {
            showOpenPackError(OpenPackStep.Failed.PurchaseFailed)
            openPackStep.value = OpenPackStep.Failed.PurchaseFailed
            return
        }

        openPackStep.value = when (request) {
            is OpenPackRequest.Buy -> OpenPackStep.Generating
            is OpenPackRequest.Reopen -> OpenPackStep.Confirming
        }

        modelScope.launch {
            openCollectiblePackUseCase(userWallet, request).collect { step ->
                if (step is OpenPackStep.Failed) showOpenPackError(step)

                openPackStep.value = step

                // Thrown once the card is known: an earlier throw could not be taken back if the purchase then failed
                if (step is OpenPackStep.Opened) {
                    observeBuybackOfferPolling(step.pack)
                    launchPack(step.pack)
                }
            }
        }
    }

    private suspend fun launchPack(pack: OpenedCollectiblePack) {
        val cardImages = preloadCardImages(pack)
        val isFull = awaitVideo() == VideoLoadState.Ready

        cardImages.await()

        if (isFull) player.start()

        reveal.update { state ->
            state.copy(
                stage = Stage.Launching,
                isFullReveal = isFull,
                card = CollectiblesRewardCardConverter.convert(pack),
            )
        }
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

    /** Completes once both faces are in the image cache, or at the timeout; failed loads count as done. */
    private fun preloadCardImages(pack: OpenedCollectiblePack): Deferred<Unit> {
        val loads: List<Disposable> = listOfNotNull(pack.imageFrontUrl, pack.imageBackUrl).map(imagePreloader::preload)

        return modelScope.async {
            withTimeoutOrNull(PackOpeningConst.CARD_IMAGES_TIMEOUT_MS.milliseconds) {
                loads.forEach { load -> load.job.await() }
            }
        }
    }

    private suspend fun awaitVideo(): VideoLoadState? =
        withTimeoutOrNull(PackOpeningConst.VIDEO_READY_TIMEOUT_MS.milliseconds) {
            player.videoLoadState.firstOrNull { it != VideoLoadState.Loading }
        }

    private fun onPackAnimationFinished() {
        val state = reveal.updateAndGet {
            it.copy(stage = if (it.isFullReveal && !it.isCardRevealReached) Stage.Reveal else Stage.Reward)
        }
        if (!state.isFullReveal) player.playSkipSound()
    }

    private fun onCardReveal() = reveal.update { state ->
        if (state.stage == Stage.Reveal) state.copy(stage = Stage.Reward) else state.copy(isCardRevealReached = true)
    }

    private fun onRevealTap() {
        if (reveal.value.stage != Stage.Reveal) return
        if (player.skipToReveal()) reveal.update { it.copy(isSkipped = true) }
    }

    private fun onKeepCardClick() {
        if (isBuybackRunning()) return

        router.pop()
    }

    // TODO waiting for Collector Crypt: whether "not available" can turn into "available", or one check is enough
    // Driven by state: polls only while there is no offer and the screen is in front
    private fun observeBuybackOfferPolling(pack: OpenedCollectiblePack) {
        combine(buyback.map { it is BuybackState.Unavailable }, isComponentResumed) { isUnavailable, isResumed ->
            isUnavailable && isResumed
        }
            .distinctUntilChanged()
            .onEach { shouldPoll ->
                if (shouldPoll) startBuybackOfferPolling(pack) else buybackOfferPolling.cancelTask()
            }
            .launchIn(modelScope)
    }

    private fun startBuybackOfferPolling(pack: OpenedCollectiblePack) {
        buybackOfferPolling.scheduleTask(
            scope = modelScope,
            task = PeriodicTask(
                delay = BUYBACK_OFFER_POLLING_DELAY_MS,
                task = {
                    getBuybackOfferUseCase(userWalletId = params.userWalletId, nftAddress = pack.nftAddress)
                        .fold(ifLeft = { Result.failure(it.cause) }, ifRight = { Result.success(it) })
                },
                onSuccess = { offer ->
                    if (offer != null && buyback.value is BuybackState.Unavailable) {
                        buyback.value = BuybackState.Available(offer)
                    }
                },
                onError = { error -> TangemLogger.e("Unable to check the buyback offer", error) },
            ),
        )
    }

    private fun onSellBackClick() {
        val offer = (buyback.value as? BuybackState.Available)?.offer ?: return
        val pack = (openPackStep.value as? OpenPackStep.Opened)?.pack ?: return

        buyback.value = BuybackState.InProgress(offer)
        modelScope.launch {
            val userWallet = userWalletsListRepository.getSyncOrNull(params.userWalletId)
                ?: return@launch onBuybackFailed(BuybackError.Unexpected, offer)

            buybackCollectibleUseCase(userWallet, pack.nftAddress).fold(
                ifLeft = { error -> onBuybackFailed(error, offer) },
                ifRight = {
                    showSnackbar(
                        resourceReference(
                            id = R.string.collectibles_open_pack_sell_back_success,
                            formatArgs = wrappedList(offer.amount.formatUsd()),
                        ),
                    )
                    router.pop()
                },
            )
        }
    }

    private fun onBuybackFailed(error: BuybackError, offer: CollectibleBuybackOffer) {
        when (error) {
            // The NFC sheet has already shown the card's refusal, and a cancel needs no answer
            BuybackError.Cancelled, BuybackError.CardRefused -> {
                buyback.value = BuybackState.Available(offer)
            }
            BuybackError.Unexpected -> {
                buyback.value = BuybackState.Available(offer)
                showSnackbar(resourceReference(R.string.unexpected_error_description))
            }
            // The sale may still land, so the offer is asked for again instead of being offered at once
            BuybackError.NotConfirmed -> {
                buyback.value = BuybackState.Unavailable
                showSnackbar(resourceReference(R.string.unexpected_error_description))
            }
        }
    }

    private fun onBackClick() {
        if (isOpeningRunning() || isBuybackRunning()) return

        router.pop()
    }

    private fun onCloseClick() {
        if (isOpeningRunning() || isBuybackRunning()) return

        router.popTo(CollectiblesRoute.Main(params.userWalletId))
    }

    private fun isOpeningRunning(): Boolean = when (openPackStep.value) {
        OpenPackStep.Generating, OpenPackStep.Confirming, OpenPackStep.Opening -> true
        is OpenPackStep.Opened -> reveal.value.stage != Stage.Reward
        else -> false
    }

    private fun isBuybackRunning(): Boolean = buyback.value is BuybackState.InProgress

    private data class RevealState(
        val stage: Stage = Stage.Order,
        val isFullReveal: Boolean = true,
        val isCardRevealReached: Boolean = false,
        val isSkipped: Boolean = false,
        val card: RewardCardUM? = null,
    )

    private companion object {

        const val BUYBACK_OFFER_POLLING_DELAY_MS = 5_000L
    }
}