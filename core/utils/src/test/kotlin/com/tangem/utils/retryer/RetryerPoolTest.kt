package com.tangem.utils.retryer

import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RetryerPoolTest {

    private val coroutineScope = CoroutineScope(SupervisorJob() + TestingCoroutineDispatcherProvider().default)

    @Test
    fun `retryer pool plus operator adds retryer to the pool`() {
        // Arrange
        val retryerPool = RetryerPool(coroutineScope = coroutineScope)
        val retryer = mockk<Retryer>(relaxUnitFun = true)

        // Act
        retryerPool + retryer

        // Assert
        coVerify(exactly = 1) {
            retryer.launch()
        }
    }

    @Test
    fun `multiple retryers added to the pool are launched`() {
        // Arrange
        val retryerPool = RetryerPool(coroutineScope = coroutineScope)
        val retryer1 = mockk<Retryer>(relaxUnitFun = true)
        val retryer2 = mockk<Retryer>(relaxUnitFun = true)
        val retryer3 = mockk<Retryer>(relaxUnitFun = true)

        // Act
        retryerPool + retryer1 + retryer2 + retryer3

        // Assert
        coVerifyOrder {
            retryer1.launch()
            retryer2.launch()
            retryer3.launch()
        }
    }
}