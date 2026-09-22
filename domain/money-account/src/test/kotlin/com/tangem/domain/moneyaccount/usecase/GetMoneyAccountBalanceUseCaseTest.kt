package com.tangem.domain.moneyaccount.usecase

import arrow.core.right
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.networks.single.SingleNetworkStatusProducer
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.pay.TangemPayCurrencyFactory
import com.google.common.truth.Truth
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class GetMoneyAccountBalanceUseCaseTest {

    private val tangemPayCurrencyFactory: TangemPayCurrencyFactory = mockk()
    private val singleNetworkStatusFetcher: SingleNetworkStatusFetcher = mockk()
    private val singleNetworkStatusSupplier: SingleNetworkStatusSupplier = mockk()

    private val useCase = GetMoneyAccountBalanceUseCase(
        tangemPayCurrencyFactory = tangemPayCurrencyFactory,
        singleNetworkStatusFetcher = singleNetworkStatusFetcher,
        singleNetworkStatusSupplier = singleNetworkStatusSupplier,
    )

    private val userWalletId = UserWalletId("011")
    private val network: Network = mockk()
    private val tokenId: CryptoCurrency.ID = mockk()
    private val token: CryptoCurrency.Token = mockk {
        every { this@mockk.network } returns this@GetMoneyAccountBalanceUseCaseTest.network
        every { id } returns tokenId
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(tangemPayCurrencyFactory, singleNetworkStatusFetcher, singleNetworkStatusSupplier)
        every { tangemPayCurrencyFactory.createMoneyAccountToken(userWalletId) } returns token
        coEvery { singleNetworkStatusFetcher(any()) } returns Unit.right()
    }

    @Test
    fun `GIVEN verified status WHEN invoke THEN token amount returned`() = runTest {
        // Arrange
        val amount = BigDecimal("12.34")
        coEvery {
            singleNetworkStatusSupplier.getSyncOrNull(SingleNetworkStatusProducer.Params(userWalletId, network))
        } returns networkStatusWith(NetworkStatus.Amount.Loaded(amount))

        // Act
        val actual = useCase(userWalletId)

        // Assert
        Truth.assertThat(actual.getOrNull()).isEqualTo(amount)
        coVerify(exactly = 1) {
            singleNetworkStatusFetcher(
                SingleNetworkStatusFetcher.Params(
                    userWalletId = userWalletId,
                    network = network,
                    extraTokens = setOf(token),
                ),
            )
        }
    }

    @Test
    fun `GIVEN no verified status WHEN invoke THEN left returned`() = runTest {
        // Arrange
        coEvery {
            singleNetworkStatusSupplier.getSyncOrNull(SingleNetworkStatusProducer.Params(userWalletId, network))
        } returns null

        // Act
        val actual = useCase(userWalletId)

        // Assert
        Truth.assertThat(actual.isLeft()).isTrue()
    }

    @Test
    fun `GIVEN token cannot be created WHEN invoke THEN left returned`() = runTest {
        // Arrange
        every { tangemPayCurrencyFactory.createMoneyAccountToken(userWalletId) } throws IllegalArgumentException()

        // Act
        val actual = useCase(userWalletId)

        // Assert
        Truth.assertThat(actual.isLeft()).isTrue()
        coVerify(exactly = 0) { singleNetworkStatusFetcher(any()) }
    }

    private fun networkStatusWith(amount: NetworkStatus.Amount): NetworkStatus {
        val verified: NetworkStatus.Verified = mockk {
            every { amounts } returns mapOf(tokenId to amount)
        }
        return mockk { every { value } returns verified }
    }
}