package com.tangem.features.collectibles.impl.nftdetails.model

import coil.request.SuccessResult
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.coil.ImagePreloader
import com.tangem.features.collectibles.impl.CollectiblesRoute
import com.tangem.features.collectibles.impl.common.ui.rewardcard.DEFAULT_CARD_ASPECT
import com.tangem.features.collectibles.impl.nftdetails.CollectiblesNftDetailsComponent
import com.tangem.features.collectibles.impl.nftdetails.model.converter.CollectiblesNftDetailsConverter
import com.tangem.features.collectibles.impl.nftdetails.ui.state.CollectiblesNftDetailsUM
import com.tangem.features.collectibles.impl.inventory.model.CollectiblesInventoryMockSource
import com.tangem.features.collectibles.impl.packopening.PackOpeningConst
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@ModelScoped
internal class CollectiblesNftDetailsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    // TODO([REDACTED_TASK_KEY]): read the card through CollectiblesInventoryRepository.getByAddress
    private val inventorySource: CollectiblesInventoryMockSource,
    private val router: Router,
    private val imagePreloader: ImagePreloader,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<CollectiblesNftDetailsComponent.Params>()

    val uiState: StateFlow<CollectiblesNftDetailsUM>
        field = MutableStateFlow<CollectiblesNftDetailsUM>(CollectiblesNftDetailsUM.Loading)

    init {
        loadCard()
    }

    fun onBackClick() {
        router.pop()
    }

    fun onCloseClick() {
        router.popTo(CollectiblesRoute.Main(params.userWalletId))
    }

    fun onBuybackClick() {
        // TODO([REDACTED_TASK_KEY]): accept the buyback
    }

    private fun loadCard() {
        val card = inventorySource.getCard(id = params.id)
        if (card == null) {
            TangemLogger.e("Collectible ${params.id} is not in the inventory cache")
            router.pop()
            return
        }

        val content = CollectiblesNftDetailsConverter.convert(card)
        uiState.value = content
        showCaseOncePreloaded(content)
    }

    /**
     * Like the reward card, the case appears once both faces are in the image cache, or at the timeout; its size
     * comes from the preloaded front face, so it never resizes once shown.
     */
    private fun showCaseOncePreloaded(content: CollectiblesNftDetailsUM.Content) {
        val frontLoad = content.frontImageUrl?.let(imagePreloader::preload)
        val backLoad = content.backImageUrl?.let(imagePreloader::preload)

        modelScope.launch {
            val front = withTimeoutOrNull(PackOpeningConst.CARD_IMAGES_TIMEOUT_MS.milliseconds) {
                backLoad?.job?.await()
                frontLoad?.job?.await()
            }
            val cardAspect = (front as? SuccessResult)?.drawable
                ?.takeIf { it.intrinsicWidth > 0 && it.intrinsicHeight > 0 }
                ?.let { it.intrinsicHeight.toFloat() / it.intrinsicWidth }
                ?: DEFAULT_CARD_ASPECT

            uiState.update { state ->
                (state as? CollectiblesNftDetailsUM.Content)?.copy(cardAspect = cardAspect) ?: state
            }
        }
    }
}