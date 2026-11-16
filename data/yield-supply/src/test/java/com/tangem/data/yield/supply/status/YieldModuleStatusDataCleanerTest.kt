package com.tangem.data.yield.supply.status

import com.tangem.data.yield.supply.status.store.YieldModuleStatusStore
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.YieldSupplyWrappedNativeFactory
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class YieldModuleStatusDataCleanerTest {

    private val store: YieldModuleStatusStore = mockk(relaxUnitFun = true)
    private val wrappedNativeFactory: YieldSupplyWrappedNativeFactory = mockk()
    private val cleaner = YieldModuleStatusDataCleaner(store = store, wrappedNativeFactory = wrappedNativeFactory)

    private val userWalletId = UserWalletId("011")

    @BeforeEach
    fun setUp() {
        clearMocks(store, wrappedNativeFactory)
        every { wrappedNativeFactory.create(any()) } returns null
    }

    @Test
    fun `GIVEN the wrapped native token and another token WHEN invoke THEN only the other status is cleared`() =
        runTest {
            // Arrange
            val wrappedNative = createToken(contractAddress = WETH_ADDRESS, symbol = "WETH")
            val token = createToken()
            every { wrappedNativeFactory.create(any()) } returns wrappedNative

            // Act
            cleaner(userWalletId = userWalletId, currencies = listOf(wrappedNative, token))

            // Assert
            coVerify(exactly = 1) { store.clear(userWalletId, setOf(createStatus(token).key)) }
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