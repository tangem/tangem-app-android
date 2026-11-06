package com.tangem.data.yield.supply.status

import com.tangem.data.yield.supply.status.store.YieldModuleStatusStore
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class YieldModuleStatusDataCleanerTest {

    private val store: YieldModuleStatusStore = mockk(relaxUnitFun = true)
    private val cleaner = YieldModuleStatusDataCleaner(store = store)

    private val userWalletId = UserWalletId("011")

    @BeforeEach
    fun setUp() {
        clearMocks(store)
    }

    @Test
    fun `GIVEN token and coin WHEN invoke THEN only the token status is cleared`() = runTest {
        // Arrange
        val token = createToken()
        val coin: CryptoCurrency.Coin = mockk()

        // Act
        cleaner(userWalletId = userWalletId, currencies = listOf(token, coin))

        // Assert
        coVerify(exactly = 1) { store.clear(userWalletId, setOf(createStatus(token).key)) }
    }

    @Test
    fun `GIVEN wallet ids WHEN clear THEN their statuses are removed`() = runTest {
        // Act
        cleaner.clear(userWalletIds = listOf(userWalletId))

        // Assert
        coVerify(exactly = 1) { store.remove(listOf(userWalletId)) }
    }
}