package com.tangem.data.polymarket

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.domain.polymarket.model.PredictionOrderQuoteError
import com.tangem.domain.polymarket.model.PredictionQuoteStatus
import com.tangem.grow.datasource.polymarket.models.PolymarketOrderQuoteFeesResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketOrderQuoteRequest
import com.tangem.grow.datasource.polymarket.models.PolymarketOrderQuoteResponse
import com.tangem.domain.polymarket.model.PredictionOrderQuoteRequest
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.data.polymarket.builder.PolymarketOrderTypedDataBuilder
import com.tangem.data.polymarket.error.PolymarketAuthErrorResolver
import com.tangem.data.polymarket.error.PolymarketEventErrorResolver
import com.tangem.data.polymarket.error.PredictionPlaceOrderErrorResolver
import com.tangem.data.polymarket.error.PolymarketWalletErrorResolver
import com.tangem.data.polymarket.error.ProblemDetailParser
import com.tangem.data.polymarket.signer.Base64UrlCodec
import com.tangem.data.polymarket.signer.PolymarketHmacSigner
import com.tangem.data.polymarket.signer.PolymarketL2HeaderBuilder
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.grow.datasource.polymarket.PolymarketApi
import com.tangem.grow.datasource.polymarket.clob.PolymarketClobApi
import com.tangem.grow.datasource.polymarket.clob.models.PolymarketApiKeyResponse
import com.tangem.grow.datasource.polymarket.clob.models.PolymarketBalanceAllowanceResponse
import com.tangem.grow.datasource.polymarket.clob.models.PolymarketOrderResponse
import com.tangem.grow.datasource.polymarket.clob.models.PolymarketPlaceOrderResponse
import com.tangem.grow.datasource.polymarket.clob.models.PolymarketPriceResponse
import com.tangem.grow.datasource.polymarket.geo.PolymarketGeoApi
import com.tangem.grow.datasource.polymarket.geo.models.PolymarketGeoblockResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketCategoriesResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketConfigurationResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketCategoryDto
import com.tangem.grow.datasource.polymarket.models.PolymarketEventDto
import com.tangem.grow.datasource.polymarket.models.PolymarketEventResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketEventsResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketSearchResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketWalletApprovalsRequest
import com.tangem.grow.datasource.polymarket.models.PolymarketWalletDeployRequest
import com.tangem.grow.datasource.polymarket.models.PolymarketWalletOperationResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketWalletStatusResponse
import com.tangem.grow.datasource.polymarket.relayer.PolymarketRelayerApi
import com.tangem.grow.datasource.polymarket.relayer.models.PolymarketNonceResponse
import com.tangem.domain.core.error.DataError
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.domain.polymarket.model.PolymarketCategory
import com.tangem.domain.polymarket.model.PolymarketEventError
import com.tangem.domain.polymarket.model.PolymarketEventsListConfig
import com.tangem.domain.polymarket.model.PolymarketSearchConfig
import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import com.tangem.domain.polymarket.model.PolymarketApprovalsBatch
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PolymarketBalanceAllowance
import com.tangem.domain.polymarket.model.PolymarketL1Headers
import com.tangem.domain.polymarket.model.PolymarketWalletError
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.domain.polymarket.model.PredictionOrder
import com.tangem.domain.polymarket.model.PredictionOrderState
import com.tangem.domain.polymarket.model.PredictionOrderStateError
import com.tangem.domain.polymarket.model.PredictionOrderStatus
import com.tangem.domain.polymarket.model.PredictionPlaceOrderError
import com.tangem.domain.polymarket.model.PredictionPlaceOrderResult
import com.tangem.domain.polymarket.model.PredictionPriceError
import com.tangem.domain.polymarket.model.PredictionSignedOrder
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchingContext
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CancellationException
import okhttp3.RequestBody
import okio.Buffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.math.BigInteger
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.Base64 as JavaBase64

internal class DefaultPolymarketRepositoryTest {

    private val api: PolymarketApi = mockk()
    private val geoApi: PolymarketGeoApi = mockk()
    private val relayerApi: PolymarketRelayerApi = mockk()
    private val clobApi: PolymarketClobApi = mockk()
    private val walletErrorResolver = PolymarketWalletErrorResolver(ProblemDetailParser(Moshi.Builder().build()))
    private val authErrorResolver = PolymarketAuthErrorResolver()
    private val eventErrorResolver = PolymarketEventErrorResolver()
    private val placeOrderErrorResolver = PredictionPlaceOrderErrorResolver()
    private val dispatchers = TestingCoroutineDispatcherProvider()
    private val jvmCodec = object : Base64UrlCodec {
        override fun decode(value: String): ByteArray = JavaBase64.getUrlDecoder().decode(value)
        override fun encode(bytes: ByteArray): String = JavaBase64.getUrlEncoder().encodeToString(bytes)
    }
    private val l2HeaderBuilder = PolymarketL2HeaderBuilder(PolymarketHmacSigner(jvmCodec))

    private val repository = DefaultPolymarketRepository(
        polymarketApi = api,
        geoApi = geoApi,
        relayerApi = relayerApi,
        clobApi = clobApi,
        walletErrorResolver = walletErrorResolver,
        authErrorResolver = authErrorResolver,
        eventErrorResolver = eventErrorResolver,
        placeOrderErrorResolver = placeOrderErrorResolver,
        l2HeaderBuilder = l2HeaderBuilder,
        dispatchers = dispatchers,
    )

    @Nested
    inner class GetBuilderCode {

        @Test
        fun `GIVEN the configuration is served WHEN asked twice THEN the network is hit once`() = runTest {
            // Arrange
            coEvery { api.getConfiguration(any()) } returns ApiResponse.Success(
                PolymarketConfigurationResponse(builderCode = BUILDER_CODE),
            )

            // Act
            val first = repository.getBuilderCode(walletId = WALLET_ID)
            val second = repository.getBuilderCode(walletId = WALLET_ID)

            // Assert
            assertThat(first.getOrNull()).isEqualTo(BUILDER_CODE)
            assertThat(second.getOrNull()).isEqualTo(BUILDER_CODE)
            coVerify(exactly = 1) { api.getConfiguration(WALLET_ID) }
        }

        @Test
        fun `GIVEN the configuration call fails WHEN asked again THEN it is retried rather than cached`() =
            runTest {
                // Arrange
                coEvery { api.getConfiguration(any()) } returns networkError()

                // Act
                val first = repository.getBuilderCode(walletId = WALLET_ID)
                val second = repository.getBuilderCode(walletId = WALLET_ID)

                // Assert
                assertThat(first.isLeft()).isTrue()
                assertThat(second.isLeft()).isTrue()
                coVerify(exactly = 2) { api.getConfiguration(WALLET_ID) }
            }
    }

    @Test
    fun `GIVEN categories response WHEN getCategories THEN maps to domain categories`() = runTest {
        // Arrange
        coEvery { api.getCategories(locale = null) } returns ApiResponse.Success(
            PolymarketCategoriesResponse(
                categories = listOf(
                    PolymarketCategoryDto(id = 1, label = "Trending", icon = "https://img/trending.png"),
                    PolymarketCategoryDto(id = 2, label = "Sport", icon = null),
                ),
            ),
        )

        // Act
        val result = repository.getCategories()

        // Assert
        assertThat(result).isEqualTo(
            listOf(
                PolymarketCategory(id = 1, label = "Trending", iconUrl = "https://img/trending.png"),
                PolymarketCategory(id = 2, label = "Sport", iconUrl = null),
            ).right(),
        )
    }

    @Test
    fun `GIVEN network exception WHEN getCategories THEN returns left no internet`() = runTest {
        // Arrange
        coEvery { api.getCategories(locale = null) } returns networkError()

        // Act
        val result = repository.getCategories()

        // Assert
        assertThat(result).isEqualTo(DataError.NetworkError.NoInternetConnection.left())
    }

    @Test
    fun `GIVEN event response WHEN getEvent THEN converts and returns right event`() = runTest {
        // Arrange
        coEvery { api.getEvent(eventId = "event-1") } returns ApiResponse.Success(
            PolymarketEventResponse(event = EVENT_DTO),
        )

        // Act
        val result = repository.getEvent(eventId = "event-1")

        // Assert
        val event = result.getOrNull()
        assertThat(event?.id).isEqualTo("event-id")
        assertThat(event?.title).isEqualTo("Event title")
    }

    @Test
    fun `GIVEN network exception WHEN getEvent THEN returns left network error`() = runTest {
        // Arrange
        coEvery { api.getEvent(eventId = "event-1") } returns networkError()

        // Act
        val result = repository.getEvent(eventId = "event-1")

        // Assert
        assertThat(result).isEqualTo(PolymarketEventError.Network.left())
    }

    @Test
    fun `GIVEN pages WHEN batch flow reloaded and scrolled THEN the body cursor drives the next page`() = runTest {
        // Arrange
        coEvery { api.getEvents(category = 5, limit = 20, cursor = null) } returns ApiResponse.Success(
            PolymarketEventsResponse(events = listOf(EVENT_DTO), cursor = "cursor-1", hasNext = true),
        )
        coEvery { api.getEvents(category = 5, limit = 20, cursor = "cursor-1") } returns ApiResponse.Success(
            PolymarketEventsResponse(events = listOf(EVENT_DTO), cursor = null, hasNext = false),
        )
        val actions = MutableSharedFlow<BatchAction<Int, PolymarketEventsListConfig, Nothing>>(replay = 1)
        val sourceScope = testSourceScope()
        val batchFlow = repository.getEventsBatchFlow(
            context = BatchingContext(actionsFlow = actions, coroutineScope = sourceScope),
            batchSize = 20,
        )

        // Act
        actions.emit(BatchAction.Reload(requestParams = PolymarketEventsListConfig(category = 5)))
        advanceUntilIdle()
        actions.emit(BatchAction.LoadMore())
        advanceUntilIdle()

        // Assert
        val events = batchFlow.state.value.data.flatMap { batch -> batch.data.events }
        assertThat(events.map { it.id }).containsExactly("event-id", "event-id")
        coVerify(exactly = 1) { api.getEvents(category = 5, limit = 20, cursor = "cursor-1") }
        sourceScope.cancel()
    }

    @Test
    fun `GIVEN failing first page WHEN batch flow reloaded THEN one silent retry precedes the error state`() =
        runTest {
            // Arrange
            coEvery { api.getEvents(category = null, limit = 20, cursor = null) } returns networkError()
            val repository = DefaultPolymarketRepository(
                polymarketApi = api,
                geoApi = geoApi,
                relayerApi = relayerApi,
                clobApi = clobApi,
                walletErrorResolver = walletErrorResolver,
                authErrorResolver = authErrorResolver,
                eventErrorResolver = eventErrorResolver,
                placeOrderErrorResolver = placeOrderErrorResolver,
                l2HeaderBuilder = l2HeaderBuilder,
                // The silent retry waits on a virtual-time dispatcher, so the test skips the 2s delay.
                dispatchers = TestingCoroutineDispatcherProvider(io = StandardTestDispatcher(testScheduler)),
            )
            val actions = MutableSharedFlow<BatchAction<Int, PolymarketEventsListConfig, Nothing>>(replay = 1)
            val sourceScope = testSourceScope()
            val batchFlow = repository.getEventsBatchFlow(
                context = BatchingContext(actionsFlow = actions, coroutineScope = sourceScope),
                batchSize = 20,
            )

            // Act
            actions.emit(BatchAction.Reload(requestParams = PolymarketEventsListConfig(category = null)))
            advanceUntilIdle()

            // Assert
            assertThat(batchFlow.state.value.status)
                .isInstanceOf(PaginationStatus.InitialLoadingError::class.java)
            coVerify(exactly = 2) { api.getEvents(category = null, limit = 20, cursor = null) }
            sourceScope.cancel()
        }

    @Test
    fun `GIVEN result pages WHEN search flow reloaded and scrolled THEN pages are requested by number`() = runTest {
        // Arrange
        coEvery { api.searchEvents(query = "uzb", limit = 20, page = 1) } returns ApiResponse.Success(
            PolymarketSearchResponse(events = listOf(EVENT_DTO), page = 1, total = 2, hasNext = true),
        )
        coEvery { api.searchEvents(query = "uzb", limit = 20, page = 2) } returns ApiResponse.Success(
            PolymarketSearchResponse(events = listOf(EVENT_DTO), page = 2, total = 2, hasNext = false),
        )
        val actions = MutableSharedFlow<BatchAction<Int, PolymarketSearchConfig, Nothing>>(replay = 1)
        val sourceScope = testSourceScope()
        val batchFlow = repository.searchEventsBatchFlow(
            context = BatchingContext(actionsFlow = actions, coroutineScope = sourceScope),
            batchSize = 20,
        )

        // Act
        actions.emit(BatchAction.Reload(requestParams = PolymarketSearchConfig(query = "uzb")))
        advanceUntilIdle()
        actions.emit(BatchAction.LoadMore())
        advanceUntilIdle()

        // Assert
        val events = batchFlow.state.value.data.flatMap { batch -> batch.data }
        assertThat(events.map { it.id }).containsExactly("event-id", "event-id")
        coVerify(exactly = 1) { api.searchEvents(query = "uzb", limit = 20, page = 2) }
        sourceScope.cancel()
    }

    @Test
    fun `GIVEN nothing found WHEN search flow reloaded THEN an empty last page is served without an error`() =
        runTest {
            // Arrange
            coEvery { api.searchEvents(query = "nothing", limit = 20, page = 1) } returns ApiResponse.Success(
                PolymarketSearchResponse(events = emptyList(), page = 1, total = 0, hasNext = false),
            )
            val actions = MutableSharedFlow<BatchAction<Int, PolymarketSearchConfig, Nothing>>(replay = 1)
            val sourceScope = testSourceScope()
            val batchFlow = repository.searchEventsBatchFlow(
                context = BatchingContext(actionsFlow = actions, coroutineScope = sourceScope),
                batchSize = 20,
            )

            // Act
            actions.emit(BatchAction.Reload(requestParams = PolymarketSearchConfig(query = "nothing")))
            advanceUntilIdle()

            // Assert
            val state = batchFlow.state.value
            assertThat(state.status).isInstanceOf(PaginationStatus.EndOfPagination::class.java)
            assertThat(state.data.flatMap { batch -> batch.data }).isEmpty()
            sourceScope.cancel()
        }

    @Test
    fun `GIVEN failing search WHEN search flow reloaded THEN the error is served without a retry`() = runTest {
        // Arrange
        coEvery { api.searchEvents(query = "boom", limit = 20, page = 1) } returns networkError()
        val actions = MutableSharedFlow<BatchAction<Int, PolymarketSearchConfig, Nothing>>(replay = 1)
        val sourceScope = testSourceScope()
        val batchFlow = repository.searchEventsBatchFlow(
            context = BatchingContext(actionsFlow = actions, coroutineScope = sourceScope),
            batchSize = 20,
        )

        // Act
        actions.emit(BatchAction.Reload(requestParams = PolymarketSearchConfig(query = "boom")))
        advanceUntilIdle()

        // Assert
        assertThat(batchFlow.state.value.status)
            .isInstanceOf(PaginationStatus.InitialLoadingError::class.java)
        coVerify(exactly = 1) { api.searchEvents(query = "boom", limit = 20, page = 1) }
        sourceScope.cancel()
    }

    /**
     * A scope for the batch source, driven by the scheduler of the test. Deliberately NOT [TestScope.backgroundScope]:
     * its tasks carry the background marker, which [advanceUntilIdle] does not run. The job is detached so the
     * source's never-completing collectors do not keep [runTest] waiting; cancel the scope at the end of the test.
     */
    private fun TestScope.testSourceScope(): CoroutineScope = CoroutineScope(coroutineContext + Job())

    @Test
    fun `GIVEN the event is gone WHEN getEvent THEN returns left not found`() = runTest {
        // Arrange
        coEvery { api.getEvent(eventId = "event-1") } returns httpError(code = Code.NOT_FOUND, body = null)

        // Act
        val result = repository.getEvent(eventId = "event-1")

        // Assert
        assertThat(result).isEqualTo(PolymarketEventError.NotFound.left())
    }

    @Test
    fun `GIVEN stored wallet WHEN getWalletStatus THEN maps to domain state`() = runTest {
        // Arrange
        coEvery { api.getWalletStatus(OWNER) } returns
            ApiResponse.Success(PolymarketWalletStatusResponse(depositWalletAddress = DW, status = "DEPLOYED"))

        // Act
        val result = repository.getWalletStatus(OWNER)

        // Assert
        assertThat(result).isEqualTo(PolymarketWalletState(DW, PolymarketWalletStatus.DEPLOYED).right())
    }

    @Test
    fun `GIVEN unknown status string WHEN getWalletStatus THEN maps to UNKNOWN`() = runTest {
        // Arrange
        coEvery { api.getWalletStatus(OWNER) } returns
            ApiResponse.Success(PolymarketWalletStatusResponse(depositWalletAddress = null, status = "SOME_FUTURE_STATE"))

        // Act
        val result = repository.getWalletStatus(OWNER)

        // Assert
        assertThat(result).isEqualTo(PolymarketWalletState(null, PolymarketWalletStatus.UNKNOWN).right())
    }

    @Test
    fun `GIVEN 409 WHEN getWalletStatus THEN WalletNotDeployed error (delegated to resolver)`() = runTest {
        // Arrange
        coEvery { api.getWalletStatus(OWNER) } returns httpError(Code.CONFLICT, body = null)

        // Act
        val result = repository.getWalletStatus(OWNER)

        // Assert
        assertThat(result).isEqualTo(PolymarketWalletError.WalletNotDeployed.left())
    }

    @Test
    fun `GIVEN network exception WHEN getWalletStatus THEN Network error`() = runTest {
        // Arrange
        coEvery { api.getWalletStatus(OWNER) } returns networkError()

        // Act
        val result = repository.getWalletStatus(OWNER)

        // Assert
        assertThat(result).isEqualTo(PolymarketWalletError.Network.left())
    }

    @Test
    fun `GIVEN cancellation WHEN getWalletStatus THEN it propagates and is not mapped to a domain error`() = runTest {
        // Arrange
        coEvery { api.getWalletStatus(OWNER) } throws CancellationException("cancelled")

        // Act
        val thrown = runCatching { repository.getWalletStatus(OWNER) }.exceptionOrNull()

        // Assert
        assertThat(thrown).isInstanceOf(CancellationException::class.java)
    }

    @Test
    fun `GIVEN accepted WHEN deployWallet THEN maps operation status`() = runTest {
        // Arrange
        val request = slot<PolymarketWalletDeployRequest>()
        coEvery { api.deployWallet(capture(request)) } returns
            ApiResponse.Success(PolymarketWalletOperationResponse(status = "DEPLOYMENT_IN_PROGRESS"))

        // Act
        val result = repository.deployWallet(
            ownerAddress = OWNER,
            walletId = WALLET_ID,
            depositWalletAddress = "0xDeF0000000000000000000000000000000000002",
        )

        // Assert
        assertThat(result).isEqualTo(PolymarketWalletStatus.DEPLOYMENT_IN_PROGRESS.right())
        assertThat(request.captured.ownerAddress).isEqualTo(OWNER)
        assertThat(request.captured.walletId).isEqualTo(WALLET_ID)
        assertThat(request.captured.depositWalletAddress).isEqualTo("0xDeF0000000000000000000000000000000000002")
    }

    @Test
    fun `GIVEN signed batch WHEN submitApprovals THEN sends converted request and maps status`() = runTest {
        // Arrange
        val requestSlot = slot<PolymarketWalletApprovalsRequest>()
        coEvery { api.submitApprovals(capture(requestSlot)) } returns
            ApiResponse.Success(PolymarketWalletOperationResponse(status = "READY_TO_TRADE"))
        val batch = PolymarketApprovalsBatch(
            ownerAddress = OWNER,
            depositWalletAddress = DW,
            nonce = "0",
            deadline = "1752346200",
            calls = listOf(PolymarketApprovalCall(target = "0xToken", value = "0", data = "0x095ea7b3")),
            signature = "0xbatchSig",
        )

        // Act
        val result = repository.submitApprovals(batch)

        // Assert
        assertThat(result).isEqualTo(PolymarketWalletStatus.READY_TO_TRADE.right())
        val sent = requestSlot.captured
        assertThat(sent.ownerAddress).isEqualTo(OWNER)
        assertThat(sent.depositWalletAddress).isEqualTo(DW)
        assertThat(sent.nonce).isEqualTo("0")
        assertThat(sent.calls).hasSize(1)
        assertThat(sent.calls.first().data).isEqualTo("0x095ea7b3")
        assertThat(sent.signature).isEqualTo("0xbatchSig")
    }

    @Test
    fun `GIVEN geoblock true WHEN checkGeoblock THEN returns right true`() = runTest {
        // Arrange
        coEvery { geoApi.getGeoblock() } returns ApiResponse.Success(PolymarketGeoblockResponse(blocked = true))

        // Act
        val actual = repository.checkGeoblock()

        // Assert
        assertThat(actual).isEqualTo(true.right())
    }

    @Test
    fun `GIVEN geo api throws WHEN checkGeoblock THEN returns left no internet`() = runTest {
        // Arrange
        coEvery { geoApi.getGeoblock() } returns networkError()

        // Act
        val actual = repository.checkGeoblock()

        // Assert
        assertThat(actual).isEqualTo(DataError.NetworkError.NoInternetConnection.left())
    }

    @Test
    fun `GIVEN nonce string WHEN getRelayerNonce THEN returns right BigInteger`() = runTest {
        // Arrange
        coEvery { relayerApi.getNonce(OWNER, "WALLET") } returns ApiResponse.Success(PolymarketNonceResponse(nonce = "7"))

        // Act
        val actual = repository.getRelayerNonce(OWNER)

        // Assert
        assertThat(actual).isEqualTo(BigInteger("7").right())
    }

    @Test
    fun `GIVEN relayer throws WHEN getRelayerNonce THEN returns left no internet`() = runTest {
        // Arrange
        coEvery { relayerApi.getNonce(OWNER, "WALLET") } returns networkError()

        // Act
        val actual = repository.getRelayerNonce(OWNER)

        // Assert
        assertThat(actual).isEqualTo(DataError.NetworkError.NoInternetConnection.left())
    }

    @Test
    fun `GIVEN malformed nonce WHEN getRelayerNonce THEN returns left no internet`() = runTest {
        // Arrange
        coEvery { relayerApi.getNonce(OWNER, "WALLET") } returns
            ApiResponse.Success(PolymarketNonceResponse(nonce = "not-a-number"))

        // Act
        val actual = repository.getRelayerNonce(OWNER)

        // Assert
        assertThat(actual).isEqualTo(DataError.NetworkError.NoInternetConnection.left())
    }

    @Test
    fun `GIVEN api key response WHEN deriveApiCredentials THEN returns right credentials`() = runTest {
        // Arrange
        coEvery { clobApi.deriveApiKey(HEADERS.toMap()) } returns
            ApiResponse.Success(PolymarketApiKeyResponse(apiKey = "k", secret = "s", passphrase = "p"))

        // Act
        val actual = repository.deriveApiCredentials(HEADERS)

        // Assert
        assertThat(actual).isEqualTo(PolymarketApiCredentials(apiKey = "k", secret = "s", passphrase = "p").right())
    }

    @Test
    fun `GIVEN 404 WHEN deriveApiCredentials THEN returns left KeyNotFound`() = runTest {
        // Arrange
        coEvery { clobApi.deriveApiKey(HEADERS.toMap()) } returns httpError(Code.NOT_FOUND, body = null)

        // Act
        val actual = repository.deriveApiCredentials(HEADERS)

        // Assert
        assertThat(actual).isEqualTo(PolymarketAuthError.KeyNotFound.left())
    }

    @Test
    fun `GIVEN api key response WHEN createApiCredentials THEN returns right credentials`() = runTest {
        // Arrange
        coEvery { clobApi.createApiKey(HEADERS.toMap()) } returns
            ApiResponse.Success(PolymarketApiKeyResponse(apiKey = "k", secret = "s", passphrase = "p"))

        // Act
        val actual = repository.createApiCredentials(HEADERS)

        // Assert
        assertThat(actual).isEqualTo(PolymarketApiCredentials(apiKey = "k", secret = "s", passphrase = "p").right())
    }

    @Test
    fun `GIVEN credentials WHEN syncBalanceAllowance THEN signs the path without query and sends the fixed params`() =
        runTest {
            // Arrange
            val headers = slot<Map<String, String>>()
            val assetType = slot<String>()
            val signatureType = slot<Int>()
            coEvery {
                clobApi.updateBalanceAllowance(capture(headers), capture(assetType), capture(signatureType))
            } returns ApiResponse.Success(Unit)

            // Act
            val result = repository.syncBalanceAllowance(ownerAddress = OWNER, credentials = SYNC_CREDENTIALS)

            // Assert
            assertThat(result).isEqualTo(Unit.right())
            assertThat(assetType.captured).isEqualTo("COLLATERAL")
            assertThat(signatureType.captured).isEqualTo(3)
            assertThat(headers.captured["POLY_ADDRESS"]).isEqualTo(OWNER)
            assertThat(headers.captured["POLY_API_KEY"]).isEqualTo(SYNC_CREDENTIALS.apiKey)
            assertThat(headers.captured["POLY_PASSPHRASE"]).isEqualTo(SYNC_CREDENTIALS.passphrase)
            val timestamp = headers.captured.getValue("POLY_TIMESTAMP")
            assertThat(headers.captured["POLY_SIGNATURE"])
                .isEqualTo(hmac(timestamp + "GET" + "/balance-allowance/update"))
            val nowSeconds = System.currentTimeMillis() / 1_000L
            assertThat(timestamp.toLong()).isAtLeast(nowSeconds - TIMESTAMP_TOLERANCE_SECONDS)
            assertThat(timestamp.toLong()).isAtMost(nowSeconds + TIMESTAMP_TOLERANCE_SECONDS)
        }

    @Test
    fun `GIVEN a signed order WHEN placeOrder THEN the signature covers exactly the bytes that are sent`() =
        runTest {
            // Arrange
            val headers = slot<Map<String, String>>()
            val body = slot<RequestBody>()
            coEvery { clobApi.placeOrder(capture(headers), capture(body)) } returns
                ApiResponse.Success(placeOrderResponse())

            // Act
            val result = repository.placeOrder(
                ownerAddress = OWNER,
                credentials = SYNC_CREDENTIALS,
                signedOrder = signedOrder(),
                isNegRisk = false,
            )

            // Assert
            assertThat(result.isRight()).isTrue()
            val sent = Buffer().also { body.captured.writeTo(it) }.readUtf8()
            val timestamp = headers.captured.getValue("POLY_TIMESTAMP")
            assertThat(headers.captured["POLY_SIGNATURE"]).isEqualTo(hmac(timestamp + "POST" + "/order" + sent))
            assertThat(sent).contains(""""orderType":"FAK"""")
            assertThat(sent).contains(""""owner":"k"""")
        }

    @Test
    fun `GIVEN a timeout WHEN placeOrder THEN the outcome is ambiguous rather than failed`() = runTest {
        // Arrange
        coEvery { clobApi.placeOrder(any(), any()) } returns timeoutError()

        // Act
        val result = repository.placeOrder(
            ownerAddress = OWNER,
            credentials = SYNC_CREDENTIALS,
            signedOrder = signedOrder(),
            isNegRisk = false,
        )

        // Assert
        assertThat(result.leftOrNull()).isInstanceOf(PredictionPlaceOrderError.Ambiguous::class.java)
    }

    @Test
    fun `GIVEN an order signed for the other exchange WHEN placeOrder THEN nothing is sent`() = runTest {
        // Arrange
        val standardExchangeOrder = signedOrder(signature = standardExchangeSignature())

        // Act
        val result = repository.placeOrder(
            ownerAddress = OWNER,
            credentials = SYNC_CREDENTIALS,
            signedOrder = standardExchangeOrder,
            isNegRisk = true,
        )

        // Assert
        assertThat(result.leftOrNull()).isInstanceOf(PredictionPlaceOrderError.NotSent::class.java)
        coVerify(exactly = 0) { clobApi.placeOrder(any(), any()) }
    }

    @Test
    fun `GIVEN a refusal inside a 2xx WHEN placeOrder THEN it is a rejection and not a result`() = runTest {
        // Arrange
        coEvery { clobApi.placeOrder(any(), any()) } returns ApiResponse.Success(
            placeOrderResponse(isSuccess = false, errorMessage = "not enough balance"),
        )

        // Act
        val result = repository.placeOrder(
            ownerAddress = OWNER,
            credentials = SYNC_CREDENTIALS,
            signedOrder = signedOrder(),
            isNegRisk = false,
        )

        // Assert
        assertThat(result.leftOrNull())
            .isEqualTo(PredictionPlaceOrderError.Rejected(detail = "not enough balance"))
    }

    @Test
    fun `GIVEN a 5xx WHEN placeOrder THEN the outcome is ambiguous rather than retryable`() = runTest {
        // Arrange
        coEvery { clobApi.placeOrder(any(), any()) } returns
            httpError(code = Code.INTERNAL_SERVER_ERROR, body = "upstream timeout")

        // Act
        val result = repository.placeOrder(
            ownerAddress = OWNER,
            credentials = SYNC_CREDENTIALS,
            signedOrder = signedOrder(),
            isNegRisk = false,
        )

        // Assert
        assertThat(result.leftOrNull()).isInstanceOf(PredictionPlaceOrderError.Ambiguous::class.java)
    }

    @Test
    fun `GIVEN a filled response WHEN placeOrder THEN the fill amounts come from the exchange`() = runTest {
        // Arrange
        coEvery { clobApi.placeOrder(any(), any()) } returns ApiResponse.Success(
            placeOrderResponse(status = "matched", makingAmount = "36.00", takingAmount = "48.97959"),
        )

        // Act
        val result = repository.placeOrder(
            ownerAddress = OWNER,
            credentials = SYNC_CREDENTIALS,
            signedOrder = signedOrder(),
            isNegRisk = false,
        )

        // Assert
        assertThat(result).isEqualTo(
            PredictionPlaceOrderResult(
                isSuccess = true,
                orderId = "0xorder",
                status = "matched",
                makingAmount = BigDecimal("36.00"),
                takingAmount = BigDecimal("48.97959"),
                transactionHashes = listOf("0xtx"),
                errorMessage = null,
            ).right(),
        )
    }

    @Test
    fun `GIVEN a 401 WHEN syncBalanceAllowance THEN maps to InvalidSignature`() = runTest {
        // Arrange
        coEvery { clobApi.updateBalanceAllowance(any(), any(), any()) } returns httpError(Code.UNAUTHORIZED, body = null)

        // Act
        val result = repository.syncBalanceAllowance(ownerAddress = OWNER, credentials = SYNC_CREDENTIALS)

        // Assert
        assertThat(result).isEqualTo(PolymarketAuthError.InvalidSignature.left())
    }

    @Test
    fun `GIVEN a secret with non-url-safe base64 characters WHEN syncBalanceAllowance THEN returns Unknown without calling the api`() =
        runTest {
            // Arrange
            val credentials = SYNC_CREDENTIALS.copy(secret = "abc+def/==")

            // Act
            val result = repository.syncBalanceAllowance(ownerAddress = OWNER, credentials = credentials)

            // Assert
            val error = result.leftOrNull() as? PolymarketAuthError.Unknown
            assertThat(error).isNotNull()
            assertThat(error?.httpCode).isNull()
            assertThat(error?.detail).isNotNull()
            coVerify(exactly = 0) { clobApi.updateBalanceAllowance(any(), any(), any()) }
        }

    @Test
    fun `GIVEN an empty secret WHEN syncBalanceAllowance THEN returns Unknown without calling the api`() = runTest {
        // Arrange
        val credentials = SYNC_CREDENTIALS.copy(secret = "")

        // Act
        val result = repository.syncBalanceAllowance(ownerAddress = OWNER, credentials = credentials)

        // Assert
        val error = result.leftOrNull() as? PolymarketAuthError.Unknown
        assertThat(error).isNotNull()
        assertThat(error?.httpCode).isNull()
        assertThat(error?.detail).isNotNull()
        coVerify(exactly = 0) { clobApi.updateBalanceAllowance(any(), any(), any()) }
    }

    @Test
    fun `GIVEN credentials WHEN getOrder THEN signs the path with the id and reads the fill`() = runTest {
        // Arrange
        val headers = slot<Map<String, String>>()
        coEvery { clobApi.getOrder(capture(headers), ORDER_ID) } returns ApiResponse.Success(
            PolymarketOrderResponse(
                id = ORDER_ID,
                status = "ORDER_STATUS_MATCHED",
                price = "0.52",
                originalSize = "100000000",
                sizeMatched = "37500000",
            ),
        )

        // Act
        val result = repository.getOrder(ownerAddress = OWNER, credentials = SYNC_CREDENTIALS, orderId = ORDER_ID)

        // Assert
        assertThat(result).isEqualTo(
            PredictionOrderState(
                status = PredictionOrderStatus.Matched,
                price = BigDecimal("0.52"),
                originalSize = BigDecimal("100.000000"),
                sizeMatched = BigDecimal("37.500000"),
            ).right(),
        )
        val timestamp = headers.captured.getValue("POLY_TIMESTAMP")
        assertThat(headers.captured["POLY_SIGNATURE"]).isEqualTo(hmac(timestamp + "GET" + "/data/order/" + ORDER_ID))
    }

    @Test
    fun `GIVEN the book does not know the id WHEN getOrder THEN reports not found`() = runTest {
        // Arrange
        coEvery { clobApi.getOrder(any(), ORDER_ID) } returns httpError(code = Code.NOT_FOUND, body = "order not found")

        // Act
        val result = repository.getOrder(ownerAddress = OWNER, credentials = SYNC_CREDENTIALS, orderId = ORDER_ID)

        // Assert
        assertThat(result).isEqualTo(PredictionOrderStateError.NotFound.left())
    }

    @Test
    fun `GIVEN refused credentials WHEN getOrder THEN reports unauthorized with the body`() = runTest {
        // Arrange
        coEvery { clobApi.getOrder(any(), ORDER_ID) } returns httpError(code = Code.UNAUTHORIZED, body = "bad sig")

        // Act
        val result = repository.getOrder(ownerAddress = OWNER, credentials = SYNC_CREDENTIALS, orderId = ORDER_ID)

        // Assert
        assertThat(result).isEqualTo(PredictionOrderStateError.Unauthorized(detail = "bad sig").left())
    }

    @Test
    fun `GIVEN a timeout WHEN getOrder THEN reports a timeout apart from other failures`() = runTest {
        // Arrange
        coEvery { clobApi.getOrder(any(), ORDER_ID) } returns timeoutError()

        // Act
        val result = repository.getOrder(ownerAddress = OWNER, credentials = SYNC_CREDENTIALS, orderId = ORDER_ID)

        // Assert
        assertThat(result).isEqualTo(PredictionOrderStateError.Timeout.left())
    }

    @Test
    fun `GIVEN a 5xx WHEN getOrder THEN reports unknown with the code`() = runTest {
        // Arrange
        coEvery { clobApi.getOrder(any(), ORDER_ID) } returns
            httpError(code = Code.INTERNAL_SERVER_ERROR, body = "upstream")

        // Act
        val result = repository.getOrder(ownerAddress = OWNER, credentials = SYNC_CREDENTIALS, orderId = ORDER_ID)

        // Assert
        assertThat(result).isEqualTo(PredictionOrderStateError.Unknown(httpCode = 500, detail = "upstream").left())
    }

    internal data class PriceModel(
        val side: PredictionOrderSide,
        val price: String,
        val expected: Either<PredictionPriceError, BigDecimal>,
    ) {
        override fun toString(): String = "$side answers $price"
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetPrice {

        @ParameterizedTest
        @ProvideTestModels
        fun getPrice(model: PriceModel) = runTest {
            // Arrange
            coEvery { clobApi.getPrice(tokenId = ASSET_ID, side = model.side.name) } returns
                ApiResponse.Success(PolymarketPriceResponse(price = model.price))

            // Act
            val result = repository.getPrice(assetId = ASSET_ID, side = model.side)

            // Assert
            assertThat(result).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            PriceModel(
                side = PredictionOrderSide.BUY,
                price = "0",
                expected = PredictionPriceError.EmptyBook.left(),
            ),
            PriceModel(
                side = PredictionOrderSide.BUY,
                price = "0.000",
                expected = PredictionPriceError.EmptyBook.left(),
            ),
            PriceModel(
                side = PredictionOrderSide.SELL,
                price = "1",
                expected = PredictionPriceError.EmptyBook.left(),
            ),
            PriceModel(
                side = PredictionOrderSide.SELL,
                price = "1.000",
                expected = PredictionPriceError.EmptyBook.left(),
            ),
            PriceModel(
                side = PredictionOrderSide.BUY,
                price = "0.001",
                expected = BigDecimal("0.001").right(),
            ),
            PriceModel(
                side = PredictionOrderSide.SELL,
                price = "0.999",
                expected = BigDecimal("0.999").right(),
            ),
            PriceModel(
                side = PredictionOrderSide.BUY,
                price = "1",
                expected = BigDecimal("1").right(),
            ),
            PriceModel(
                side = PredictionOrderSide.SELL,
                price = "0",
                expected = BigDecimal("0").right(),
            ),
        )

        @Test
        fun `GIVEN an unreadable price WHEN getPrice THEN reports unknown`() = runTest {
            // Arrange
            coEvery { clobApi.getPrice(tokenId = ASSET_ID, side = any()) } returns
                ApiResponse.Success(PolymarketPriceResponse(price = "not a number"))

            // Act
            val result = repository.getPrice(assetId = ASSET_ID, side = PredictionOrderSide.BUY)

            // Assert
            assertThat(result.leftOrNull()).isInstanceOf(PredictionPriceError.Unknown::class.java)
        }

        @Test
        fun `GIVEN no orderbook WHEN getPrice THEN reports a closed market apart from an empty book`() = runTest {
            // Arrange
            coEvery { clobApi.getPrice(tokenId = ASSET_ID, side = any()) } returns
                httpError(code = Code.NOT_FOUND, body = null)

            // Act
            val result = repository.getPrice(assetId = ASSET_ID, side = PredictionOrderSide.BUY)

            // Assert
            assertThat(result).isEqualTo(PredictionPriceError.MarketClosed.left())
        }
    }

    @Test
    fun `GIVEN credentials WHEN getBalanceAllowance THEN signs the read path and converts the base units`() = runTest {
        // Arrange
        val headers = slot<Map<String, String>>()
        val assetType = slot<String>()
        val signatureType = slot<Int>()
        coEvery {
            clobApi.getBalanceAllowance(capture(headers), capture(assetType), capture(signatureType))
        } returns ApiResponse.Success(
            PolymarketBalanceAllowanceResponse(balance = "12340000", allowances = mapOf(SPENDER to "1000000")),
        )

        // Act
        val result = repository.getBalanceAllowance(ownerAddress = OWNER, credentials = SYNC_CREDENTIALS)

        // Assert
        assertThat(result).isEqualTo(
            PolymarketBalanceAllowance(balance = BigDecimal("12.34"), allowances = mapOf(SPENDER to BigDecimal("1"))).right(),
        )
        assertThat(assetType.captured).isEqualTo("COLLATERAL")
        assertThat(signatureType.captured).isEqualTo(3)
        val timestamp = headers.captured.getValue("POLY_TIMESTAMP")
        assertThat(headers.captured["POLY_SIGNATURE"]).isEqualTo(hmac(timestamp + "GET" + "/balance-allowance"))
    }

    @Test
    fun `GIVEN no allowance reported WHEN getBalanceAllowance THEN leaves it absent rather than zero`() = runTest {
        // Arrange
        coEvery { clobApi.getBalanceAllowance(any(), any(), any()) } returns ApiResponse.Success(
            PolymarketBalanceAllowanceResponse(balance = "12340000", allowances = null),
        )

        // Act
        val result = repository.getBalanceAllowance(ownerAddress = OWNER, credentials = SYNC_CREDENTIALS)

        // Assert
        assertThat(result).isEqualTo(
            PolymarketBalanceAllowance(balance = BigDecimal("12.34"), allowances = null).right(),
        )
    }

    @Test
    fun `GIVEN an empty wallet WHEN getBalanceAllowance THEN the balance equals zero rather than merely scaling to it`() =
        runTest {
            // Arrange
            coEvery { clobApi.getBalanceAllowance(any(), any(), any()) } returns ApiResponse.Success(
                PolymarketBalanceAllowanceResponse(balance = "0", allowances = mapOf(SPENDER to "1000000000")),
            )

            // Act
            val result = repository.getBalanceAllowance(ownerAddress = OWNER, credentials = SYNC_CREDENTIALS)

            // Assert
            assertThat(result).isEqualTo(
                PolymarketBalanceAllowance(
                    balance = BigDecimal.ZERO,
                    allowances = mapOf(SPENDER to BigDecimal("1000")),
                ).right(),
            )
        }

    @Test
    fun `GIVEN a spender with no allowance WHEN getBalanceAllowance THEN the zero survives as itself`() = runTest {
        // Arrange
        coEvery { clobApi.getBalanceAllowance(any(), any(), any()) } returns ApiResponse.Success(
            PolymarketBalanceAllowanceResponse(
                balance = "2950011",
                allowances = mapOf(SPENDER to "1000000000", "0xD91E80cF" to "0"),
            ),
        )

        // Act
        val result = repository.getBalanceAllowance(ownerAddress = OWNER, credentials = SYNC_CREDENTIALS)

        // Assert
        assertThat(result.getOrNull()?.allowances)
            .containsExactly(SPENDER, BigDecimal("1000"), "0xd91e80cf", BigDecimal.ZERO)
    }

    @Test
    fun `GIVEN an unparsable balance WHEN getBalanceAllowance THEN returns Unknown instead of a wrong amount`() =
        runTest {
            // Arrange
            coEvery { clobApi.getBalanceAllowance(any(), any(), any()) } returns ApiResponse.Success(
                PolymarketBalanceAllowanceResponse(balance = "not-a-number", allowances = null),
            )

            // Act
            val result = repository.getBalanceAllowance(ownerAddress = OWNER, credentials = SYNC_CREDENTIALS)

            // Assert
            assertThat(result.leftOrNull()).isInstanceOf(PolymarketAuthError.Unknown::class.java)
        }

    @Test
    fun `GIVEN a 401 WHEN getBalanceAllowance THEN maps to InvalidSignature`() = runTest {
        // Arrange
        coEvery { clobApi.getBalanceAllowance(any(), any(), any()) } returns httpError(Code.UNAUTHORIZED, body = null)

        // Act
        val result = repository.getBalanceAllowance(ownerAddress = OWNER, credentials = SYNC_CREDENTIALS)

        // Assert
        assertThat(result).isEqualTo(PolymarketAuthError.InvalidSignature.left())
    }

    private fun hmac(message: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(JavaBase64.getUrlDecoder().decode(SYNC_CREDENTIALS.secret), "HmacSHA256"))
        return JavaBase64.getUrlEncoder().encodeToString(mac.doFinal(message.toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun `GIVEN a priced quote WHEN getOrderQuote THEN the figures arrive as the exchange stated them`() = runTest {
        // Arrange
        coEvery { api.getOrderQuote(request = any()) } returns ApiResponse.Success(quoteResponse())

        // Act
        val result = repository.getOrderQuote(request = quoteRequest())

        // Assert
        val quote = result.getOrNull()!!
        assertThat(quote.status).isEqualTo(PredictionQuoteStatus.FULL)
        assertThat(quote.shares).isEqualTo(BigDecimal("40.81632"))
        assertThat(quote.notional).isEqualTo(BigDecimal("2.00"))
        assertThat(quote.total).isEqualTo(BigDecimal("2.05000"))
        assertThat(quote.worstCasePrice).isEqualTo(BigDecimal("0.049"))
        assertThat(quote.isLive).isFalse()
    }

    @Test
    fun `GIVEN an unreadable amount WHEN getOrderQuote THEN it fails instead of escaping as an exception`() =
        runTest {
            // Arrange — safeApiCall recovers a raise, not a throw, so an unparsable figure would otherwise
            coEvery { api.getOrderQuote(request = any()) } returns
                ApiResponse.Success(quoteResponse(shares = "not-a-number"))

            // Act
            val result = repository.getOrderQuote(request = quoteRequest())

            // Assert
            assertThat(result.leftOrNull()).isInstanceOf(PredictionOrderQuoteError.Unknown::class.java)
        }

    @Test
    fun `GIVEN a quote request WHEN getOrderQuote THEN the market and the outcome are sent as asked`() = runTest {
        // Arrange
        val body = slot<PolymarketOrderQuoteRequest>()
        coEvery { api.getOrderQuote(request = capture(body)) } returns ApiResponse.Success(quoteResponse())

        // Act
        repository.getOrderQuote(request = quoteRequest())

        // Assert — a swapped market and outcome would quote the opposite bet instead of failing
        assertThat(body.captured.marketId).isEqualTo("2944989")
        assertThat(body.captured.assetId).isEqualTo("1116047")
    }

    @Test
    fun `GIVEN a refusal from the endpoint WHEN getOrderQuote THEN the status survives for the poller`() = runTest {
        // Arrange — a deterministic 400 repeats on every tick, unlike a transient 5xx
        coEvery { api.getOrderQuote(request = any()) } returns httpError(Code.BAD_REQUEST, body = "bad amount")

        // Act
        val result = repository.getOrderQuote(request = quoteRequest())

        // Assert
        assertThat(result.leftOrNull())
            .isEqualTo(PredictionOrderQuoteError.Unknown(httpCode = 400, detail = "bad amount"))
    }

    @Test
    fun `GIVEN an unclassifiable failure WHEN getOrderQuote THEN its cause is carried`() = runTest {
        // Arrange
        coEvery { api.getOrderQuote(request = any()) } returns
            (ApiResponse.Error(ApiResponseError.UnknownException(IllegalStateException("boom"))) as ApiResponse<PolymarketOrderQuoteResponse>)

        // Act
        val result = repository.getOrderQuote(request = quoteRequest())

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(PredictionOrderQuoteError.Unknown(httpCode = null, detail = "boom"))
    }

    @Test
    fun `GIVEN no connection WHEN getOrderQuote THEN the failure keeps its kind`() = runTest {
        // Arrange
        coEvery { api.getOrderQuote(request = any()) } returns networkError()

        // Act
        val result = repository.getOrderQuote(request = quoteRequest())

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(PredictionOrderQuoteError.Network)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> httpError(code: Code, body: String?): ApiResponse<T> =
        ApiResponse.Error(
            ApiResponseError.HttpException(code = code, message = "error", errorBody = body),
        ) as ApiResponse<T>

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> networkError(): ApiResponse<T> =
        ApiResponse.Error(ApiResponseError.NetworkException()) as ApiResponse<T>

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> timeoutError(): ApiResponse<T> =
        ApiResponse.Error(ApiResponseError.TimeoutException()) as ApiResponse<T>

    private companion object {
        const val SPENDER = "0xe111180000d2663c0091e4f400237545b87b996b"
        const val TIMESTAMP_TOLERANCE_SECONDS = 60L
        const val OWNER = "0xAbC0000000000000000000000000000000000001"
        const val ORDER_ID = "0x6b2e"
        const val ASSET_ID = "1116047"
        const val WALLET_ID = "7CE25DC32EF792CFC32380007A4172F5B64F67E4F91D37F14B351A76DAFA33DA"
        const val BUILDER_CODE = "0x0000000000000000000000000000000000000000000000000000000000000001"
        const val DW = "0xDEf0000000000000000000000000000000000002"
        val HEADERS = PolymarketL1Headers(address = "0xabc", signature = "0xsig", timestamp = "1700", nonce = "0")
        fun quoteRequest() = PredictionOrderQuoteRequest(
            marketId = "2944989",
            assetId = "1116047",
            side = PredictionOrderSide.BUY,
            amount = BigDecimal("2"),
            slippagePercent = BigDecimal("3"),
        )

        fun quoteResponse(
            shares: String = "40.81632",
            status: String = "FULL",
            notional: String = "2.00",
            worstCasePrice: String = "0.049",
            total: String = "2.05000",
        ) = PolymarketOrderQuoteResponse(
            status = status,
            side = "BUY",
            shares = shares,
            notional = notional,
            expectedExecutionAmount = "41.66666",
            averagePrice = "0.048",
            worstCasePrice = worstCasePrice,
            fees = PolymarketOrderQuoteFeesResponse(market = "0.00000", builder = "0.05000", total = "0.05000"),
            total = total,
            builderCode = "0xbuilder",
            minOrderSize = "5",
            minOrderAmount = "1",
            tickSize = "0.001",
            isLive = false,
        )

        fun standardExchangeSignature(): String = "0x" + "11".repeat(n = 65) +
            PolymarketOrderTypedDataBuilder.domainSeparator(isNegRisk = false)
                .joinToString(separator = "") { byte -> "%02x".format(byte) }

        fun signedOrder(
            signature: String = standardExchangeSignature(),
            salt: Long = 1_234_567_890L,
        ): PredictionSignedOrder =
            PredictionSignedOrder(
            order = PredictionOrder(
                salt = salt,
                maker = "0xMaker",
                signer = "0xSigner",
                tokenId = BigInteger("7"),
                makerAmount = BigInteger("72000000"),
                takerAmount = BigInteger("97959180"),
                side = PredictionOrderSide.BUY,
                signatureType = 3,
                timestampMillis = 1_787_300_000_000L,
                metadata = "0xmeta",
                builder = "0xbuilder",
            ),
            signature = signature,
        )

        fun placeOrderResponse(
            status: String? = "live",
            makingAmount: String? = null,
            takingAmount: String? = null,
            isSuccess: Boolean = true,
            errorMessage: String? = null,
        ): PolymarketPlaceOrderResponse = PolymarketPlaceOrderResponse(
            isSuccess = isSuccess,
            orderId = "0xorder",
            status = status,
            makingAmount = makingAmount,
            takingAmount = takingAmount,
            transactionHashes = listOf("0xtx"),
            errorMessage = errorMessage,
        )

        val SYNC_CREDENTIALS = PolymarketApiCredentials(
            apiKey = "k",
            secret = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=",
            passphrase = "p",
        )

        val EVENT_DTO = PolymarketEventDto(
            eventId = "event-id",
            slug = "event-slug",
            title = "Event title",
            description = "Event description",
            polymarketRulesUrl = "https://polymarket.com/rules",
            icon = null,
            image = null,
            status = "active",
            startDate = null,
            endDate = null,
            volume = null,
            volume24hr = null,
            liquidity = null,
            totalMarketsCount = 0,
            displayMode = "plain_markets",
            markets = emptyList(),
            isNegRisk = false,
        )
    }
}