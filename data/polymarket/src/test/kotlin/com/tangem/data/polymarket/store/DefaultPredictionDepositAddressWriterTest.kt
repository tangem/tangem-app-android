package com.tangem.data.polymarket.store

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class DefaultPredictionDepositAddressWriterTest {

    private val statusStore: PredictionAccountStatusStore = mockk(relaxUnitFun = true)

    private val writer = DefaultPredictionDepositAddressWriter(
        statusStore = statusStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() = clearMocks(statusStore)

    @Test
    fun `GIVEN an active status WHEN an address is stored THEN it lands on the status`() = runTest {
        // Arrange
        coEvery { statusStore.getSyncOrNull(USER_WALLET_ID) } returns ACTIVE

        // Act
        writer.store(userWalletId = USER_WALLET_ID, address = EVM_ADDRESS)

        // Assert
        coVerify(exactly = 1) {
            statusStore.store(userWalletId = USER_WALLET_ID, value = ACTIVE.copy(depositAddress = EVM_ADDRESS))
        }
    }

    @Test
    fun `GIVEN an active status WHEN null is stored THEN the address is withdrawn`() = runTest {
        // Arrange
        coEvery { statusStore.getSyncOrNull(USER_WALLET_ID) } returns ACTIVE.copy(depositAddress = EVM_ADDRESS)

        // Act
        writer.store(userWalletId = USER_WALLET_ID, address = null)

        // Assert
        coVerify(exactly = 1) {
            statusStore.store(userWalletId = USER_WALLET_ID, value = ACTIVE.copy(depositAddress = null))
        }
    }

    @Test
    fun `GIVEN no active status WHEN an address is stored THEN nothing is written`() = runTest {
        // Arrange
        coEvery { statusStore.getSyncOrNull(USER_WALLET_ID) } returns PredictionAccountStatusValue.Loading

        // Act
        writer.store(userWalletId = USER_WALLET_ID, address = EVM_ADDRESS)

        // Assert
        coVerify(exactly = 0) { statusStore.store(any(), any()) }
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId(stringValue = "011")
        const val EVM_ADDRESS = "0x4038A7a1"

        val COLLATERAL: CryptoCurrency.Token = mockk(relaxed = true)

        val ACTIVE = PredictionAccountStatusValue.Active(
            source = StatusSource.ACTUAL,
            balance = BigDecimal("12.5"),
            fiatRate = null,
            isTradingAllowed = true,
            depositCurrency = COLLATERAL,
            depositAddress = null,
        )
    }
}