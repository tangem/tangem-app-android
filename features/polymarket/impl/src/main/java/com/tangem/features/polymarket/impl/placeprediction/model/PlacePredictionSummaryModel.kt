package com.tangem.features.polymarket.impl.placeprediction.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.features.polymarket.impl.common.PolymarketLegalUrls
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionRoute
import com.tangem.features.polymarket.impl.placeprediction.entity.PlaceButtonStyleUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PredictionFeeSheetUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PreparedPrediction
import com.tangem.features.polymarket.impl.placeprediction.entity.SubmitUM
import com.tangem.features.polymarket.impl.placeprediction.entity.toStatusRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class PlacePredictionSummaryModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val urlOpener: UrlOpener,
    override val dispatchers: CoroutineDispatcherProvider,
    orderSubmitterFactory: PlacePredictionOrderSubmitter.Factory,
    buttonStyleFactory: PlacePredictionButtonStyle.Factory,
    private val quoteFreshness: PlacePredictionQuoteFreshness,
) : Model(), PlacePredictionSummaryIntents {

    private val order = paramsContainer.require<PreparedPrediction>()
    private val params = order.params

    val uiState: StateFlow<PlacePredictionUM>
        field = MutableStateFlow(order.state)

    private val buttonStyle = buttonStyleFactory.create(
        userWalletId = params.userWalletId,
        uiState = uiState,
        scope = modelScope,
    )

    private val orderSubmitter = orderSubmitterFactory.create(
        params = params,
        uiState = uiState,
        scope = modelScope,
        closeOrder = ::onCloseClick,
    )

    val feeSheet = PlacePredictionFeeSheet(
        uiState = uiState,
        scope = modelScope,
        heading = PredictionFeeSheetUM.Heading.Fee,
    )

    fun onResume() {
        buttonStyle.checkSessionKey()
    }

    override fun onFeeClick() {
        feeSheet.onFeeClick()
    }

    override fun onPlaceClick(confirmedWith: PlaceButtonStyleUM) {
        if (uiState.value.submit is SubmitUM.Idle && quoteFreshness.refuseIfStale(quote = order.quote)) {
            router.pop()
            return
        }

        modelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            val result = orderSubmitter.submit(
                prepared = order,
                preference = buttonStyle.signingPreference(confirmedWith = confirmedWith),
                onKeyLost = buttonStyle::onSessionKeyLost,
            )
            if (result == null) {
                buttonStyle.onPlacementFailed()
                uiState.update { it.copy(holdResetKey = it.holdResetKey + 1) }
                buttonStyle.applyIfIdle()
                return@launch
            }
            router.replaceAll(order.toStatusRoute(result = result, current = uiState.value))
        }
    }

    override fun onQuoteRetryClick() {
        if (!isSubmitting()) router.popTo(PlacePredictionRoute.Amount)
    }

    override fun onPolymarketTermsClick() {
        urlOpener.openUrl(PolymarketLegalUrls.polymarketTerms)
    }

    override fun onTangemTermsClick() {
        urlOpener.openUrl(PolymarketLegalUrls.TANGEM_TERMS)
    }

    override fun onBackClick() {
        if (isSubmitting()) return

        router.pop()
    }

    override fun onCloseClick() {
        if (isSubmitting()) return

        router.popTo(PlacePredictionRoute.Amount)
        router.pop()
    }

    fun isSubmitting(): Boolean = orderSubmitter.isSubmitting()
}