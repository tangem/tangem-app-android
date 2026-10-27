package com.tangem.features.tangempay.withdraw.bank.preview.completion

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderResult
import com.tangem.domain.pay.model.bankofframp.OfframpDestination
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawSourceAccount
import com.tangem.features.tangempay.withdraw.bank.preview.model.PreviewFailure
import com.tangem.features.tangempay.withdraw.bank.savedRecipientDraft
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class BankWithdrawCompletionFactoryTest {

    private val userWalletId = UserWalletId("011")
    private val orderCompletion: OrderBankWithdrawCompletion = mockk()
    private val orderCompletionFactory: OrderBankWithdrawCompletion.Factory = mockk()
    private val routeCompletion: RouteBankWithdrawCompletion = mockk()
    private val routeCompletionFactory: RouteBankWithdrawCompletion.Factory = mockk()
    private val factory = BankWithdrawCompletionFactory(orderCompletionFactory, routeCompletionFactory)
    private val onOrderCreated: (BankOfframpOrderResult) -> Unit = {}
    private val onDestinationChanged: (OfframpDestination) -> Unit = {}

    @BeforeEach
    fun resetMocks() {
        clearMocks(orderCompletion, orderCompletionFactory, routeCompletion, routeCompletionFactory)
    }

    @Test
    fun `GIVEN payment account WHEN create THEN order completion of its wallet`() = runTest {
        // Arrange
        every { orderCompletionFactory.create(userWalletId, backgroundScope, onOrderCreated) } returns orderCompletion

        // Act
        val actual = factory.create(
            sourceAccount = BankWithdrawSourceAccount.Payment(userWalletId),
            scope = backgroundScope,
            onOrderCreated = onOrderCreated,
            onDestinationChanged = onDestinationChanged,
        )

        // Assert
        assertThat(actual).isSameInstanceAs(orderCompletion)
        verify(exactly = 0) { routeCompletionFactory.create(any(), any(), any()) }
    }

    @Test
    fun `GIVEN money account WHEN create THEN route completion of its product instance`() = runTest {
        // Arrange
        every {
            routeCompletionFactory.create(userWalletId, PRODUCT_INSTANCE_ID, onDestinationChanged)
        } returns routeCompletion

        // Act
        val actual = factory.create(
            sourceAccount = BankWithdrawSourceAccount.Money(
                userWalletId = userWalletId,
                productInstanceId = PRODUCT_INSTANCE_ID,
            ),
            scope = backgroundScope,
            onOrderCreated = onOrderCreated,
            onDestinationChanged = onDestinationChanged,
        )

        // Assert
        assertThat(actual).isSameInstanceAs(routeCompletion)
        verify(exactly = 0) { orderCompletionFactory.create(any(), any(), any()) }
    }

    @Test
    fun `GIVEN money account without product instance WHEN complete THEN transfers unavailable failure`() = runTest {
        // Arrange
        val states = mutableListOf<CompletionState>()
        val completion = factory.create(
            sourceAccount = BankWithdrawSourceAccount.Money(userWalletId = userWalletId, productInstanceId = null),
            scope = backgroundScope,
            onOrderCreated = onOrderCreated,
            onDestinationChanged = onDestinationChanged,
        )

        // Act
        completion.complete(savedRecipientDraft(), states::add)

        // Assert
        assertThat(states).containsExactly(CompletionState.Failed(PreviewFailure.UnsupportedConfiguration))
        verify(exactly = 0) { routeCompletionFactory.create(any(), any(), any()) }
    }

    private companion object {
        const val PRODUCT_INSTANCE_ID = "pi-1"
    }
}