package com.tangem.data.collectibles

import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.domain.collectibles.CollectibleGachaMachinesBatchAction
import com.tangem.data.collectibles.error.CollectiblesErrorResolver
import com.tangem.data.collectibles.store.CollectiblesMachineStore
import com.tangem.domain.collectibles.models.CollectibleBuybackOffer
import com.tangem.domain.collectibles.models.CollectibleBuybackStatus
import com.tangem.domain.collectibles.models.CollectibleBuybackTx
import com.tangem.domain.collectibles.models.CollectibleException
import com.tangem.domain.collectibles.models.CollectiblePurchaseTx
import com.tangem.domain.collectibles.models.CollectibleRarity
import com.tangem.domain.collectibles.models.CollectibleTurboMode
import com.tangem.domain.collectibles.models.CollectibleTxState
import com.tangem.domain.collectibles.models.CollectibleTxStatus
import com.tangem.domain.collectibles.models.GenerateBuybackTxParams
import com.tangem.domain.collectibles.models.GeneratePurchaseTxParams
import com.tangem.domain.collectibles.models.OpenedCollectiblePack
import com.tangem.grow.datasource.collectibles.CollectiblesApi
import com.tangem.grow.datasource.collectibles.models.request.BuybackTxSentRequestBody
import com.tangem.grow.datasource.collectibles.models.request.CollectibleTxRequestBody
import com.tangem.grow.datasource.collectibles.models.request.GenerateBuybackDataRequestBody
import com.tangem.grow.datasource.collectibles.models.request.GeneratePurchaseTxRequestBody
import com.tangem.grow.datasource.collectibles.models.request.PurchaseTxSentRequestBody
import com.tangem.grow.datasource.collectibles.models.response.BuybackAvailabilityResponse
import com.tangem.grow.datasource.collectibles.models.response.BuybackStatusResponse
import com.tangem.grow.datasource.collectibles.models.response.BuybackStatusResponse.BuybackStatusDTO
import com.tangem.grow.datasource.collectibles.models.response.CollectibleTxResponse
import com.tangem.grow.datasource.collectibles.models.response.GenerateBuybackDataResponse
import com.tangem.grow.datasource.collectibles.models.response.GeneratePurchaseTxResponse
import com.tangem.grow.datasource.collectibles.models.response.MachineCategoriesResponse
import com.tangem.grow.datasource.collectibles.models.response.MachinesResponse
import com.tangem.grow.datasource.collectibles.models.response.model.MachineDTO
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchingContext
import com.tangem.pagination.PaginationStatus
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class DefaultCollectiblesRepositoryTest {

    private val collectiblesApi: CollectiblesApi = mockk()

    // The repository caches machines in memory, so both it and its store are rebuilt per test — a machine
    // stored by one test would otherwise satisfy the next one without reaching the api.
    private lateinit var repository: DefaultCollectiblesRepository

    @BeforeEach
    fun resetMocks() {
        repository = DefaultCollectiblesRepository(
            collectiblesApi = collectiblesApi,
            machineStore = CollectiblesMachineStore(store = RuntimeSharedStore()),
            errorResolver = CollectiblesErrorResolver(json = NetworkJsonFactory.create()),
            dispatchers = TestingCoroutineDispatcherProvider(),
        )
        clearMocks(collectiblesApi)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetCategories {

        @Test
        fun `GIVEN api returns categories WHEN getCategories THEN maps to domain`() = runTest {
            // Arrange
            coEvery { collectiblesApi.getCategories() } returns ApiResponse.Success(
                MachineFactory.createCategoriesResponse(
                    categories = listOf(MachineFactory.createCategoryDto(id = "pokemons", name = "Pokemons")),
                    totalMachinesQuantity = 47,
                ),
            )

            // Act
            val actual = repository.getCategories()

            // Assert
            assertThat(actual.totalMachinesQuantity).isEqualTo(47)
            assertThat(actual.categories.single().name).isEqualTo("Pokemons")
        }

        @Test
        fun `GIVEN api fails WHEN getCategories THEN the api error is thrown`() = runTest {
            // Arrange
            coEvery { collectiblesApi.getCategories() } returns timeoutError<MachineCategoriesResponse>()

            // Act
            val actual = runCatching { repository.getCategories() }.exceptionOrNull()

            // Assert
            assertThat(actual).isInstanceOf(ApiResponseError.TimeoutException::class.java)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetMachine {

        @Test
        fun `GIVEN api returns machine WHEN getMachine THEN maps every tier`() = runTest {
            // Arrange
            coEvery { collectiblesApi.getMachine(any()) } returns ApiResponse.Success(MachineFactory.createDto())

            // Act
            val actual = repository.getMachine(externalId = "pokemon_50")

            // Assert
            assertThat(actual).isEqualTo(MachineFactory.create())
            coVerify(exactly = 1) { collectiblesApi.getMachine("pokemon_50") }
        }

        @Test
        fun `GIVEN a machine already read WHEN getMachine THEN the api is not called again`() = runTest {
            // Arrange
            coEvery { collectiblesApi.getMachine(any()) } returns ApiResponse.Success(MachineFactory.createDto())
            repository.getMachine(externalId = "pokemon_50")

            // Act
            val actual = repository.getMachine(externalId = "pokemon_50")

            // Assert
            assertThat(actual).isEqualTo(MachineFactory.create())
            coVerify(exactly = 1) { collectiblesApi.getMachine("pokemon_50") }
        }

        @Test
        fun `GIVEN a cached machine WHEN getMachine with skipCache THEN the api is called again`() = runTest {
            // Arrange
            coEvery { collectiblesApi.getMachine(any()) } returnsMany listOf(
                ApiResponse.Success(MachineFactory.createDto()),
                ApiResponse.Success(MachineFactory.createDto(price = BigDecimal("7500"))),
            )
            repository.getMachine(externalId = "pokemon_50")

            // Act
            val actual = repository.getMachine(externalId = "pokemon_50", skipCache = true)

            // Assert
            assertThat(actual).isEqualTo(MachineFactory.create(price = BigDecimal("75.00")))
            coVerify(exactly = 2) { collectiblesApi.getMachine("pokemon_50") }
        }

        @Test
        fun `GIVEN a refreshed machine WHEN getMachine THEN the refreshed one is served from the cache`() = runTest {
            // Arrange
            coEvery { collectiblesApi.getMachine(any()) } returnsMany listOf(
                ApiResponse.Success(MachineFactory.createDto()),
                ApiResponse.Success(MachineFactory.createDto(price = BigDecimal("7500"))),
            )
            repository.getMachine(externalId = "pokemon_50")
            repository.getMachine(externalId = "pokemon_50", skipCache = true)

            // Act
            val actual = repository.getMachine(externalId = "pokemon_50")

            // Assert
            assertThat(actual).isEqualTo(MachineFactory.create(price = BigDecimal("75.00")))
            coVerify(exactly = 2) { collectiblesApi.getMachine("pokemon_50") }
        }

        @Test
        fun `GIVEN the catalogue was browsed WHEN getMachine THEN it is served without a request`() = runTest {
            // Arrange
            coEvery {
                collectiblesApi.getMachines(categoryId = null, search = null, limit = 20, cursor = null)
            } returns ApiResponse.Success(MachineFactory.createMachinesResponse())
            val actions = MutableSharedFlow<CollectibleGachaMachinesBatchAction>(replay = 1)
            val sourceScope = testSourceScope()
            repository.getMachinesBatchFlow(
                context = BatchingContext(actionsFlow = actions, coroutineScope = sourceScope),
                batchSize = 20,
            )
            actions.emit(BatchAction.Reload(requestParams = MachineFactory.createListConfig()))
            advanceUntilIdle()

            // Act
            val actual = repository.getMachine(externalId = "pokemon_50")

            // Assert
            assertThat(actual).isEqualTo(MachineFactory.create())
            coVerify(exactly = 0) { collectiblesApi.getMachine(any()) }
            sourceScope.cancel()
        }

        @Test
        fun `GIVEN api fails WHEN getMachine THEN the api error is thrown`() = runTest {
            // Arrange
            coEvery { collectiblesApi.getMachine(any()) } returns networkError<MachineDTO>()

            // Act
            val actual = runCatching { repository.getMachine(externalId = "pokemon_50") }.exceptionOrNull()

            // Assert
            assertThat(actual).isInstanceOf(ApiResponseError.NetworkException::class.java)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GeneratePurchaseTx {

        @Test
        fun `GIVEN turbo mode WHEN generatePurchaseTx THEN body carries turbo params and memo becomes the join key`() =
            runTest {
                // Arrange
                coEvery { collectiblesApi.generatePurchaseTx(any(), any()) } returns
                    ApiResponse.Success(purchaseTxResponse())

                // Act
                val actual = repository.generatePurchaseTx(
                    userWalletId = userWalletId,
                    params = GeneratePurchaseTxParams(
                        fromAddress = "from",
                        machineId = "pokemon_50",
                        toAddress = "to",
                        collectibleTurboMode = CollectibleTurboMode(isEnabled = true, buybackAddress = "buyback"),
                    ),
                )

                // Assert
                assertThat(actual).isEqualTo(
                    CollectiblePurchaseTx(
                        txId = "tx-id",
                        memo = "cc-uuid",
                        amount = 50_000_000L,
                        feeAmount = 1_000_000L,
                    ),
                )
                coVerify(exactly = 1) {
                    collectiblesApi.generatePurchaseTx(
                        userWalletId = userWalletId.stringValue,
                        body = GeneratePurchaseTxRequestBody(
                            fromAddress = "from",
                            machineId = "pokemon_50",
                            toAddress = "to",
                            turboMode = GeneratePurchaseTxRequestBody.TurboModeDTO(
                                isEnabled = true,
                                buybackAddress = "buyback",
                            ),
                        ),
                    )
                }
            }

        @Test
        fun `GIVEN api fails WHEN generatePurchaseTx THEN the api error is thrown`() = runTest {
            // Arrange
            coEvery { collectiblesApi.generatePurchaseTx(any(), any()) } returns
                timeoutError<GeneratePurchaseTxResponse>()

            // Act
            val actual = runCatching {
                repository.generatePurchaseTx(
                    userWalletId = userWalletId,
                    params = GeneratePurchaseTxParams(fromAddress = "from", machineId = "pokemon_50", toAddress = "to"),
                )
            }.exceptionOrNull()

            // Assert
            assertThat(actual).isInstanceOf(ApiResponseError.TimeoutException::class.java)
        }

        @Test
        fun `GIVEN low inventory code WHEN generatePurchaseTx THEN low inventory is thrown`() = runTest {
            // Arrange
            coEvery { collectiblesApi.generatePurchaseTx(any(), any()) } returns httpError(
                code = ApiResponseError.HttpException.Code.SERVICE_UNAVAILABLE,
                body = """{"status":503,"detail":"Low inventory","code":3016}""",
            )

            // Act
            val actual = runCatching { generate() }.exceptionOrNull()

            // Assert
            assertThat(actual).isInstanceOf(CollectibleException.LowInventory::class.java)
        }

        @Test
        fun `GIVEN another backend code WHEN generatePurchaseTx THEN the http error is thrown as is`() = runTest {
            // Arrange
            coEvery { collectiblesApi.generatePurchaseTx(any(), any()) } returns httpError(
                code = ApiResponseError.HttpException.Code.FORBIDDEN,
                body = """{"status":403,"detail":"Purchase address is blocked","code":3012}""",
            )

            // Act
            val actual = runCatching { generate() }.exceptionOrNull()

            // Assert
            assertThat(actual).isInstanceOf(ApiResponseError.HttpException::class.java)
        }

        private suspend fun generate() = repository.generatePurchaseTx(
            userWalletId = userWalletId,
            params = GeneratePurchaseTxParams(fromAddress = "from", machineId = "pokemon_50", toAddress = "to"),
        )

        private fun purchaseTxResponse() = GeneratePurchaseTxResponse(
            txId = "tx-id",
            txData = GeneratePurchaseTxResponse.TxDataDTO(
                memo = "cc-uuid",
                fromNetwork = "solana-dev",
                fromContractAddress = "mint",
                fromDecimals = 6,
                fromAmount = "50000000",
                feeAmount = "1000000",
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TxStatus {

        @Test
        fun `GIVEN a hash WHEN reportPurchaseTxSent THEN it is sent with the tx id and the status is parsed`() =
            runTest {
                // Arrange
                coEvery { collectiblesApi.purchaseTxSent(any(), any()) } returns
                    ApiResponse.Success(CollectibleTxResponse(txId = "tx-id", status = "purchase-tx-hash"))

                // Act
                val actual = repository.reportPurchaseTxSent(userWalletId, txId = "tx-id", txHash = "hash")

                // Assert
                assertThat(actual).isEqualTo(CollectibleTxStatus.PURCHASE_TX_HASH)
                coVerify(exactly = 1) {
                    collectiblesApi.purchaseTxSent(
                        userWalletId = userWalletId.stringValue,
                        body = PurchaseTxSentRequestBody(txId = "tx-id", txHash = "hash"),
                    )
                }
            }

        @Test
        fun `GIVEN api fails WHEN getTxState THEN the api error is thrown`() = runTest {
            // Arrange
            coEvery { collectiblesApi.getTxStatus(userWalletId.stringValue, txId = "tx-id", includeNftWon = false) } returns
                networkError<CollectibleTxResponse>()

            // Act
            val actual = runCatching { repository.getTxState(userWalletId, txId = "tx-id") }.exceptionOrNull()

            // Assert
            assertThat(actual).isInstanceOf(ApiResponseError.NetworkException::class.java)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OpenPack {

        @Test
        fun `GIVEN the card in the answer WHEN openPack THEN status and card are returned`() = runTest {
            // Arrange
            coEvery {
                collectiblesApi.openPack(
                    userWalletId = userWalletId.stringValue,
                    body = CollectibleTxRequestBody(txId = "tx-id"),
                    includeNftWon = true,
                )
            } returns ApiResponse.Success(
                CollectibleTxResponse(
                    txId = "tx-id",
                    status = "pack-opened",
                    nftWon = CollectibleTxResponse.NftWonDTO(
                        nftAddress = "nft",
                        gradingCompany = "PSA",
                        grade = "GEM-MT 10",
                        gradingId = "123",
                        imageFront = "https://front",
                        imageBack = "https://back",
                        rarity = "common",
                        year = "2024",
                        set = "Temporal Forces",
                        serialNumber = "218",
                        insuredValue = 3500L,
                    ),
                ),
            )

            // Act
            val actual = repository.openPack(userWalletId, txId = "tx-id")

            // Assert
            assertThat(actual).isEqualTo(
                CollectibleTxState(
                    status = CollectibleTxStatus.PACK_OPENED,
                    pack = OpenedCollectiblePack(
                        txId = "tx-id",
                        nftAddress = "nft",
                        imageFrontUrl = "https://front",
                        imageBackUrl = "https://back",
                        rarity = CollectibleRarity.COMMON,
                        gradingCompany = "PSA",
                        gradingId = "123",
                        year = "2024",
                        set = "Temporal Forces",
                        serialNumber = "218",
                        insuredValue = BigDecimal("35.00"),
                    ),
                ),
            )
        }

        @Test
        fun `GIVEN no card in the answer WHEN openPack THEN only the status is returned`() = runTest {
            // Arrange
            coEvery { collectiblesApi.openPack(any(), any(), any()) } returns
                ApiResponse.Success(CollectibleTxResponse(txId = "tx-id", status = "pack-opened"))

            // Act
            val actual = repository.openPack(userWalletId, txId = "tx-id")

            // Assert
            assertThat(actual).isEqualTo(CollectibleTxState(status = CollectibleTxStatus.PACK_OPENED, pack = null))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetMachinesBatchFlow {

        @Test
        fun `GIVEN two pages WHEN flow reloaded and scrolled THEN the body cursor drives the next page`() = runTest {
            // Arrange
            coEvery {
                collectiblesApi.getMachines(categoryId = "pokemons", search = null, limit = 20, cursor = null)
            } returns ApiResponse.Success(
                MachineFactory.createMachinesResponse(
                    machines = listOf(MachineFactory.createDto(id = "first")),
                    hasNext = true,
                    cursor = "c1",
                ),
            )
            coEvery {
                collectiblesApi.getMachines(categoryId = "pokemons", search = null, limit = 20, cursor = "c1")
            } returns ApiResponse.Success(
                MachineFactory.createMachinesResponse(
                    machines = listOf(MachineFactory.createDto(id = "second")),
                    hasNext = false,
                    cursor = null,
                ),
            )
            val actions = MutableSharedFlow<CollectibleGachaMachinesBatchAction>(replay = 1)
            val sourceScope = testSourceScope()
            val batchFlow = repository.getMachinesBatchFlow(
                context = BatchingContext(actionsFlow = actions, coroutineScope = sourceScope),
                batchSize = 20,
            )

            // Act
            actions.emit(BatchAction.Reload(requestParams = pokemonsConfig))
            advanceUntilIdle()
            actions.emit(BatchAction.LoadMore())
            advanceUntilIdle()

            // Assert
            val batches = batchFlow.state.value.data
            assertThat(batches.map { it.key }).containsExactly(0, 1).inOrder()
            assertThat(batches.flatMap { it.data.collectibleGachaMachines }.map { it.id })
                .containsExactly("first", "second")
                .inOrder()
            assertThat(batches.map { it.data.requestCursor }).containsExactly(null, "c1").inOrder()
            coVerify(exactly = 1) {
                collectiblesApi.getMachines(categoryId = "pokemons", search = null, limit = 20, cursor = "c1")
            }
            sourceScope.cancel()
        }

        @Test
        fun `GIVEN a search config WHEN flow reloaded THEN search and batch size reach the api`() = runTest {
            // Arrange
            coEvery {
                collectiblesApi.getMachines(any(), any(), any(), any())
            } returns ApiResponse.Success(MachineFactory.createMachinesResponse())
            val actions = MutableSharedFlow<CollectibleGachaMachinesBatchAction>(replay = 1)
            val sourceScope = testSourceScope()
            val batchFlow = repository.getMachinesBatchFlow(
                context = BatchingContext(actionsFlow = actions, coroutineScope = sourceScope),
                batchSize = 7,
            )

            // Act
            actions.emit(
                BatchAction.Reload(
                    requestParams = MachineFactory.createListConfig(categoryId = null, search = "pikachu"),
                ),
            )
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) {
                collectiblesApi.getMachines(categoryId = null, search = "pikachu", limit = 7, cursor = null)
            }
            assertThat(batchFlow.state.value.data).hasSize(1)
            sourceScope.cancel()
        }

        @Test
        fun `GIVEN a last page WHEN flow reloaded THEN pagination ends`() = runTest {
            // Arrange
            coEvery {
                collectiblesApi.getMachines(any(), any(), any(), any())
            } returns ApiResponse.Success(
                MachineFactory.createMachinesResponse(hasNext = false, cursor = null),
            )
            val actions = MutableSharedFlow<CollectibleGachaMachinesBatchAction>(replay = 1)
            val sourceScope = testSourceScope()
            val batchFlow = repository.getMachinesBatchFlow(
                context = BatchingContext(actionsFlow = actions, coroutineScope = sourceScope),
                batchSize = 20,
            )

            // Act
            actions.emit(BatchAction.Reload(requestParams = pokemonsConfig))
            advanceUntilIdle()

            // Assert
            assertThat(batchFlow.state.value.status).isInstanceOf(PaginationStatus.EndOfPagination::class.java)
            sourceScope.cancel()
        }

        @Test
        fun `GIVEN a failing first page WHEN flow reloaded THEN the error is reported without a retry`() = runTest {
            // Arrange
            coEvery { collectiblesApi.getMachines(any(), any(), any(), any()) } returns networkError<MachinesResponse>()
            val actions = MutableSharedFlow<CollectibleGachaMachinesBatchAction>(replay = 1)
            val sourceScope = testSourceScope()
            val batchFlow = repository.getMachinesBatchFlow(
                context = BatchingContext(actionsFlow = actions, coroutineScope = sourceScope),
                batchSize = 20,
            )

            // Act
            actions.emit(BatchAction.Reload(requestParams = pokemonsConfig))
            advanceUntilIdle()

            // Assert
            val status = batchFlow.state.value.status
            assertThat(status).isInstanceOf(PaginationStatus.InitialLoadingError::class.java)
            assertThat((status as PaginationStatus.InitialLoadingError).throwable)
                .isInstanceOf(ApiResponseError.NetworkException::class.java)
            coVerify(exactly = 1) { collectiblesApi.getMachines(any(), any(), any(), any()) }
            sourceScope.cancel()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetBuybackOffer {

        @Test
        fun `GIVEN buyback available WHEN getBuybackOffer THEN the offer is returned`() = runTest {
            // Arrange
            coEvery {
                collectiblesApi.getBuybackAvailability(userWalletId = userWalletId.stringValue, nftAddress = "nft")
            } returns ApiResponse.Success(BuybackAvailabilityResponse(isAvailable = true, amount = 4250L))

            // Act
            val actual = repository.getBuybackOffer(userWalletId, nftAddress = "nft")

            // Assert
            assertThat(actual).isEqualTo(CollectibleBuybackOffer(amount = BigDecimal("42.50"), expiresAt = null))
        }

        @Test
        fun `GIVEN buyback not available yet WHEN getBuybackOffer THEN null is returned`() = runTest {
            // Arrange
            coEvery { collectiblesApi.getBuybackAvailability(any(), any()) } returns
                ApiResponse.Success(BuybackAvailabilityResponse(isAvailable = false))

            // Act
            val actual = repository.getBuybackOffer(userWalletId, nftAddress = "nft")

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN api fails WHEN getBuybackOffer THEN the api error is thrown`() = runTest {
            // Arrange
            coEvery { collectiblesApi.getBuybackAvailability(any(), any()) } returns
                networkError<BuybackAvailabilityResponse>()

            // Act
            val actual = runCatching { repository.getBuybackOffer(userWalletId, nftAddress = "nft") }.exceptionOrNull()

            // Assert
            assertThat(actual).isInstanceOf(ApiResponseError.NetworkException::class.java)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GenerateBuybackTx {

        @Test
        fun `GIVEN buyback data WHEN generateBuybackTx THEN params reach the body and the tx is mapped`() = runTest {
            // Arrange
            coEvery { collectiblesApi.generateBuybackData(any(), any()) } returns
                ApiResponse.Success(buybackDataResponse())

            // Act
            val actual = generate()

            // Assert
            assertThat(actual).isEqualTo(
                CollectibleBuybackTx(
                    buybackId = "buyback-id",
                    memo = "cc-memo",
                    amount = BigDecimal("42.500000"),
                    transaction = "AQID",
                ),
            )
            coVerify(exactly = 1) {
                collectiblesApi.generateBuybackData(
                    userWalletId = userWalletId.stringValue,
                    body = GenerateBuybackDataRequestBody(
                        fromAddress = "owner",
                        nftAddress = "nft",
                        toAddress = "owner",
                    ),
                )
            }
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a failed request WHEN generateBuybackTx THEN the error is mapped`(model: FailureModel) = runTest {
            // Arrange
            coEvery { collectiblesApi.generateBuybackData(any(), any()) } returns model.response

            // Act
            val actual = runCatching { generate() }.exceptionOrNull()

            // Assert
            assertThat(actual).isInstanceOf(model.expected)
        }

        private suspend fun generate() = repository.generateBuybackTx(
            userWalletId = userWalletId,
            params = GenerateBuybackTxParams(fromAddress = "owner", nftAddress = "nft", toAddress = "owner"),
        )

        private fun buybackDataResponse() = GenerateBuybackDataResponse(
            buybackId = "buyback-id",
            txData = GenerateBuybackDataResponse.TxDataDTO(
                memo = "cc-memo",
                toDecimals = 6,
                toAmount = "42500000",
                tx = "AQID",
            ),
        )

        private fun provideTestModels() = listOf(
            FailureModel(
                response = httpError(code = ApiResponseError.HttpException.Code.INTERNAL_SERVER_ERROR, body = null),
                expected = ApiResponseError.HttpException::class.java,
            ),
            FailureModel(
                response = timeoutError(),
                expected = ApiResponseError.TimeoutException::class.java,
            ),
        )
    }

    internal data class FailureModel(
        val response: ApiResponse<GenerateBuybackDataResponse>,
        val expected: Class<out Throwable>,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class BuybackStatus {

        @Test
        fun `GIVEN a hash WHEN reportBuybackTxSent THEN it is sent with the buyback id and the status is parsed`() =
            runTest {
                // Arrange
                coEvery { collectiblesApi.buybackTxSent(any(), any()) } returns ApiResponse.Success(
                    BuybackStatusResponse(buybackId = "buyback-id", status = BuybackStatusDTO.BUYBACK_TX_SENT),
                )

                // Act
                val actual = repository.reportBuybackTxSent(userWalletId, buybackId = "buyback-id", txHash = "hash")

                // Assert
                assertThat(actual).isEqualTo(CollectibleBuybackStatus.BUYBACK_TX_SENT)
                coVerify(exactly = 1) {
                    collectiblesApi.buybackTxSent(
                        userWalletId = userWalletId.stringValue,
                        body = BuybackTxSentRequestBody(buybackId = "buyback-id", txHash = "hash"),
                    )
                }
            }

        @Test
        fun `GIVEN a confirmed buyback WHEN getBuybackStatus THEN confirmed is returned`() = runTest {
            // Arrange
            coEvery {
                collectiblesApi.getBuybackStatus(userWalletId = userWalletId.stringValue, buybackId = "buyback-id")
            } returns ApiResponse.Success(
                BuybackStatusResponse(buybackId = "buyback-id", status = BuybackStatusDTO.CONFIRMED),
            )

            // Act
            val actual = repository.getBuybackStatus(userWalletId, buybackId = "buyback-id")

            // Assert
            assertThat(actual).isEqualTo(CollectibleBuybackStatus.CONFIRMED)
        }

        @Test
        fun `GIVEN api fails WHEN getBuybackStatus THEN the api error is thrown`() = runTest {
            // Arrange
            coEvery { collectiblesApi.getBuybackStatus(any(), any()) } returns networkError<BuybackStatusResponse>()

            // Act
            val actual = runCatching { repository.getBuybackStatus(userWalletId, buybackId = "buyback-id") }
                .exceptionOrNull()

            // Assert
            assertThat(actual).isInstanceOf(ApiResponseError.NetworkException::class.java)
        }
    }

    /**
     * A scope for the batch source, driven by the scheduler of the test. Deliberately NOT
     * [TestScope.backgroundScope]: its tasks carry the background marker, which [advanceUntilIdle] does not run.
     * The job is detached so the source's never-completing collectors do not keep [runTest] waiting; cancel the
     * scope at the end of the test.
     */
    private fun TestScope.testSourceScope(): CoroutineScope = CoroutineScope(coroutineContext + Job())

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> timeoutError(): ApiResponse<T> =
        ApiResponse.Error(ApiResponseError.TimeoutException()) as ApiResponse<T>

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> networkError(): ApiResponse<T> =
        ApiResponse.Error(ApiResponseError.NetworkException()) as ApiResponse<T>

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> httpError(code: ApiResponseError.HttpException.Code, body: String?): ApiResponse<T> =
        ApiResponse.Error(
            ApiResponseError.HttpException(code = code, message = null, errorBody = body),
        ) as ApiResponse<T>

    private companion object {
        val userWalletId = MachineFactory.userWalletId
        val pokemonsConfig = MachineFactory.createListConfig(categoryId = "pokemons")
    }
}