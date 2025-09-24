package com.tangem.domain.tokens

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.serialization.SerializedBigDecimal
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.tokens.repository.YieldSupplyWarningsViewedRepository
import com.tangem.domain.tokens.mock.MockTokens
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class NeedShowYieldSupplyDepositedWarningUseCaseTest {

    @RelaxedMockK
    private lateinit var repository: YieldSupplyWarningsViewedRepository

    private lateinit var dispatchers: TestingCoroutineDispatcherProvider

    @BeforeEach
    fun setup() {
        dispatchers = TestingCoroutineDispatcherProvider()
    }

    @Test
    fun `GIVEN null status WHEN invoke THEN returns false`() = runTest {
        val useCase = NeedShowYieldSupplyDepositedWarningUseCase(repository, dispatchers)

        val result = useCase.invoke(null)

        assertThat(result).isFalse()
        coVerify(exactly = 0) { repository.getViewedWarnings() }
    }

    @Test
    fun `GIVEN inactive lending WHEN invoke THEN returns false`() = runTest {
        val useCase = NeedShowYieldSupplyDepositedWarningUseCase(repository, dispatchers)
        val status = createStatus(isActive = false)

        val result = useCase.invoke(status)

        assertThat(result).isFalse()
        coVerify(exactly = 0) { repository.getViewedWarnings() }
    }

    @Test
    fun `GIVEN active lending and not viewed WHEN invoke THEN returns true`() = runTest {
        val useCase = NeedShowYieldSupplyDepositedWarningUseCase(repository, dispatchers)
        val status = createStatus(isActive = true)
        coEvery { repository.getViewedWarnings() } returns emptySet()

        val result = useCase.invoke(status)

        assertThat(result).isTrue()
        coVerify(exactly = 1) { repository.getViewedWarnings() }
    }

    @Test
    fun `GIVEN active lending and already viewed WHEN invoke THEN returns false`() = runTest {
        val useCase = NeedShowYieldSupplyDepositedWarningUseCase(repository, dispatchers)
        val status = createStatus(isActive = true)
        coEvery { repository.getViewedWarnings() } returns setOf(status.currency.name)

        val result = useCase.invoke(status)

        assertThat(result).isFalse()
        coVerify(exactly = 1) { repository.getViewedWarnings() }
    }

    private fun createStatus(isActive: Boolean): CryptoCurrencyStatus {
        val currency = MockTokens.token1
        val yieldSupplyStatus = YieldSupplyStatus(
            isActive = isActive,
            isInitialized = true,
            isAllowedToSpend = true,
        )
        val value = CryptoCurrencyStatus.NoQuote(
            amount = SerializedBigDecimal.ZERO,
            yieldBalance = null,
            yieldSupplyStatus = yieldSupplyStatus,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(
                    value = "address",
                    type = NetworkAddress.Address.Type.Primary,
                ),
            ),
            sources = CryptoCurrencyStatus.Sources(),
        )

        return CryptoCurrencyStatus(
            currency = currency,
            value = value,
        )
    }
}