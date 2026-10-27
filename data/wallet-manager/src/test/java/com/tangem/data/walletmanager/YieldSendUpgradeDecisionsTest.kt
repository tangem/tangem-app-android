package com.tangem.data.walletmanager

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.smartcontract.CompiledSmartContractCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyUpgradeToAndCallCallData
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class YieldSendUpgradeDecisionsTest {

    private var nowMillis = 0L
    private lateinit var decisions: YieldSendUpgradeDecisions

    private val userWalletId = UserWalletId("011")
    private val factory = MockCryptoCurrencyFactory()
    private val ethereum = factory.ethereum.network
    private val sendCallData = CompiledSmartContractCallData(byteArrayOf(1, 2, 3))
    private val wrappedCallData = EthereumYieldSupplyUpgradeToAndCallCallData(
        newImplementation = LATEST_IMPLEMENTATION,
        innerCallData = CompiledSmartContractCallData(byteArrayOf(9)),
    )

    @BeforeEach
    fun setUp() {
        nowMillis = 0L
        decisions = YieldSendUpgradeDecisions(currentTimeMillis = { nowMillis })
    }

    @Test
    fun `GIVEN wrapped estimate WHEN applyTo THEN call is wrapped into the same implementation`() = runTest {
        // Arrange
        decisions.remember(userWalletId, ethereum) { wrappedCallData }

        // Act
        val actual = decisions.applyTo(userWalletId, ethereum, sendCallData)

        // Assert
        assertThat(actual).isInstanceOf(EthereumYieldSupplyUpgradeToAndCallCallData::class.java)
        val wrapped = actual as EthereumYieldSupplyUpgradeToAndCallCallData
        assertThat(wrapped.newImplementation).isEqualTo(LATEST_IMPLEMENTATION)
        assertThat(wrapped.innerCallData).isSameInstanceAs(sendCallData)
    }

    @Test
    fun `GIVEN bare estimate WHEN applyTo THEN call is left bare`() = runTest {
        // Arrange
        decisions.remember(userWalletId, ethereum) { CompiledSmartContractCallData(byteArrayOf(9)) }

        // Act
        val actual = decisions.applyTo(userWalletId, ethereum, sendCallData)

        // Assert
        assertThat(actual).isSameInstanceAs(sendCallData)
    }

    @Test
    fun `GIVEN estimate older than its lifetime WHEN applyTo THEN no decision`() = runTest {
        // Arrange
        decisions.remember(userWalletId, ethereum) { wrappedCallData }
        nowMillis += (5.minutes + 1.seconds).inWholeMilliseconds

        // Act
        val actual = decisions.applyTo(userWalletId, ethereum, sendCallData)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN forgotten decision WHEN applyTo THEN no decision`() = runTest {
        // Arrange
        decisions.remember(userWalletId, ethereum) { wrappedCallData }
        decisions.forget(userWalletId, ethereum)

        // Act
        val actual = decisions.applyTo(userWalletId, ethereum, sendCallData)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN decision forgotten while it is priced WHEN applyTo THEN no decision`() = runTest {
        // Arrange
        decisions.remember(userWalletId, ethereum) {
            decisions.forget(userWalletId, ethereum)
            wrappedCallData
        }

        // Act
        val actual = decisions.applyTo(userWalletId, ethereum, sendCallData)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN decision priced after forget WHEN applyTo THEN call follows the new decision`() = runTest {
        // Arrange
        decisions.remember(userWalletId, ethereum) { wrappedCallData }
        decisions.forget(userWalletId, ethereum)
        decisions.remember(userWalletId, ethereum) { CompiledSmartContractCallData(byteArrayOf(9)) }

        // Act
        val actual = decisions.applyTo(userWalletId, ethereum, sendCallData)

        // Assert
        assertThat(actual).isSameInstanceAs(sendCallData)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN estimate for another wallet or network WHEN applyTo THEN no decision`(model: OtherSendModel) = runTest {
        // Arrange
        decisions.remember(model.userWalletId, model.network) { wrappedCallData }

        // Act
        val actual = decisions.applyTo(userWalletId, ethereum, sendCallData)

        // Assert
        assertThat(actual).isNull()
    }

    private fun provideTestModels() = listOf(
        OtherSendModel(userWalletId = UserWalletId("022"), network = ethereum),
        OtherSendModel(userWalletId = userWalletId, network = factory.createCoin(Blockchain.Polygon).network),
    )

    data class OtherSendModel(val userWalletId: UserWalletId, val network: Network)

    private companion object {
        const val LATEST_IMPLEMENTATION = "0x5555555555555555555555555555555555555555"
    }
}