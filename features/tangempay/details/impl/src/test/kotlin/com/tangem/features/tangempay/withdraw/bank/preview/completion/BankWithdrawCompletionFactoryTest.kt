package com.tangem.features.tangempay.withdraw.bank.preview.completion

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpOrderResult
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpQuote
import com.tangem.domain.tangempay.model.bankofframp.BankRail
import com.tangem.domain.tangempay.model.bankofframp.OfframpDestination
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawSourceAccount
import com.tangem.features.tangempay.withdraw.bank.offrampDestination
import com.tangem.features.tangempay.withdraw.bank.preview.model.PreviewFailure
import com.tangem.features.tangempay.withdraw.bank.savedRecipientDraft
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class BankWithdrawCompletionFactoryTest {

    private val userWalletId = UserWalletId("011")
    private val orderCompletion: OrderBankWithdrawCompletion = mockk()
    private val orderCompletionFactory: OrderBankWithdrawCompletion.Factory = mockk()
    private val routeCompletion: RouteBankWithdrawCompletion = mockk()
    private val routeCompletionFactory: RouteBankWithdrawCompletion.Factory = mockk()
    private val speiCompletion: SpeiBankWithdrawCompletion = mockk()
    private val speiCompletionFactory: SpeiBankWithdrawCompletion.Factory = mockk()
    private val factory = BankWithdrawCompletionFactory(
        orderCompletionFactory = orderCompletionFactory,
        routeCompletionFactory = routeCompletionFactory,
        speiCompletionFactory = speiCompletionFactory,
    )
    private val onOrderCreated: (BankOfframpOrderResult) -> Unit = {}
    private val onDestinationChanged: (OfframpDestination) -> Unit = {}
    private val onNetworkFeeChanged: (BigDecimal) -> Unit = {}
    private val onQuoteChanged: (BankOfframpQuote) -> Unit = {}
    private val callbacks = BankWithdrawCompletionFactory.Callbacks(
        onOrderCreated = onOrderCreated,
        onDestinationChanged = onDestinationChanged,
        onNetworkFeeChanged = onNetworkFeeChanged,
        onQuoteChanged = onQuoteChanged,
    )
    private val onState: (CompletionState) -> Unit = {}

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            orderCompletion,
            orderCompletionFactory,
            routeCompletion,
            routeCompletionFactory,
            speiCompletion,
            speiCompletionFactory,
        )
        every {
            routeCompletionFactory.create(userWalletId, PRODUCT_INSTANCE_ID, onDestinationChanged, onNetworkFeeChanged)
        } returns routeCompletion
        every {
            speiCompletionFactory.create(userWalletId, PRODUCT_INSTANCE_ID, onQuoteChanged, onNetworkFeeChanged)
        } returns speiCompletion
        coEvery { routeCompletion.complete(any(), any()) } returns Unit
        coEvery { speiCompletion.complete(any(), any()) } returns Unit
        coEvery { routeCompletion.prepare(any()) } returns Unit
        coEvery { speiCompletion.prepare(any()) } returns Unit
    }

    @Test
    fun `GIVEN payment account WHEN create THEN order completion of its wallet`() = runTest {
        // Arrange
        every { orderCompletionFactory.create(userWalletId, backgroundScope, onOrderCreated) } returns orderCompletion

        // Act
        val actual = factory.create(
            sourceAccount = BankWithdrawSourceAccount.Payment(userWalletId),
            scope = backgroundScope,
            callbacks = callbacks,
        )

        // Assert
        assertThat(actual).isSameInstanceAs(orderCompletion)
        verify(exactly = 0) { routeCompletionFactory.create(any(), any(), any(), any()) }
        verify(exactly = 0) { speiCompletionFactory.create(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN money account and an ACH draft WHEN complete THEN the route completion completes it`() = runTest {
        // Arrange
        val draft = savedRecipientDraft().copy(destination = offrampDestination(rail = BankRail.ACH))
        val completion = createMoneyAccountCompletion()

        // Act
        completion.prepare(draft)
        completion.complete(draft, onState)

        // Assert
        coVerify(exactly = 1) { routeCompletion.prepare(draft) }
        coVerify(exactly = 1) { routeCompletion.complete(draft, onState) }
        coVerify(exactly = 0) { speiCompletion.complete(any(), any()) }
        verify(exactly = 0) { orderCompletionFactory.create(any(), any(), any()) }
    }

    @Test
    fun `GIVEN money account and a SPEI draft WHEN complete THEN the quote completion completes it`() = runTest {
        // Arrange
        val draft = savedRecipientDraft().copy(destination = offrampDestination(rail = BankRail.SPEI))
        val completion = createMoneyAccountCompletion()

        // Act
        completion.prepare(draft)
        completion.complete(draft, onState)

        // Assert
        coVerify(exactly = 1) { speiCompletion.prepare(draft) }
        coVerify(exactly = 1) { speiCompletion.complete(draft, onState) }
        coVerify(exactly = 0) { routeCompletion.complete(any(), any()) }
    }

    @Test
    fun `GIVEN money account without product instance WHEN complete THEN transfers unavailable failure`() = runTest {
        // Arrange
        val states = mutableListOf<CompletionState>()
        val completion = factory.create(
            sourceAccount = BankWithdrawSourceAccount.Money(userWalletId = userWalletId, productInstanceId = null),
            scope = backgroundScope,
            callbacks = callbacks,
        )

        // Act
        completion.complete(savedRecipientDraft(), states::add)

        // Assert
        assertThat(states).containsExactly(CompletionState.Failed(PreviewFailure.UnsupportedConfiguration))
        verify(exactly = 0) { routeCompletionFactory.create(any(), any(), any(), any()) }
        verify(exactly = 0) { speiCompletionFactory.create(any(), any(), any(), any()) }
    }

    private fun TestScope.createMoneyAccountCompletion() = factory.create(
        sourceAccount = BankWithdrawSourceAccount.Money(
            userWalletId = userWalletId,
            productInstanceId = PRODUCT_INSTANCE_ID,
        ),
        scope = backgroundScope,
        callbacks = callbacks,
    )

    private companion object {
        const val PRODUCT_INSTANCE_ID = "pi-1"
    }
}