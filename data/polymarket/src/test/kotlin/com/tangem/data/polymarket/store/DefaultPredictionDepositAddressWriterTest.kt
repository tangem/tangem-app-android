package com.tangem.data.polymarket.store

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
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
    fun `GIVEN an active status WHEN an address is stored THEN the merge puts it on the status`() = runTest {
        // Act
        writer.store(userWalletId = USER_WALLET_ID, address = EVM_ADDRESS)

        // Assert
        assertThat(capturedMerge()(ACTIVE)).isEqualTo(ACTIVE.copy(depositAddress = EVM_ADDRESS))
    }

    @Test
    fun `GIVEN an active status WHEN null is stored THEN the merge withdraws the address`() = runTest {
        // Act
        writer.store(userWalletId = USER_WALLET_ID, address = null)

        // Assert
        assertThat(capturedMerge()(ACTIVE.copy(depositAddress = EVM_ADDRESS)))
            .isEqualTo(ACTIVE.copy(depositAddress = null))
    }

    @Test
    fun `GIVEN no active status WHEN an address is stored THEN the merge writes nothing`() = runTest {
        // Act
        writer.store(userWalletId = USER_WALLET_ID, address = EVM_ADDRESS)

        // Assert
        assertThat(capturedMerge()(PredictionAccountStatusValue.Loading)).isNull()
    }

    @Test
    fun `GIVEN a refresh commits first WHEN the address is written THEN the refreshed balance is kept`() = runTest {
        // Arrange
        val refreshed = ACTIVE.copy(balance = BigDecimal("99"))

        // Act
        writer.store(userWalletId = USER_WALLET_ID, address = EVM_ADDRESS)

        // Assert
        assertThat(capturedMerge()(refreshed)).isEqualTo(refreshed.copy(depositAddress = EVM_ADDRESS))
    }

    private fun capturedMerge(): (PredictionAccountStatusValue?) -> PredictionAccountStatusValue? {
        val transform = slot<(PredictionAccountStatusValue?) -> PredictionAccountStatusValue?>()

        coVerify(exactly = 1) {
            statusStore.update(userWalletId = USER_WALLET_ID, transform = capture(transform))
        }

        return transform.captured
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId(stringValue = "011")
        const val EVM_ADDRESS = "0x4038A7a1"

        val COLLATERAL: CryptoCurrency.Token = mockk(relaxed = true)

        val ACTIVE = PredictionAccountStatusValue.Active(
            source = StatusSource.ACTUAL,
            balance = BigDecimal("12.5"),
            positionsValue = null,
            fiatRate = null,
            depositCurrency = COLLATERAL,
            depositAddress = null,
        )
    }
}