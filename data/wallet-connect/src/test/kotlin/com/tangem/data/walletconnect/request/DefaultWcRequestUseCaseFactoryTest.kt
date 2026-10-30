package com.tangem.data.walletconnect.request

import com.google.common.truth.Truth.assertThat
import com.tangem.data.walletconnect.request.WcRequestHandlersFixture.Companion.BITCOIN_MAINNET
import com.tangem.data.walletconnect.request.WcRequestHandlersFixture.Companion.BITCOIN_SIGN_MESSAGE_PARAMS
import com.tangem.data.walletconnect.request.WcRequestHandlersFixture.Companion.ETH_MAINNET
import com.tangem.data.walletconnect.request.WcRequestHandlersFixture.Companion.ETH_PERSONAL_SIGN_PARAMS
import com.tangem.data.walletconnect.request.WcRequestHandlersFixture.Companion.SOLANA_MAINNET
import com.tangem.data.walletconnect.request.WcRequestHandlersFixture.Companion.SOLANA_SIGN_MESSAGE_PARAMS
import com.tangem.data.walletconnect.request.WcRequestHandlersFixture.Companion.SOLANA_SIGN_TRANSACTION_PARAMS
import com.tangem.domain.walletconnect.model.HandleMethodError
import com.tangem.domain.walletconnect.usecase.method.WcMethodUseCase
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultWcRequestUseCaseFactoryTest {

    private val fixture = WcRequestHandlersFixture()

    private val factory = DefaultWcRequestUseCaseFactory(
        requestConverters = fixture.handlers,
        namespaceConverters = fixture.namespaceConverters,
        analytics = mockk(relaxed = true),
    )

    @BeforeEach
    fun setup() {
        fixture.reset()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN method under foreign namespace chainId WHEN createUseCase THEN rejected without creating use case`(
        model: ForeignNamespaceModel,
    ) = runTest {
        // Arrange
        val request = fixture.createRequest(method = model.method, chainId = model.chainId, params = model.params)

        // Act
        val result = factory.createUseCase<WcMethodUseCase>(request)

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(HandleMethodError.UnknownError("Failed to create WcUseCase"))
        verify(exactly = 0) { fixture.ethMessageSignFactory.create(any(), any()) }
        verify(exactly = 0) { fixture.solanaMessageSignFactory.create(any(), any()) }
        verify(exactly = 0) { fixture.solanaSignTransactionFactory.create(any(), any()) }
        verify(exactly = 0) { fixture.bitcoinSignMessageFactory.create(any(), any()) }
    }

    @Test
    fun `GIVEN solana_signMessage on solana chainId WHEN createUseCase THEN solana use case created`() = runTest {
        // Arrange
        val request = fixture.createRequest("solana_signMessage", SOLANA_MAINNET, SOLANA_SIGN_MESSAGE_PARAMS)

        // Act
        val result = factory.createUseCase<WcMethodUseCase>(request)

        // Assert
        assertThat(result.isRight()).isTrue()
        verify(exactly = 1) { fixture.solanaMessageSignFactory.create(any(), any()) }
    }

    @Test
    fun `GIVEN personal_sign on eip155 chainId WHEN createUseCase THEN eth use case created`() = runTest {
        // Arrange
        val request = fixture.createRequest("personal_sign", ETH_MAINNET, ETH_PERSONAL_SIGN_PARAMS)

        // Act
        val result = factory.createUseCase<WcMethodUseCase>(request)

        // Assert
        assertThat(result.isRight()).isTrue()
        verify(exactly = 1) { fixture.ethMessageSignFactory.create(any(), any()) }
    }

    @Test
    fun `GIVEN signMessage on bip122 chainId WHEN createUseCase THEN bitcoin use case created`() = runTest {
        // Arrange
        val request = fixture.createRequest("signMessage", BITCOIN_MAINNET, BITCOIN_SIGN_MESSAGE_PARAMS)

        // Act
        val result = factory.createUseCase<WcMethodUseCase>(request)

        // Assert
        assertThat(result.isRight()).isTrue()
        verify(exactly = 1) { fixture.bitcoinSignMessageFactory.create(any(), any()) }
    }

    data class ForeignNamespaceModel(val method: String, val chainId: String, val params: String)

    private fun provideTestModels() = listOf(
        ForeignNamespaceModel("solana_signMessage", ETH_MAINNET, SOLANA_SIGN_MESSAGE_PARAMS),
        ForeignNamespaceModel("solana_signTransaction", ETH_MAINNET, SOLANA_SIGN_TRANSACTION_PARAMS),
        ForeignNamespaceModel("solana_signMessage", BITCOIN_MAINNET, SOLANA_SIGN_MESSAGE_PARAMS),
        ForeignNamespaceModel("personal_sign", SOLANA_MAINNET, ETH_PERSONAL_SIGN_PARAMS),
        ForeignNamespaceModel("personal_sign", BITCOIN_MAINNET, ETH_PERSONAL_SIGN_PARAMS),
        ForeignNamespaceModel("signMessage", ETH_MAINNET, BITCOIN_SIGN_MESSAGE_PARAMS),
        ForeignNamespaceModel("signMessage", SOLANA_MAINNET, BITCOIN_SIGN_MESSAGE_PARAMS),
        ForeignNamespaceModel("solana_signMessage", "", SOLANA_SIGN_MESSAGE_PARAMS),
    )
}