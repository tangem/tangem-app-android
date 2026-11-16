package com.tangem.data.walletmanager

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.data.walletmanager.SafeTestFixtures.jointNetwork
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.txhistory.TxHistoryFeatureToggles
import com.tangem.domain.yield.supply.YieldSupplyWrappedNativeFactory
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class InternalTransfersPolicyTest {

    private val toggles: TxHistoryFeatureToggles = mockk()
    private val wrappedNativeFactory: YieldSupplyWrappedNativeFactory = mockk()
    private val policy = InternalTransfersPolicy(toggles, wrappedNativeFactory)

    private val currencyFactory = MockCryptoCurrencyFactory()
    private val ethereum = currencyFactory.ethereum
    private val usdt = currencyFactory.createToken(Blockchain.Ethereum)
    private val chia = currencyFactory.chia

    @BeforeEach
    fun resetMocks() {
        clearMocks(toggles, wrappedNativeFactory)
        every { wrappedNativeFactory.create(any()) } returns null
        every { wrappedNativeFactory.create(ethereum.network) } returns usdt
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN currency and lending toggle WHEN shouldInclude THEN internal transfers requested as expected`(
        model: PolicyModel,
    ) {
        // Arrange
        every { toggles.isLendingHistoryEnabled } returns model.isLendingEnabled

        // Act
        val actual = policy.shouldInclude(model.currency)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        PolicyModel("lending coin, toggle on", ethereum, isLendingEnabled = true, expected = true),
        PolicyModel("lending coin, toggle off", ethereum, isLendingEnabled = false, expected = false),
        PolicyModel("coin without a wrapped native", chia, isLendingEnabled = true, expected = false),
        PolicyModel("token on a lending network", usdt, isLendingEnabled = true, expected = false),
        PolicyModel("joint coin, toggle off", ethereum.copy(network = jointNetwork), false, expected = true),
        PolicyModel("joint token", usdt.copy(network = jointNetwork), isLendingEnabled = true, expected = false),
    )

    data class PolicyModel(
        val name: String,
        val currency: CryptoCurrency,
        val isLendingEnabled: Boolean,
        val expected: Boolean,
    ) {
        override fun toString(): String = name
    }
}