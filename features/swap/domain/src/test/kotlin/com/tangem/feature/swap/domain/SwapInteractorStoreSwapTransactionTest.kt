package com.tangem.feature.swap.domain

import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class SwapInteractorStoreSwapTransactionTest : SwapInteractorImplTestBase() {

    @Test
    fun `GIVEN a stored swap WHEN storeSwapTransaction THEN both sides are recorded`() = runTest {
        // Arrange
        val from = buildSwapCurrencyStatus()
        val to = buildSwapCurrencyStatus()
        val swapDataModel: SwapDataModel = mockk(relaxed = true)
        val fromUserWalletId = from.userWalletId
        val fromAccount = from.account
        val fromCurrency = from.currency
        val toUserWalletId = to.userWalletId
        val toAccount = to.account
        val toCurrency = to.currency

        // Act
        sut.storeSwapTransaction(
            fromSwapCurrencyStatus = from,
            toSwapCurrencyStatus = to,
            amount = SwapAmount(value = BigDecimal.ONE, decimals = 18),
            swapProvider = buildSwapProvider(),
            swapDataModel = swapDataModel,
            timestamp = 0L,
            txExternalUrl = null,
            txExternalId = null,
            averageDuration = null,
        )

        // Assert
        coVerify(exactly = 1) {
            recordTokenTransferFromSwapUseCase(
                fromUserWalletId = fromUserWalletId,
                fromAccount = fromAccount,
                fromCurrency = fromCurrency,
                toUserWalletId = toUserWalletId,
                toAccount = toAccount,
                toCurrency = toCurrency,
            )
        }
    }
}