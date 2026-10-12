package com.tangem.features.polymarket.impl.placeprediction.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.polymarket.PolymarketPositionsRepository
import com.tangem.domain.polymarket.model.PolymarketPositionId
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.features.polymarket.impl.common.PolymarketConst
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionRoute
import com.tangem.features.polymarket.impl.placeprediction.entity.PlaceResultUM
import com.tangem.features.polymarket.impl.placeprediction.entity.ViewPositionUM
import com.tangem.features.polymarket.impl.positiondetails.model.canOpenPositionDetails
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ModelScoped
internal class PlacePredictionStatusModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
    private val derivePolymarketAddressesUseCase: DerivePolymarketAddressesUseCase,
    private val positionsRepository: PolymarketPositionsRepository,
) : Model(), PlacePredictionStatusIntents {

    private val params = paramsContainer.require<PlacePredictionRoute.Status>()

    val viewPosition: StateFlow<ViewPositionUM>
        field = MutableStateFlow<ViewPositionUM>(ViewPositionUM.Hidden)

    private var indexedPositionId: PolymarketPositionId? = null

    init {
        awaitCreatedPosition()
    }

    override fun onToMainClick() {
        router.popTo(PolymarketRoute.Main(userWalletId = params.userWalletId))
    }

    override fun onViewPositionClick() {
        val positionId = indexedPositionId ?: return

        router.replaceAll(
            PolymarketRoute.Main(userWalletId = params.userWalletId),
            PolymarketRoute.PositionDetails(userWalletId = params.userWalletId, positionId = positionId),
        )
    }

    /** The exchange may take up to 30 seconds to index a fill; unavailable positions must not open details. */
    private fun awaitCreatedPosition() {
        if (params.side != PredictionOrderSide.BUY) return
        val result = params.result
        if (result !is PlaceResultUM.Filled && result !is PlaceResultUM.PartiallyFilled) return
        val positionId = params.positionId ?: return

        viewPosition.value = ViewPositionUM.Awaiting

        modelScope.launch {
            val addresses = withContext(dispatchers.default) {
                derivePolymarketAddressesUseCase.stored(userWalletId = params.userWalletId)
            }
            if (addresses == null) {
                logger.e("Stored addresses unavailable, the created position cannot be read back")
                viewPosition.value = ViewPositionUM.Hidden
                return@launch
            }

            repeat(PolymarketConst.CREATED_POSITION_READS) { read ->
                val position = withContext(dispatchers.default) {
                    positionsRepository.getPosition(addresses = addresses, id = positionId)
                }.getOrNull()

                if (position != null) {
                    val canOpen = position.canOpenPositionDetails
                    if (canOpen) indexedPositionId = positionId
                    viewPosition.value = if (canOpen) ViewPositionUM.Available else ViewPositionUM.Hidden
                    return@launch
                }
                if (read < PolymarketConst.CREATED_POSITION_READS - 1) {
                    delay(PolymarketConst.POSITION_INDEXING_RETRY_MILLIS)
                }
            }

            logger.i("The created position was not indexed after ${PolymarketConst.CREATED_POSITION_READS} reads")
            viewPosition.value = ViewPositionUM.Hidden
        }
    }

    private companion object {
        val logger = TangemLogger.withTag(tag = "PlacePredictionStatusModel")
    }
}