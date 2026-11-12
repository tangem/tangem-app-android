package com.tangem.data.tangempay.flow

import com.google.common.truth.Truth.assertThat
import com.tangem.data.tangempay.store.PaymentAccountStatusesStore
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.tangempay.flow.PaymentAccountStatusProducer
import com.tangem.domain.tangempay.usecase.order.ApplyActiveOrdersUseCase
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.test.core.getEmittedValues
import com.tangem.test.mock.MockAccounts
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultPaymentAccountStatusProducerTest {

    private val paymentAccountStatusesStore: PaymentAccountStatusesStore = mockk()
    private val applyActiveOrdersUseCase: ApplyActiveOrdersUseCase = mockk()
    private val tangemPayFeatureToggles: TangemPayFeatureToggles = mockk()

    private val producer = DefaultPaymentAccountStatusProducer(
        params = PaymentAccountStatusProducer.Params(MockAccounts.userWalletId),
        flowProducerTools = mockk(),
        paymentAccountStatusesStore = paymentAccountStatusesStore,
        applyActiveOrdersUseCase = applyActiveOrdersUseCase,
        tangemPayFeatureToggles = tangemPayFeatureToggles,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(paymentAccountStatusesStore, applyActiveOrdersUseCase, tangemPayFeatureToggles)
        every { paymentAccountStatusesStore.get(MockAccounts.userWalletId) } returns flowOf(CACHED_STATUS)
    }

    @Test
    fun `GIVEN order framework enabled WHEN produce THEN emits the status with active orders applied`() = runTest {
        // Arrange
        every { tangemPayFeatureToggles.isOrderFrameworkEnabled } returns true
        every { applyActiveOrdersUseCase(MockAccounts.userWalletId, any()) } returns flowOf(STATUS_WITH_ORDERS)

        // Act
        val emitted = getEmittedValues(producer.produce())
        runCurrent()

        // Assert
        assertThat(emitted).containsExactly(STATUS_WITH_ORDERS)
    }

    @Test
    fun `GIVEN order framework disabled WHEN produce THEN emits the cached status as is`() = runTest {
        // Arrange
        every { tangemPayFeatureToggles.isOrderFrameworkEnabled } returns false

        // Act
        val emitted = getEmittedValues(producer.produce())
        runCurrent()

        // Assert
        assertThat(emitted).containsExactly(CACHED_STATUS)
        verify(exactly = 0) { applyActiveOrdersUseCase(any(), any()) }
    }

    private companion object {
        val CACHED_STATUS = MockAccounts.createPaymentAccountStatus()
        val STATUS_WITH_ORDERS = CACHED_STATUS.copy(value = PaymentAccountStatusValue.Empty)
    }
}