package com.tangem.data.wallets

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.WalletCardDTO
import com.tangem.datasource.api.tangemTech.models.WalletCardsBody
import com.tangem.datasource.api.tangemTech.models.WalletCardsResponse
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.backup.CardBackupStatus
import com.tangem.domain.wallets.models.backup.WalletCardBackup
import com.tangem.domain.wallets.models.errors.WalletCardsBackupError
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultWalletCardsBackupRepositoryTest {

    private val tangemTechApi: TangemTechApi = mockk()

    private val repository = DefaultWalletCardsBackupRepository(
        tangemTechApi = tangemTechApi,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(tangemTechApi)
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