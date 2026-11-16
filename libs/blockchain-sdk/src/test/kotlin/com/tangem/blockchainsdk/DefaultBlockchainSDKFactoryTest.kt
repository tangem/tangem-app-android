package com.tangem.blockchainsdk

import com.tangem.blockchain.common.ExceptionHandler
import com.tangem.blockchainsdk.analytics.BlockchainExceptionHandler
import com.tangem.blockchainsdk.providers.BlockchainProvidersTypesManager
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultBlockchainSDKFactoryTest {

    private val blockchainProvidersTypesManager: BlockchainProvidersTypesManager = mockk()
    private val blockchainExceptionHandler: BlockchainExceptionHandler = mockk()

    @BeforeEach
    fun mockExceptionHandler() {
        mockkObject(ExceptionHandler)
        every { ExceptionHandler.append(any()) } just runs
        every { blockchainProvidersTypesManager.get() } returns emptyFlow()
    }

    @AfterEach
    fun unmockExceptionHandler() {
        unmockkObject(ExceptionHandler)
    }

    @Test
    fun `GIVEN exception handler WHEN factory created THEN handler is registered with the SDK`() {
        // Act
        DefaultBlockchainSDKFactory(
            blockchainSdkConfig = mockk(),
            blockchainProvidersTypesManager = blockchainProvidersTypesManager,
            walletManagerFactoryCreator = mockk(),
            blockchainExceptionHandler = blockchainExceptionHandler,
            dispatchers = TestingCoroutineDispatcherProvider(),
        )

        // Assert
        verify(exactly = 1) { ExceptionHandler.append(blockchainExceptionHandler) }
    }
}