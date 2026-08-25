package com.tangem.data.wallets

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException
import com.tangem.data.wallets.converters.PendingWalletCardsBackupConverter
import com.tangem.data.wallets.store.PendingWalletCardsBackupStore
import com.tangem.data.wallets.store.PendingWalletCardsBackups
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.WalletCardDTO
import com.tangem.datasource.api.tangemTech.models.WalletCardsBody
import com.tangem.datasource.api.tangemTech.models.WalletCardsResponse
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.backup.CardBackupStatus
import com.tangem.domain.wallets.models.backup.WalletCardBackup
import com.tangem.domain.wallets.models.errors.WalletCardsBackupError
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultWalletCardsBackupRepositoryTest {

    private val tangemTechApi: TangemTechApi = mockk()
    private val dataStore = MockStateDataStore<PendingWalletCardsBackups>(default = emptyList())

    private val pendingStore = PendingWalletCardsBackupStore(dataStore = dataStore)

    private val repository = DefaultWalletCardsBackupRepository(
        tangemTechApi = tangemTechApi,
        pendingStore = pendingStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(tangemTechApi)
        runBlocking { dataStore.updateData { emptyList() } }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SaveWalletCards {

        @Test
        fun `GIVEN api succeeds WHEN saveWalletCards THEN converted body is sent`() = runTest {
            // Arrange
            coEvery { tangemTechApi.saveWalletCards(any(), any()) } returns ApiResponse.Success(Unit)

            // Act
            val actual = repository.saveWalletCards(
                userWalletId = WALLET_ID,
                cards = listOf(domainCard(CardBackupStatus.ACTIVE)),
                usedSeed = true,
            )

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
            coVerify(exactly = 1) {
                tangemTechApi.saveWalletCards(
                    walletId = WALLET_ID.stringValue,
                    body = WalletCardsBody(
                        cards = listOf(dtoCard(WalletCardDTO.BackupStatus.ACTIVE)),
                        usedSeed = true,
                    ),
                )
            }
            assertThat(pendingStore.getAll()).isEmpty()
        }

        @Test
        fun `GIVEN no connection WHEN saveWalletCards THEN NoInternetConnection is returned`() = runTest {
            // Arrange
            coEvery {
                tangemTechApi.saveWalletCards(any(), any())
            } returns ApiResponse.Error(cause = ApiResponseError.NetworkException()).cast()

            // Act
            val actual = repository.saveWalletCards(WALLET_ID, cards = emptyList(), usedSeed = false)

            // Assert
            assertThat(actual).isEqualTo(WalletCardsBackupError.NoInternetConnection.left())
        }

        @Test
        fun `GIVEN server error WHEN saveWalletCards THEN Unexpected is returned`() = runTest {
            // Arrange
            val cause = httpError(HttpException.Code.INTERNAL_SERVER_ERROR)
            coEvery { tangemTechApi.saveWalletCards(any(), any()) } returns ApiResponse.Error(cause).cast()

            // Act
            val actual = repository.saveWalletCards(WALLET_ID, cards = emptyList(), usedSeed = false)

            // Assert
            assertThat(actual).isEqualTo(WalletCardsBackupError.Unexpected(cause = cause).left())
        }

        @Test
        fun `GIVEN the request fails WHEN saveWalletCards THEN the report stays queued`() = runTest {
            // Arrange
            coEvery {
                tangemTechApi.saveWalletCards(any(), any())
            } returns ApiResponse.Error(cause = ApiResponseError.NetworkException()).cast()

            // Act
            repository.saveWalletCards(
                userWalletId = WALLET_ID,
                cards = listOf(domainCard(CardBackupStatus.CARD_LINKED)),
                usedSeed = true,
            )

            // Assert
            val pending = pendingStore.getAll()
            assertThat(pending.map { it.walletId }).containsExactly(WALLET_ID.stringValue)
            assertThat(PendingWalletCardsBackupConverter.convertBack(pending.single())).isEqualTo(
                WalletCardsBody(
                    cards = listOf(dtoCard(WalletCardDTO.BackupStatus.CARD_LINKED)),
                    usedSeed = true,
                ),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SendPendingWalletCards {

        @Test
        fun `GIVEN a report is already queued WHEN saveWalletCards THEN the older one is sent first`() = runTest {
            // Arrange
            coEvery {
                tangemTechApi.saveWalletCards(any(), any())
            } returns ApiResponse.Error(cause = ApiResponseError.NetworkException()).cast()
            repository.saveWalletCards(UserWalletId(stringValue = WALLET_IDS[0]), emptyList(), usedSeed = false)
            clearMocks(tangemTechApi)

            val sent = mutableListOf<String>()
            coEvery { tangemTechApi.saveWalletCards(capture(sent), any()) } returns ApiResponse.Success(Unit)

            // Act
            repository.saveWalletCards(UserWalletId(stringValue = WALLET_IDS[1]), emptyList(), usedSeed = false)

            // Assert
            assertThat(sent).containsExactly(WALLET_IDS[0], WALLET_IDS[1]).inOrder()
            assertThat(pendingStore.getAll()).isEmpty()
        }


        @Test
        fun `GIVEN nothing queued WHEN sendPendingWalletCards THEN the api is not called`() = runTest {
            // Act
            val actual = repository.sendPendingWalletCards()

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
            coVerify(exactly = 0) { tangemTechApi.saveWalletCards(any(), any()) }
        }

        @Test
        fun `GIVEN reports queued WHEN sendPendingWalletCards THEN they are sent oldest first`() = runTest {
            // Arrange
            val sent = queueReports(WALLET_IDS)

            // Act
            val actual = repository.sendPendingWalletCards()

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
            assertThat(sent).containsExactlyElementsIn(WALLET_IDS).inOrder()
            assertThat(pendingStore.getAll()).isEmpty()
        }

        @Test
        fun `GIVEN the device goes offline WHEN sendPendingWalletCards THEN the rest stays queued`() = runTest {
            // Arrange
            queueReports(WALLET_IDS)
            coEvery { tangemTechApi.saveWalletCards(WALLET_IDS[0], any()) } returns ApiResponse.Success(Unit)
            coEvery {
                tangemTechApi.saveWalletCards(WALLET_IDS[1], any())
            } returns ApiResponse.Error(cause = ApiResponseError.NetworkException()).cast()

            // Act
            val actual = repository.sendPendingWalletCards()

            // Assert
            assertThat(actual).isEqualTo(WalletCardsBackupError.NoInternetConnection.left())
            assertThat(pendingStore.getAll().map { it.walletId })
                .containsExactly(WALLET_IDS[1], WALLET_IDS[2]).inOrder()
            coVerify(exactly = 0) { tangemTechApi.saveWalletCards(WALLET_IDS[2], any()) }
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = ["INTERNAL_SERVER_ERROR", "SERVICE_UNAVAILABLE", "TOO_MANY_REQUESTS", "REQUEST_TIMEOUT"])
        fun `GIVEN the backend is failing WHEN sendPendingWalletCards THEN the queue is kept`(code: String) =
            runTest {
                // Arrange
                queueReports(WALLET_IDS)
                val cause = httpError(HttpException.Code.valueOf(code))
                coEvery { tangemTechApi.saveWalletCards(any(), any()) } returns ApiResponse.Error(cause).cast()

                // Act
                val actual = repository.sendPendingWalletCards()

                // Assert
                assertThat(actual).isEqualTo(WalletCardsBackupError.Unexpected(cause = cause).left())
                assertThat(pendingStore.getAll().map { it.walletId }).containsExactlyElementsIn(WALLET_IDS).inOrder()
                coVerify(exactly = 1) { tangemTechApi.saveWalletCards(any(), any()) }
            }

        @Test
        fun `GIVEN an unreadable response WHEN sendPendingWalletCards THEN the report is kept`() = runTest {
            // Arrange
            queueReports(WALLET_IDS)
            val cause = ApiResponseError.UnknownException(cause = IllegalStateException("malformed"))
            coEvery { tangemTechApi.saveWalletCards(any(), any()) } returns ApiResponse.Error(cause).cast()

            // Act
            val actual = repository.sendPendingWalletCards()

            // Assert
            assertThat(actual).isEqualTo(WalletCardsBackupError.Unexpected(cause = cause).left())
            assertThat(pendingStore.getAll().map { it.walletId }).containsExactlyElementsIn(WALLET_IDS).inOrder()
        }

        @Test
        fun `GIVEN the backend rejects a report WHEN sendPendingWalletCards THEN it is dropped`() = runTest {
            // Arrange
            queueReports(WALLET_IDS)
            coEvery { tangemTechApi.saveWalletCards(any(), any()) } returns ApiResponse.Success(Unit)
            coEvery {
                tangemTechApi.saveWalletCards(WALLET_IDS[1], any())
            } returns ApiResponse.Error(cause = httpError(HttpException.Code.BAD_REQUEST)).cast()

            // Act
            val actual = repository.sendPendingWalletCards()

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
            assertThat(pendingStore.getAll()).isEmpty()
            coVerify(exactly = 1) { tangemTechApi.saveWalletCards(WALLET_IDS[2], any()) }
        }

        /**
         * Fills the queue the way production does — a report that fails to send stays behind — and returns
         * the wallet ids the api is called with, in call order.
         */
        private suspend fun queueReports(walletIds: List<String>): List<String> {
            coEvery {
                tangemTechApi.saveWalletCards(any(), any())
            } returns ApiResponse.Error(cause = ApiResponseError.NetworkException()).cast()

            walletIds.forEach { walletId ->
                repository.saveWalletCards(UserWalletId(stringValue = walletId), emptyList(), usedSeed = false)
            }

            clearMocks(tangemTechApi)

            val sent = mutableListOf<String>()
            coEvery { tangemTechApi.saveWalletCards(capture(sent), any()) } returns ApiResponse.Success(Unit)

            return sent
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetWalletCards {

        @Test
        fun `GIVEN api returns cards WHEN getWalletCards THEN they are converted to domain models`() = runTest {
            // Arrange
            coEvery { tangemTechApi.getWalletCards(WALLET_ID.stringValue) } returns ApiResponse.Success(
                WalletCardsResponse(cards = listOf(dtoCard(WalletCardDTO.BackupStatus.CARD_LINKED))),
            )

            // Act
            val actual = repository.getWalletCards(WALLET_ID)

            // Assert
            assertThat(actual).isEqualTo(listOf(domainCard(CardBackupStatus.CARD_LINKED)).right())
        }

        @Test
        fun `GIVEN wallet is unknown to the backend WHEN getWalletCards THEN an empty list is returned`() = runTest {
            // Arrange
            coEvery {
                tangemTechApi.getWalletCards(any())
            } returns ApiResponse.Error(cause = httpError(HttpException.Code.NOT_FOUND)).cast()

            // Act
            val actual = repository.getWalletCards(WALLET_ID)

            // Assert
            assertThat(actual).isEqualTo(emptyList<WalletCardBackup>().right())
        }

        @Test
        fun `GIVEN no connection WHEN getWalletCards THEN NoInternetConnection is returned`() = runTest {
            // Arrange
            coEvery {
                tangemTechApi.getWalletCards(any())
            } returns ApiResponse.Error(cause = ApiResponseError.NetworkException()).cast()

            // Act
            val actual = repository.getWalletCards(WALLET_ID)

            // Assert
            assertThat(actual).isEqualTo(WalletCardsBackupError.NoInternetConnection.left())
        }

        @Test
        fun `GIVEN timeout WHEN getWalletCards THEN NoInternetConnection is returned`() = runTest {
            // Arrange
            coEvery {
                tangemTechApi.getWalletCards(any())
            } returns ApiResponse.Error(cause = ApiResponseError.TimeoutException()).cast()

            // Act
            val actual = repository.getWalletCards(WALLET_ID)

            // Assert
            assertThat(actual).isEqualTo(WalletCardsBackupError.NoInternetConnection.left())
        }

        @Test
        fun `GIVEN server error WHEN getWalletCards THEN Unexpected is returned`() = runTest {
            // Arrange
            val cause = httpError(HttpException.Code.INTERNAL_SERVER_ERROR)
            coEvery { tangemTechApi.getWalletCards(any()) } returns ApiResponse.Error(cause).cast()

            // Act
            val actual = repository.getWalletCards(WALLET_ID)

            // Assert
            assertThat(actual).isEqualTo(WalletCardsBackupError.Unexpected(cause = cause).left())
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> ApiResponse.Error.cast(): ApiResponse<T> = this as ApiResponse<T>

    private companion object {

        val WALLET_ID = UserWalletId(stringValue = "0102030405060708")
        val WALLET_IDS = listOf("0101010101010101", "0202020202020202", "0303030303030303")
        const val CARD_ID = "AC01000000000000"
        const val CARD_PUBLIC_KEY = "0AFF"

        fun httpError(code: HttpException.Code) = HttpException(code = code, message = null, errorBody = null)

        fun domainCard(backupStatus: CardBackupStatus) = WalletCardBackup(
            cardId = CARD_ID,
            cardPublicKey = CARD_PUBLIC_KEY,
            role = WalletCardBackup.Role.PRIMARY,
            backupStatus = backupStatus,
            curves = emptyList(),
        )

        fun dtoCard(backupStatus: WalletCardDTO.BackupStatus) = WalletCardDTO(
            cardId = CARD_ID,
            cardPublicKey = CARD_PUBLIC_KEY,
            role = WalletCardDTO.Role.PRIMARY,
            backupStatus = backupStatus,
            curves = emptyList(),
        )
    }
}