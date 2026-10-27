package com.tangem.features.tangempay.withdraw.bank.preview.completion

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderResult
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
    private val factory = BankWithdrawCompletionFactory(orderCompletionFactory)

    @BeforeEach
    fun resetMocks() {
        clearMocks(orderCompletion, orderCompletionFactory)
    }

    @Test
    fun `GIVEN payment account WHEN create THEN order completion of its wallet`() = runTest {
        // Arrange
        val onOrderCreated: (BankOfframpOrderResult) -> Unit = {}
        every { orderCompletionFactory.create(userWalletId, backgroundScope, onOrderCreated) } returns orderCompletion

        // Act
        val actual = factory.create(
            sourceAccount = BankWithdrawSourceAccount.Payment(userWalletId),
            scope = backgroundScope,
            onOrderCreated = onOrderCreated,
        )

        // Assert
        assertThat(actual).isSameInstanceAs(orderCompletion)
    }

    @Test
    fun `GIVEN money account WHEN complete THEN transfers unavailable failure reported`() = runTest {
        // Arrange
        val states = mutableListOf<CompletionState>()
        val completion = factory.create(
            sourceAccount = BankWithdrawSourceAccount.Money(userWalletId = userWalletId, productInstanceId = "pi-1"),
            scope = backgroundScope,
            onOrderCreated = {},
        )

        // Act
        completion.complete(savedRecipientDraft(), states::add)

        // Assert
        assertThat(states).containsExactly(CompletionState.Failed(PreviewFailure.UnsupportedConfiguration))
        verify(exactly = 0) { orderCompletionFactory.create(any(), any(), any()) }
    }
}