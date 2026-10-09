package com.tangem.domain.polymarket.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketCredentialsLock
import com.tangem.domain.polymarket.PolymarketCredentialsStore
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.domain.polymarket.model.PredictionPlaceOrderError
import com.tangem.domain.polymarket.model.PredictionPlaceOrderResult
import com.tangem.domain.polymarket.model.PredictionSignedOrder
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PlacePredictionOrderUseCaseTest {

    private val repository: PolymarketRepository = mockk()
    private val credentialsStore: PolymarketCredentialsStore = mockk()
    private val signedOrder: PredictionSignedOrder = mockk()

    private val useCase = PlacePredictionOrderUseCase(
        polymarketRepository = repository,
        getApiCredentials = GetPolymarketApiCredentialsUseCase(credentialsStore = credentialsStore),
        invalidateCredentials = InvalidatePolymarketCredentialsUseCase(credentialsStore = credentialsStore),
        credentialsLock = PolymarketCredentialsLock(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository, credentialsStore)

        coEvery { credentialsStore.clear(any()) } returns Unit
    }

    @Test
    fun `GIVEN credentials are stored WHEN invoke THEN places the order of the owner address`() = runTest {
        // Arrange
        coEvery { credentialsStore.get(USER_WALLET_ID) } returns CREDENTIALS
        coEvery { repository.placeOrder(OWNER, CREDENTIALS, signedOrder, isNegRisk = false) } returns RESULT.right()

        // Act
        val actual = useCase(addresses = ADDRESSES, signedOrder = signedOrder, isNegRisk = false)

        // Assert
        assertThat(actual).isEqualTo(RESULT.right())
        coVerify(exactly = 0) { credentialsStore.clear(any()) }
    }

    @Test
    fun `GIVEN the exchange answers unauthorized WHEN invoke THEN clears the credentials`() = runTest {
        // Arrange
        coEvery { credentialsStore.get(USER_WALLET_ID) } returns CREDENTIALS
        coEvery { repository.placeOrder(OWNER, CREDENTIALS, signedOrder, isNegRisk = false) } returns
            PredictionPlaceOrderError.Unauthorized(detail = "Unauthorized/Invalid api key").left()

        // Act
        val actual = useCase(addresses = ADDRESSES, signedOrder = signedOrder, isNegRisk = false)

        // Assert
        assertThat(actual).isEqualTo(
            PredictionPlaceOrderError.Unauthorized(detail = "Unauthorized/Invalid api key").left(),
        )
        coVerify(exactly = 1) { credentialsStore.clear(USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN the order itself is refused WHEN invoke THEN keeps the credentials`() = runTest {
        // Arrange
        coEvery { credentialsStore.get(USER_WALLET_ID) } returns CREDENTIALS
        coEvery { repository.placeOrder(OWNER, CREDENTIALS, signedOrder, isNegRisk = false) } returns
            PredictionPlaceOrderError.Rejected(detail = "invalid order").left()

        // Act
        useCase(addresses = ADDRESSES, signedOrder = signedOrder, isNegRisk = false)

        // Assert
        coVerify(exactly = 0) { credentialsStore.clear(any()) }
    }

    @Test
    fun `GIVEN nothing is stored WHEN invoke THEN fails without reaching the exchange or clearing`() = runTest {
        // Arrange
        coEvery { credentialsStore.get(USER_WALLET_ID) } returns null

        // Act
        val actual = useCase(addresses = ADDRESSES, signedOrder = signedOrder, isNegRisk = false)

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(PredictionPlaceOrderError.Unauthorized::class.java)
        coVerify(exactly = 0) { repository.placeOrder(any(), any(), any(), any()) }
        coVerify(exactly = 0) { credentialsStore.clear(any()) }
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val OWNER = "0x1111111111111111111111111111111111111111"
        val ADDRESSES = PolymarketAddresses(
            ownerAddress = OWNER,
            depositWalletAddress = "0x2222222222222222222222222222222222222222",
            userWalletId = USER_WALLET_ID,
        )
        val CREDENTIALS = PolymarketApiCredentials(apiKey = "key", secret = "secret", passphrase = "pass")
        val RESULT = PredictionPlaceOrderResult(
            isSuccess = true,
            orderId = "order-1",
            status = "matched",
            makingAmount = null,
            takingAmount = null,
            transactionHashes = emptyList(),
            errorMessage = null,
        )
    }
}