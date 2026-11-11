package com.tangem.features.earn.lending.impl.provider.aave

import arrow.core.left
import arrow.core.none
import arrow.core.right
import arrow.core.some
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.usecase.YieldSupplyMinAmountUseCase
import com.tangem.features.earn.lending.impl.provider.LendingAsset
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AaveMinDepositProviderTest {

    private val userWalletId = UserWalletId("011")
    private val currencies = MockCryptoCurrencyFactory()
    private val asset = LendingAsset(
        currency = currencies.createCoin(Blockchain.Ethereum),
        wrappedToken = currencies.createToken(Blockchain.Ethereum),
    )
    private val ethStatus: CryptoCurrencyStatus = mockk()
    private val accountStatusList: AccountStatusList = mockk()

    private val minAmountUseCase: YieldSupplyMinAmountUseCase = mockk()
    private val accountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val timeSource = TestTimeSource()

    private lateinit var provider: AaveMinDepositProvider

    @BeforeAll
    fun mockOperations() = mockkObject(CryptoCurrencyStatusOperations)

    @AfterAll
    fun unmockOperations() = unmockkObject(CryptoCurrencyStatusOperations)

    @BeforeEach
    fun setUp() {
        clearMocks(minAmountUseCase, accountStatusListSupplier)
        // The provider keeps a cache, so each test starts from an empty one
        provider = AaveMinDepositProvider(minAmountUseCase, accountStatusListSupplier, timeSource)
        coEvery { accountStatusListSupplier.getSyncOrNull(userWalletId) } returns accountStatusList
        every {
            with(CryptoCurrencyStatusOperations) { accountStatusList.getCryptoCurrencyStatus(any<CryptoCurrency>()) }
        } returns ethStatus.some()
        coEvery { minAmountUseCase(userWalletId, ethStatus) } returns MIN_DEPOSIT.right()
    }

    @Test
    fun `GIVEN value read less than a minute ago WHEN get THEN cached value without a new read`() = runTest {
        // Arrange
        provider.get(userWalletId, asset)
        timeSource += 59.seconds

        // Act
        val actual = provider.get(userWalletId, asset)

        // Assert
        assertThat(actual).isEqualTo(MIN_DEPOSIT.right())
        coVerify(exactly = 1) { minAmountUseCase(userWalletId, ethStatus) }
    }

    @Test
    fun `GIVEN a minute passed WHEN get THEN value is read again`() = runTest {
        // Arrange
        provider.get(userWalletId, asset)
        timeSource += 60.seconds

        // Act
        provider.get(userWalletId, asset)

        // Assert
        coVerify(exactly = 2) { minAmountUseCase(userWalletId, ethStatus) }
    }

    @Test
    fun `GIVEN read fails WHEN get twice THEN failure is not cached`() = runTest {
        // Arrange
        coEvery { minAmountUseCase(userWalletId, ethStatus) } returns IllegalStateException("fee").left()

        // Act
        val first = provider.get(userWalletId, asset)
        provider.get(userWalletId, asset)

        // Assert
        assertThat(first.isLeft()).isTrue()
        coVerify(exactly = 2) { minAmountUseCase(userWalletId, ethStatus) }
    }

    @Test
    fun `GIVEN no ETH status WHEN get THEN Left without a read`() = runTest {
        // Arrange
        every {
            with(CryptoCurrencyStatusOperations) { accountStatusList.getCryptoCurrencyStatus(any<CryptoCurrency>()) }
        } returns none()

        // Act
        val actual = provider.get(userWalletId, asset)

        // Assert
        assertThat(actual.isLeft()).isTrue()
        coVerify(exactly = 0) { minAmountUseCase(any(), any()) }
    }

    private companion object {
        val MIN_DEPOSIT = BigDecimal("0.01")
    }
}