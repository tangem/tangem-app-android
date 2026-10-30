package com.tangem.domain.polymarket.interactor

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.domain.polymarket.model.PolymarketClobIdentity
import com.tangem.domain.polymarket.model.PolymarketMarketVersion
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.model.PredictionOrderDraftError
import com.tangem.domain.polymarket.model.PredictionOrderFailure
import com.tangem.domain.polymarket.model.PredictionOrderQuote
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.model.PredictionOrderSigningPreference
import com.tangem.domain.polymarket.model.PredictionOrderSigningRoute
import com.tangem.domain.polymarket.model.PredictionPlaceOrderError
import com.tangem.domain.polymarket.model.PredictionPlaceOrderResult
import com.tangem.domain.polymarket.model.PredictionSignedOrder
import com.tangem.domain.polymarket.sessionkey.PolymarketOrderSigningRouter
import com.tangem.domain.polymarket.usecase.PlacePredictionOrderUseCase
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SubmitPredictionOrderInteractorTest {

    private val signOrder: SignPredictionOrderInteractor = mockk()
    private val placeOrder: PlacePredictionOrderUseCase = mockk()
    private val router: PolymarketOrderSigningRouter = mockk(relaxUnitFun = true)
    private val quote: PredictionOrderQuote = mockk()

    private val interactor = SubmitPredictionOrderInteractor(
        signOrder = signOrder,
        placeOrder = placeOrder,
        router = router,
        reconciliationScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
    )

    private val request = PredictionSignRequest(
        addresses = ADDRESSES,
        quote = quote,
        assetId = "1",
        side = PredictionOrderSide.BUY,
        isNegRisk = false,
        quoteReceivedAtMillis = 0,
    )
    private val ownerRequest = request.copy(route = PredictionOrderSigningRoute.Owner)
    private val sessionRequest = request.copy(route = SESSION_ROUTE)
    private val ownerOrder: PredictionSignedOrder = mockk()
    private val sessionOrder: PredictionSignedOrder = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(signOrder, placeOrder, router)

        every { quote.version } returns PolymarketMarketVersion.V1
        coEvery { signOrder(ownerRequest) } returns ownerOrder.right()
        coEvery { signOrder(sessionRequest) } returns sessionOrder.right()
        coEvery { placeOrder(ADDRESSES, ownerOrder, PolymarketMarketVersion.V1, PredictionOrderSigningRoute.Owner) } returns
            RESULT.right()
        coEvery { placeOrder(ADDRESSES, sessionOrder, PolymarketMarketVersion.V1, SESSION_ROUTE) } returns
            RESULT.right()
    }

    @Test
    fun `GIVEN owner only WHEN invoke THEN signs with the owner key and never asks the router`() = runTest {
        // Act
        val actual = interactor(request = request, preference = PredictionOrderSigningPreference.OWNER_ONLY)

        // Assert
        assertThat(actual).isEqualTo(
            PredictionSubmittedOrder(result = RESULT, route = PredictionOrderSigningRoute.Owner).right(),
        )
        coVerify(exactly = 0) { router.route(any()) }
        coVerify(exactly = 0) { signOrder(sessionRequest) }
    }

    @Test
    fun `GIVEN the router chooses the owner key WHEN invoke THEN signs and places on the owner route`() = runTest {
        // Arrange
        coEvery { router.route(ADDRESSES) } returns PredictionOrderSigningRoute.Owner

        // Act
        val actual = interactor(request = request, preference = PredictionOrderSigningPreference.AUTOMATIC)

        // Assert
        assertThat(actual).isEqualTo(
            PredictionSubmittedOrder(result = RESULT, route = PredictionOrderSigningRoute.Owner).right(),
        )
        coVerify(exactly = 0) { signOrder(sessionRequest) }
    }

    @Test
    fun `GIVEN the router chooses the session key WHEN invoke THEN signs and places on the session route`() =
        runTest {
            // Arrange
            coEvery { router.route(ADDRESSES) } returns SESSION_ROUTE
            val steps = mutableListOf<PredictionOrderSubmitStep>()

            // Act
            val actual = interactor(
                request = request,
                preference = PredictionOrderSigningPreference.AUTOMATIC,
                onStep = steps::add,
            )

            // Assert
            assertThat(actual).isEqualTo(PredictionSubmittedOrder(result = RESULT, route = SESSION_ROUTE).right())
            assertThat(steps).containsExactly(PredictionOrderSubmitStep.SIGNING, PredictionOrderSubmitStep.POSTING)
                .inOrder()
            coVerify(exactly = 0) { signOrder(ownerRequest) }
            coVerify(exactly = 0) { router.reportRefused(any(), any(), any()) }
        }

    @Test
    fun `GIVEN the exchange refuses the session key WHEN invoke THEN signs the order with the owner key`() =
        runTest {
            // Arrange
            coEvery { router.route(ADDRESSES) } returns SESSION_ROUTE
            coEvery { placeOrder(ADDRESSES, sessionOrder, PolymarketMarketVersion.V1, SESSION_ROUTE) } returns
                UNAUTHORIZED.left()
            val steps = mutableListOf<PredictionOrderSubmitStep>()

            // Act
            val actual = interactor(
                request = request,
                preference = PredictionOrderSigningPreference.AUTOMATIC,
                onStep = steps::add,
            )

            // Assert
            assertThat(actual).isEqualTo(
                PredictionSubmittedOrder(result = RESULT, route = PredictionOrderSigningRoute.Owner).right(),
            )
            assertThat(steps).containsExactly(
                PredictionOrderSubmitStep.SIGNING,
                PredictionOrderSubmitStep.POSTING,
                PredictionOrderSubmitStep.SIGNING,
                PredictionOrderSubmitStep.POSTING,
            ).inOrder()
            coVerifyOrder {
                placeOrder(ADDRESSES, sessionOrder, PolymarketMarketVersion.V1, SESSION_ROUTE)
                router.reportRefused(addresses = ADDRESSES, identity = SESSION_IDENTITY, error = UNAUTHORIZED)
                signOrder(ownerRequest)
                placeOrder(ADDRESSES, ownerOrder, PolymarketMarketVersion.V1, PredictionOrderSigningRoute.Owner)
            }
            coVerify(exactly = 1) { router.reconcile(ADDRESSES) }
        }

    @Test
    fun `GIVEN the session order could not be sent WHEN invoke THEN signs the order with the owner key`() = runTest {
        // Arrange
        val notSent = PredictionPlaceOrderError.NotSent(detail = "headers")
        coEvery { router.route(ADDRESSES) } returns SESSION_ROUTE
        coEvery { placeOrder(ADDRESSES, sessionOrder, PolymarketMarketVersion.V1, SESSION_ROUTE) } returns
            notSent.left()

        // Act
        val actual = interactor(request = request, preference = PredictionOrderSigningPreference.AUTOMATIC)

        // Assert
        assertThat(actual).isEqualTo(
            PredictionSubmittedOrder(result = RESULT, route = PredictionOrderSigningRoute.Owner).right(),
        )
        coVerify(exactly = 1) {
            router.reportRefused(addresses = ADDRESSES, identity = SESSION_IDENTITY, error = notSent)
        }
    }

    @Test
    fun `GIVEN the exchange refuses the session signature WHEN invoke THEN signs the order with the owner key`() =
        runTest {
            // Arrange
            coEvery { router.route(ADDRESSES) } returns SESSION_ROUTE
            coEvery { placeOrder(ADDRESSES, sessionOrder, PolymarketMarketVersion.V1, SESSION_ROUTE) } returns
                SESSION_KEY_REFUSED.left()

            // Act
            val actual = interactor(request = request, preference = PredictionOrderSigningPreference.AUTOMATIC)

            // Assert
            assertThat(actual).isEqualTo(
                PredictionSubmittedOrder(result = RESULT, route = PredictionOrderSigningRoute.Owner).right(),
            )
            coVerify(exactly = 1) {
                router.reportRefused(addresses = ADDRESSES, identity = SESSION_IDENTITY, error = SESSION_KEY_REFUSED)
            }
        }

    @Test
    fun `GIVEN the reconciliation is still running WHEN invoke after a refusal THEN the owner result returns first`() =
        runTest {
            // Arrange
            val reconciliation = CompletableDeferred<Unit>()
            var isReconciled = false
            coEvery { router.route(ADDRESSES) } returns SESSION_ROUTE
            coEvery { placeOrder(ADDRESSES, sessionOrder, PolymarketMarketVersion.V1, SESSION_ROUTE) } returns
                UNAUTHORIZED.left()
            coEvery { router.reconcile(any()) } coAnswers {
                reconciliation.await()
                isReconciled = true
            }

            // Act
            val actual = interactor(request = request, preference = PredictionOrderSigningPreference.AUTOMATIC)
            val isReconciledOnReturn = isReconciled
            reconciliation.complete(Unit)

            // Assert
            assertThat(actual).isEqualTo(
                PredictionSubmittedOrder(result = RESULT, route = PredictionOrderSigningRoute.Owner).right(),
            )
            assertThat(isReconciledOnReturn).isFalse()
            assertThat(isReconciled).isTrue()
        }

    @Test
    fun `GIVEN the owner key fails after a refusal WHEN invoke THEN returns the owner placement error`() = runTest {
        // Arrange
        val rejected = PredictionPlaceOrderError.Rejected(detail = "not enough balance")
        coEvery { router.route(ADDRESSES) } returns SESSION_ROUTE
        coEvery { placeOrder(ADDRESSES, sessionOrder, PolymarketMarketVersion.V1, SESSION_ROUTE) } returns
            UNAUTHORIZED.left()
        coEvery { placeOrder(ADDRESSES, ownerOrder, PolymarketMarketVersion.V1, PredictionOrderSigningRoute.Owner) } returns
            rejected.left()

        // Act
        val actual = interactor(request = request, preference = PredictionOrderSigningPreference.AUTOMATIC)

        // Assert
        assertThat(actual).isEqualTo(PredictionSubmitOrderError.NotPlaced(error = rejected).left())
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a session order fails for another reason WHEN invoke THEN returns it without the owner key`(
        model: PlacementModel,
    ) = runTest {
        // Arrange
        coEvery { router.route(ADDRESSES) } returns SESSION_ROUTE
        coEvery { placeOrder(ADDRESSES, sessionOrder, PolymarketMarketVersion.V1, SESSION_ROUTE) } returns
            model.error.left()

        // Act
        val actual = interactor(request = request, preference = PredictionOrderSigningPreference.AUTOMATIC)

        // Assert
        assertThat(actual).isEqualTo(PredictionSubmitOrderError.NotPlaced(error = model.error).left())
        coVerify(exactly = 0) { signOrder(ownerRequest) }
        coVerify(exactly = 0) { router.reportRefused(any(), any(), any()) }
    }

    private fun provideTestModels() = listOf(
        PlacementModel(error = PredictionPlaceOrderError.Rejected(detail = "invalid amount")),
        PlacementModel(error = PredictionPlaceOrderError.NoLiquidity(detail = "no orders found to match")),
        PlacementModel(error = PredictionPlaceOrderError.Ambiguous(detail = "timeout")),
    )

    @Test
    fun `GIVEN the session key cannot sign WHEN invoke THEN signs the order with the owner key`() = runTest {
        // Arrange
        coEvery { router.route(ADDRESSES) } returns SESSION_ROUTE
        val failure = PredictionOrderFailure.Signing(
            error = PolymarketSigningError.SessionKey(error = PolymarketSessionKeyError.KeyMissing),
        )
        coEvery { signOrder(sessionRequest) } returns failure.left()

        // Act
        val actual = interactor(request = request, preference = PredictionOrderSigningPreference.AUTOMATIC)

        // Assert
        assertThat(actual).isEqualTo(
            PredictionSubmittedOrder(result = RESULT, route = PredictionOrderSigningRoute.Owner).right(),
        )
        coVerify(exactly = 0) { placeOrder(any(), sessionOrder, any(), any()) }
        coVerifyOrder {
            router.reportSigningFailed(addresses = ADDRESSES, identity = SESSION_IDENTITY, failure = failure)
            signOrder(ownerRequest)
        }
        coVerify(exactly = 0) { router.reportRefused(any(), any(), any()) }
        coVerify(exactly = 0) { router.reconcile(any()) }
    }

    @Test
    fun `GIVEN the session key signing is cancelled WHEN invoke THEN returns it without the owner key`() = runTest {
        // Arrange
        val failure = PredictionOrderFailure.Signing(error = PolymarketSigningError.UserCancelled)
        coEvery { router.route(ADDRESSES) } returns SESSION_ROUTE
        coEvery { signOrder(sessionRequest) } returns failure.left()

        // Act
        val actual = interactor(request = request, preference = PredictionOrderSigningPreference.AUTOMATIC)

        // Assert
        assertThat(actual).isEqualTo(PredictionSubmitOrderError.NotSigned(failure = failure).left())
        coVerify(exactly = 0) { signOrder(ownerRequest) }
        coVerify(exactly = 0) { router.reportSigningFailed(any(), any(), any()) }
    }

    @Test
    fun `GIVEN the session order cannot be drafted WHEN invoke THEN returns the failure without the owner key`() =
        runTest {
            // Arrange
            val failure = PredictionOrderFailure.Draft(error = PredictionOrderDraftError.UnknownMarketVersion)
            coEvery { router.route(ADDRESSES) } returns SESSION_ROUTE
            coEvery { signOrder(sessionRequest) } returns failure.left()

            // Act
            val actual = interactor(request = request, preference = PredictionOrderSigningPreference.AUTOMATIC)

            // Assert
            assertThat(actual).isEqualTo(PredictionSubmitOrderError.NotSigned(failure = failure).left())
            coVerify(exactly = 0) { signOrder(ownerRequest) }
        }

    data class PlacementModel(val error: PredictionPlaceOrderError)

    private companion object {
        val ADDRESSES = PolymarketAddresses(
            ownerAddress = "0x00000000000000000000000000000000000000aa",
            depositWalletAddress = "0x00000000000000000000000000000000000000bb",
            userWalletId = UserWalletId("011"),
        )
        val SESSION_IDENTITY = PolymarketClobIdentity.Session(
            address = "0x5e55000000000000000000000000000000000001",
            credentials = PolymarketApiCredentials(apiKey = "session", secret = "c2VjcmV0", passphrase = "pass"),
        )
        val SESSION_ROUTE = PredictionOrderSigningRoute.Session(identity = SESSION_IDENTITY)
        val UNAUTHORIZED = PredictionPlaceOrderError.Unauthorized(detail = "Unauthorized/Invalid api key")
        val SESSION_KEY_REFUSED = PredictionPlaceOrderError.SessionKeyRefused(
            httpCode = 400,
            reason = "invalid signature",
        )
        val RESULT = PredictionPlaceOrderResult(
            isSuccess = true,
            orderId = "order",
            status = "matched",
            makingAmount = null,
            takingAmount = null,
            transactionHashes = emptyList(),
            errorMessage = null,
        )
    }
}