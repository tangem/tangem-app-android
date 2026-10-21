package com.tangem.feature.swap

import com.tangem.domain.express.models.ExpressOperationType
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.swap.domain.models.domain.RateType
import com.tangem.grow.datasource.express.ExpressUtils
import com.tangem.grow.datasource.express.TangemExpressApi
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

internal class DefaultSwapRepositoryTest {

    private val tangemExpressApi: TangemExpressApi = mockk()
    private val expressUtils: ExpressUtils = mockk()
    private val userWallet: UserWallet = mockk()

    private val repository = DefaultSwapRepository(
        tangemExpressApi = tangemExpressApi,
        coroutineDispatcher = TestingCoroutineDispatcherProvider(),
        errorsDataConverter = mockk(),
        dataSignatureVerifier = mockk(),
        appPreferencesStore = mockk(),
        growPreferencesKeys = mockk(),
        expressUtils = expressUtils,
        expressHistoryRepository = mockk(),
        moshi = mockk(relaxed = true),
    )

    @BeforeEach
    fun setUp() {
        every { userWallet.walletId } returns UserWalletId(stringValue = "0123456789ABCDEF")
        coEvery { expressUtils.getRefCode(any()) } returns null
        coEvery {
            tangemExpressApi.getExchangeData(
                userWalletId = any(),
                refCode = any(),
                fromContractAddress = any(),
                fromNetwork = any(),
                toContractAddress = any(),
                fromAddress = any(),
                toNetwork = any(),
                fromAmount = any(),
                toAmount = any(),
                fromDecimals = any(),
                toDecimals = any(),
                providerId = any(),
                rateType = any(),
                toAddress = any(),
                requestId = any(),
                refundAddress = any(),
                refundExtraId = any(),
                partnerOperationType = any(),
                toExtraId = any(),
                quoteId = any(),
                intentId = any(),
            )
        } throws IOException("no connection")
    }

    @Test
    fun `GIVEN intent WHEN getExchangeData THEN express is asked for the intent`() = runTest {
        // Act
        getExchangeData(intentId = INTENT_ID)

        // Assert
        coVerifyExchangeDataRequested(intentId = INTENT_ID)
    }

    @Test
    fun `GIVEN no intent WHEN getExchangeData THEN express is asked without an intent`() = runTest {
        // Act
        getExchangeData(intentId = null)

        // Assert
        coVerifyExchangeDataRequested(intentId = null)
    }

    private suspend fun getExchangeData(intentId: String?) = repository.getExchangeData(
        userWallet = userWallet,
        fromContractAddress = "0",
        fromNetwork = "ethereum",
        toContractAddress = "0",
        fromAddress = "0xFrom",
        toNetwork = "binance-smart-chain",
        fromAmount = "1000000000000000000",
        fromDecimals = 18,
        toDecimals = 18,
        providerId = "changelly",
        rateType = RateType.FLOAT,
        toAddress = "0xTo",
        expressOperationType = ExpressOperationType.SWAP,
        refundAddress = "0xFrom",
        refundExtraId = null,
        toExtraId = null,
        intentId = intentId,
    )

    private fun coVerifyExchangeDataRequested(intentId: String?) = coVerify(exactly = 1) {
        tangemExpressApi.getExchangeData(
            userWalletId = any(),
            refCode = any(),
            fromContractAddress = any(),
            fromNetwork = any(),
            toContractAddress = any(),
            fromAddress = any(),
            toNetwork = any(),
            fromAmount = any(),
            toAmount = any(),
            fromDecimals = any(),
            toDecimals = any(),
            providerId = any(),
            rateType = any(),
            toAddress = any(),
            requestId = any(),
            refundAddress = any(),
            refundExtraId = any(),
            partnerOperationType = any(),
            toExtraId = any(),
            quoteId = any(),
            intentId = intentId,
        )
    }

    private companion object {
        const val INTENT_ID = "intent-id"
    }
}