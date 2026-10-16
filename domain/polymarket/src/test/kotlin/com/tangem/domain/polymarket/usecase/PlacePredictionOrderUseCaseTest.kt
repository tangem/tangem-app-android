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
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PolymarketClobAsset
import com.tangem.domain.polymarket.model.PolymarketMarketVersion
import com.tangem.domain.polymarket.model.PredictionOrder
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.model.PredictionPlaceOrderError
import com.tangem.domain.polymarket.model.PredictionPlaceOrderResult
import com.tangem.domain.polymarket.model.PredictionSignedOrder
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import java.math.BigInteger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import com.tangem.test.core.ProvideTestModels

internal class PlacePredictionOrderUseCaseTest {

    private val repository: PolymarketRepository = mockk()
    private val credentialsStore: PolymarketCredentialsStore = mockk()
    private val signedOrder = signedOrder(PredictionOrderSide.BUY)

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
        val actual = useCase(addresses = ADDRESSES, signedOrder = signedOrder, isNegRisk = false, version = V1)

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
        val actual = useCase(addresses = ADDRESSES, signedOrder = signedOrder, isNegRisk = false, version = V1)

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
        useCase(addresses = ADDRESSES, signedOrder = signedOrder, isNegRisk = false, version = V1)

        // Assert
        coVerify(exactly = 0) { credentialsStore.clear(any()) }
    }

    @Test
    fun `GIVEN nothing is stored WHEN invoke THEN fails without reaching the exchange or clearing`() = runTest {
        // Arrange
        coEvery { credentialsStore.get(USER_WALLET_ID) } returns null

        // Act
        val actual = useCase(addresses = ADDRESSES, signedOrder = signedOrder, isNegRisk = false, version = V1)

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(PredictionPlaceOrderError.Unauthorized::class.java)
        coVerify(exactly = 0) { repository.placeOrder(any(), any(), any(), any()) }
        coVerify(exactly = 0) { credentialsStore.clear(any()) }
    }

    @Test
    fun `GIVEN a BUY WHEN invoke THEN no balance is refreshed before the order`() = runTest {
        // Arrange
        coEvery { credentialsStore.get(USER_WALLET_ID) } returns CREDENTIALS
        coEvery { repository.placeOrder(OWNER, CREDENTIALS, signedOrder, isNegRisk = false) } returns RESULT.right()

        // Act
        useCase(addresses = ADDRESSES, signedOrder = signedOrder, isNegRisk = false, version = V1)

        // Assert
        coVerify(exactly = 0) { repository.syncBalanceAllowance(any(), any(), any()) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a SELL WHEN invoke THEN the shares balance is refreshed under the market version before the order`(
        model: SellSyncTestModel,
    ) = runTest {
        // Arrange
        val sell = signedOrder(PredictionOrderSide.SELL)
        coEvery { credentialsStore.get(USER_WALLET_ID) } returns CREDENTIALS
        coEvery { repository.syncBalanceAllowance(OWNER, CREDENTIALS, model.expectedAsset) } returns Unit.right()
        coEvery { repository.placeOrder(OWNER, CREDENTIALS, sell, isNegRisk = true) } returns RESULT.right()

        // Act
        val actual = useCase(addresses = ADDRESSES, signedOrder = sell, isNegRisk = true, version = model.version)

        // Assert
        assertThat(actual).isEqualTo(RESULT.right())
        coVerifyOrder {
            repository.syncBalanceAllowance(OWNER, CREDENTIALS, model.expectedAsset)
            repository.placeOrder(OWNER, CREDENTIALS, sell, isNegRisk = true)
        }
    }

    @Test
    fun `GIVEN the shares refresh fails WHEN invoke THEN the SELL is sent anyway`() = runTest {
        // Arrange
        val sell = signedOrder(PredictionOrderSide.SELL)
        coEvery { credentialsStore.get(USER_WALLET_ID) } returns CREDENTIALS
        coEvery { repository.syncBalanceAllowance(OWNER, CREDENTIALS, any()) } returns PolymarketAuthError.Network.left()
        coEvery { repository.placeOrder(OWNER, CREDENTIALS, sell, isNegRisk = false) } returns RESULT.right()

        // Act
        val actual = useCase(addresses = ADDRESSES, signedOrder = sell, isNegRisk = false, version = V1)

        // Assert
        assertThat(actual).isEqualTo(RESULT.right())
        coVerify(exactly = 1) { repository.placeOrder(OWNER, CREDENTIALS, sell, isNegRisk = false) }
    }

    @Test
    fun `GIVEN the shares refresh answers unauthorized WHEN invoke THEN the credentials are kept`() = runTest {
        // Arrange
        val sell = signedOrder(PredictionOrderSide.SELL)
        coEvery { credentialsStore.get(USER_WALLET_ID) } returns CREDENTIALS
        coEvery { repository.syncBalanceAllowance(OWNER, CREDENTIALS, any()) } returns
            PolymarketAuthError.InvalidSignature.left()
        coEvery { repository.placeOrder(OWNER, CREDENTIALS, sell, isNegRisk = false) } returns RESULT.right()

        // Act
        useCase(addresses = ADDRESSES, signedOrder = sell, isNegRisk = false, version = V1)

        // Assert
        coVerify(exactly = 0) { credentialsStore.clear(any()) }
    }

    private fun signedOrder(side: PredictionOrderSide) = PredictionSignedOrder(
        order = PredictionOrder(
            salt = 1L,
            maker = "0x2222222222222222222222222222222222222222",
            signer = OWNER,
            tokenId = TOKEN_ID,
            makerAmount = BigInteger.TEN,
            takerAmount = BigInteger.ONE,
            side = side,
            signatureType = 3,
            timestampMillis = 0L,
            metadata = "",
            builder = "",
        ),
        signature = "0xsig",
    )

    data class SellSyncTestModel(val version: PolymarketMarketVersion, val expectedAsset: PolymarketClobAsset)

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val OWNER = "0x1111111111111111111111111111111111111111"
        val ADDRESSES = PolymarketAddresses(
            ownerAddress = OWNER,
            depositWalletAddress = "0x2222222222222222222222222222222222222222",
            userWalletId = USER_WALLET_ID,
        )
        val CREDENTIALS = PolymarketApiCredentials(apiKey = "key", secret = "secret", passphrase = "pass")
        val V1 = PolymarketMarketVersion.V1
        val TOKEN_ID = BigInteger("123456789")
        val RESULT = PredictionPlaceOrderResult(
            isSuccess = true,
            orderId = "order-1",
            status = "matched",
            makingAmount = null,
            takingAmount = null,
            transactionHashes = emptyList(),
            errorMessage = null,
        )

        @JvmStatic
        fun provideTestModels() = listOf(
            SellSyncTestModel(
                version = PolymarketMarketVersion.V1,
                expectedAsset = PolymarketClobAsset.Shares(tokenId = "123456789", version = PolymarketMarketVersion.V1),
            ),
            SellSyncTestModel(
                version = PolymarketMarketVersion.V2,
                expectedAsset = PolymarketClobAsset.Shares(tokenId = "123456789", version = PolymarketMarketVersion.V2),
            ),
        )
    }
}