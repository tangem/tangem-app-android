package com.tangem.domain.polymarket.interactor

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.model.PredictionOrderFailure
import com.tangem.domain.polymarket.model.PredictionOrderSigningPreference
import com.tangem.domain.polymarket.model.PredictionOrderSigningRoute
import com.tangem.domain.polymarket.model.PredictionPlaceOrderError
import com.tangem.domain.polymarket.model.PredictionPlaceOrderResult
import com.tangem.domain.polymarket.sessionkey.PolymarketOrderSigningRouter
import com.tangem.domain.polymarket.usecase.PlacePredictionOrderUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SubmitPredictionOrderInteractor(
    private val signOrder: SignPredictionOrderInteractor,
    private val placeOrder: PlacePredictionOrderUseCase,
    private val router: PolymarketOrderSigningRouter,
    private val reconciliationScope: CoroutineScope,
) {

    /**
     * @param request the order to sign; its route is chosen here
     * @param onStep told when a signature is requested and when a signed order is sent, possibly twice each
     */
    suspend operator fun invoke(
        request: PredictionSignRequest,
        preference: PredictionOrderSigningPreference,
        onStep: (PredictionOrderSubmitStep) -> Unit = {},
    ): Either<PredictionSubmitOrderError, PredictionSubmittedOrder> {
        val route = when (preference) {
            PredictionOrderSigningPreference.OWNER_ONLY -> PredictionOrderSigningRoute.Owner
            PredictionOrderSigningPreference.AUTOMATIC -> router.route(addresses = request.addresses)
        }

        return when (route) {
            PredictionOrderSigningRoute.Owner -> submit(request = request, route = route, onStep = onStep)
            is PredictionOrderSigningRoute.Session -> submitWithSessionKey(
                request = request,
                route = route,
                onStep = onStep,
            )
        }
    }

    private suspend fun submitWithSessionKey(
        request: PredictionSignRequest,
        route: PredictionOrderSigningRoute.Session,
        onStep: (PredictionOrderSubmitStep) -> Unit,
    ): Either<PredictionSubmitOrderError, PredictionSubmittedOrder> {
        onStep(PredictionOrderSubmitStep.SIGNING)
        val signedOrder = signOrder(request = request.copy(route = route)).getOrElse { failure ->
            if (!failure.isLocalSigningFailure()) return PredictionSubmitOrderError.NotSigned(failure).left()

            router.reportSigningFailed(addresses = request.addresses, identity = route.identity, failure = failure)
            return submit(request = request, route = PredictionOrderSigningRoute.Owner, onStep = onStep)
        }

        onStep(PredictionOrderSubmitStep.POSTING)
        val result = placeOrder(
            addresses = request.addresses,
            signedOrder = signedOrder,
            version = request.quote.version,
            route = route,
        ).getOrElse { error ->
            if (!error.refusesSessionKey()) return PredictionSubmitOrderError.NotPlaced(error).left()

            router.reportRefused(addresses = request.addresses, identity = route.identity, error = error)
            reconciliationScope.launch { router.reconcile(addresses = request.addresses) }
            return submit(request = request, route = PredictionOrderSigningRoute.Owner, onStep = onStep)
        }

        return PredictionSubmittedOrder(result = result, route = route).right()
    }

    private suspend fun submit(
        request: PredictionSignRequest,
        route: PredictionOrderSigningRoute,
        onStep: (PredictionOrderSubmitStep) -> Unit,
    ): Either<PredictionSubmitOrderError, PredictionSubmittedOrder> {
        onStep(PredictionOrderSubmitStep.SIGNING)
        val signedOrder = signOrder(request = request.copy(route = route))
            .getOrElse { failure -> return PredictionSubmitOrderError.NotSigned(failure).left() }

        onStep(PredictionOrderSubmitStep.POSTING)
        return placeOrder(
            addresses = request.addresses,
            signedOrder = signedOrder,
            version = request.quote.version,
            route = route,
        )
            .mapLeft(PredictionSubmitOrderError::NotPlaced)
            .map { result -> PredictionSubmittedOrder(result = result, route = route) }
    }

    private fun PredictionOrderFailure.isLocalSigningFailure(): Boolean =
        this is PredictionOrderFailure.Signing && error != PolymarketSigningError.UserCancelled

    private fun PredictionPlaceOrderError.refusesSessionKey(): Boolean = when (this) {
        is PredictionPlaceOrderError.Unauthorized,
        is PredictionPlaceOrderError.SessionKeyRefused,
        is PredictionPlaceOrderError.NotSent,
        -> true
        is PredictionPlaceOrderError.Rejected,
        is PredictionPlaceOrderError.NoLiquidity,
        is PredictionPlaceOrderError.Ambiguous,
        -> false
    }
}

enum class PredictionOrderSubmitStep {
    SIGNING,
    POSTING,
}

/** @property route the route the placed order was signed on; a delayed order is read back on it */
data class PredictionSubmittedOrder(
    val result: PredictionPlaceOrderResult,
    val route: PredictionOrderSigningRoute,
)

sealed interface PredictionSubmitOrderError {

    data class NotSigned(val failure: PredictionOrderFailure) : PredictionSubmitOrderError

    data class NotPlaced(val error: PredictionPlaceOrderError) : PredictionSubmitOrderError
}