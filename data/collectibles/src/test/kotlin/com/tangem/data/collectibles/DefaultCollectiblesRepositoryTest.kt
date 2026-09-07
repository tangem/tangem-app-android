package com.tangem.data.collectibles

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.domain.collectibles.CollectibleGachaMachinesBatchAction
import com.tangem.domain.collectibles.models.CollectibleTransaction
import com.tangem.domain.collectibles.models.CollectibleTurboMode
import com.tangem.domain.collectibles.models.GenerateTxParams
import com.tangem.domain.collectibles.models.SubmittedCollectibleTransaction
import com.tangem.grow.datasource.collectibles.CollectiblesApi
import com.tangem.grow.datasource.collectibles.models.request.GenerateTxRequestBody
import com.tangem.grow.datasource.collectibles.models.request.SubmitTxRequestBody
import com.tangem.grow.datasource.collectibles.models.response.GenerateTxResponse
import com.tangem.grow.datasource.collectibles.models.response.MachineCategoriesResponse
import com.tangem.grow.datasource.collectibles.models.response.MachinesResponse
import com.tangem.grow.datasource.collectibles.models.response.SubmitTxResponse
import com.tangem.grow.datasource.collectibles.models.response.model.MachineDTO
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchingContext
import com.tangem.pagination.PaginationStatus
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

internal class DefaultCollectiblesRepositoryTest {

    private val collectiblesApi: CollectiblesApi = mockk()

    private val repository = DefaultCollectiblesRepository(
        collectiblesApi = collectiblesApi,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
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
    inner class GenerateTx {

        @Test
        fun `GIVEN turbo mode WHEN generateTx THEN body carries turbo params`() = runTest {
            // Arrange
            coEvery { collectiblesApi.generateTx(any(), any()) } returns ApiResponse.Success(
                GenerateTxResponse(id = "tx-id", txData = "base64"),
            )

            // Act
            val actual = repository.generateTx(
                userWalletId = userWalletId,
                params = GenerateTxParams(
                    fromAddress = "from",
                    collectibleType = "pokemon_50",
                    toAddress = "to",
                    collectibleTurboMode = CollectibleTurboMode(isEnabled = true, buybackAddress = "buyback"),
                ),
            )

            // Assert
            assertThat(actual).isEqualTo(CollectibleTransaction(id = "tx-id", txData = "base64"))
            coVerify(exactly = 1) {
                collectiblesApi.generateTx(
                    userWalletId = userWalletId.stringValue,
                    body = GenerateTxRequestBody(
                        fromAddress = "from",
                        collectibleType = "pokemon_50",
                        toAddress = "to",
                        turboMode = GenerateTxRequestBody.TurboModeDTO(
                            isEnabled = true,
                            buybackAddress = "buyback",
                        ),
                    ),
                )
            }
        }

        @Test
        fun `GIVEN no turbo mode WHEN generateTx THEN body omits it`() = runTest {
            // Arrange
            coEvery { collectiblesApi.generateTx(any(), any()) } returns ApiResponse.Success(
                GenerateTxResponse(id = "tx-id", txData = "base64"),
            )

            // Act
            repository.generateTx(
                userWalletId = userWalletId,
                params = GenerateTxParams(fromAddress = "from", collectibleType = "pokemon_50", toAddress = "to"),
            )

            // Assert
            coVerify(exactly = 1) {
                collectiblesApi.generateTx(
                    userWalletId = userWalletId.stringValue,
                    body = GenerateTxRequestBody(
                        fromAddress = "from",
                        collectibleType = "pokemon_50",
                        toAddress = "to",
                        turboMode = null,
                    ),
                )
            }
        }

        @Test
        fun `GIVEN api fails WHEN generateTx THEN the api error is thrown`() = runTest {
            // Arrange
            coEvery { collectiblesApi.generateTx(any(), any()) } returns timeoutError<GenerateTxResponse>()

            // Act
            val actual = runCatching {
                repository.generateTx(
                    userWalletId = userWalletId,
                    params = GenerateTxParams(fromAddress = "from", collectibleType = "pokemon_50", toAddress = "to"),
                )
            }.exceptionOrNull()

            // Assert
            assertThat(actual).isInstanceOf(ApiResponseError.TimeoutException::class.java)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SubmitTx {

        @Test
        fun `GIVEN signed tx WHEN submitTx THEN returns submitted id`() = runTest {
            // Arrange
            coEvery {
                collectiblesApi.submitTx(any(), any())
            } returns ApiResponse.Success(SubmitTxResponse(id = "tx-id"))

            // Act
            val actual = repository.submitTx(userWalletId = userWalletId, signedTxData = "signed")

            // Assert
            assertThat(actual).isEqualTo(SubmittedCollectibleTransaction(id = "tx-id"))
            coVerify(exactly = 1) {
                collectiblesApi.submitTx(
                    userWalletId = userWalletId.stringValue,
                    body = SubmitTxRequestBody(signedTxData = "signed"),
                )
            }
        }

        @Test
        fun `GIVEN api fails WHEN submitTx THEN the api error is thrown`() = runTest {
            // Arrange
            coEvery { collectiblesApi.submitTx(any(), any()) } returns networkError<SubmitTxResponse>()

            // Act
            val actual = runCatching {
                repository.submitTx(userWalletId = userWalletId, signedTxData = "signed")
            }.exceptionOrNull()

            // Assert
            assertThat(actual).isInstanceOf(ApiResponseError.NetworkException::class.java)
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

    private companion object {
        val userWalletId = MachineFactory.userWalletId
        val pokemonsConfig = MachineFactory.createListConfig(categoryId = "pokemons")
    }
}