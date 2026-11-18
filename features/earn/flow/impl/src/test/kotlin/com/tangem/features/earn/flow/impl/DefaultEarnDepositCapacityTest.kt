package com.tangem.features.earn.flow.impl

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.tokens.IsAmountSubtractAvailableUseCase
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.features.earn.flow.api.EarnDepositCapacity
import com.tangem.features.earn.flow.impl.model.EarnFlowStatuses
import com.tangem.features.earn.flow.impl.model.testEthereum
import com.tangem.features.earn.flow.impl.model.testFee
import com.tangem.features.earn.flow.impl.model.testUserWalletId
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.session.Deposit
import com.tangem.features.earn.product.api.session.EarnSession
import com.tangem.features.earn.product.api.session.EarnSessionError
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.logging.TangemLogger
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultEarnDepositCapacityTest {

    private val session: EarnSession = mockk()
    private val isAmountSubtractAvailableUseCase: IsAmountSubtractAvailableUseCase = mockk()

    private val statuses = EarnFlowStatuses(
        singleAccountStatusListSupplier = mockk<SingleAccountStatusListSupplier>(),
        getSelectedAppCurrencyUseCase = mockk<GetSelectedAppCurrencyUseCase>(),
        getFeePaidCryptoCurrencyStatusSyncUseCase = mockk<GetFeePaidCryptoCurrencyStatusSyncUseCase>(),
        isAmountSubtractAvailableUseCase = isAmountSubtractAvailableUseCase,
    )

    private val params = EarnProduct.Params(userWalletId = testUserWalletId, currency = testEthereum)

    private var createdFor: EarnProduct.Params? = null

    private val capacity = DefaultEarnDepositCapacity(
        sessionFactories = EarnSessionFactories(
            mapOf(PRODUCT_ID.value to EarnSession.Factory { params -> session.also { createdFor = params } }),
        ),
        statuses = statuses,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(session, isAmountSubtractAvailableUseCase)
        createdFor = null
        mockkObject(TangemLogger)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(TangemLogger)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun availableMax(model: AvailableMaxModel) = runTest {
        // Arrange
        coEvery { session.maxAmount(action = any()) } returns model.max
        coEvery { session.loadFee(action = any(), amount = any()) } returns model.fee
        coEvery {
            isAmountSubtractAvailableUseCase.invoke(userWalletId = any(), currency = any(), maybeGaslessFee = any())
        } returns model.isSubtractAvailable.right()

        // Act
        val actual = capacity.availableMax(productId = model.productId, params = params, action = Deposit())

        // Assert
        assertThat(actual.isLeft()).isEqualTo(model.expected == null)
        actual.onRight { assertThat(it).isEqualToIgnoringScale(model.expected) }
    }

    @Test
    fun `GIVEN a deposit WHEN availableMax THEN the fee is estimated for the whole source of this product`() = runTest {
        // Arrange
        coEvery { session.maxAmount(action = any()) } returns BigDecimal.ONE.right()
        coEvery { session.loadFee(action = any(), amount = any()) } returns TransactionFee.Single(testFee("0.01")).right()
        coEvery {
            isAmountSubtractAvailableUseCase.invoke(userWalletId = any(), currency = any(), maybeGaslessFee = any())
        } returns true.right()

        // Act
        capacity.availableMax(productId = PRODUCT_ID, params = params, action = Deposit())

        // Assert
        coVerify(exactly = 1) { session.loadFee(action = Deposit(), amount = null) }
        assertThat(createdFor).isEqualTo(params)
    }

    @Test
    fun `GIVEN fee estimate fails WHEN availableMax THEN the reason is logged`() = runTest {
        // Arrange
        coEvery { session.maxAmount(action = any()) } returns BigDecimal.ONE.right()
        coEvery { session.loadFee(action = any(), amount = any()) } returns GetFeeError.UnknownError.left()

        // Act
        capacity.availableMax(productId = PRODUCT_ID, params = params, action = Deposit())

        // Assert
        verify(exactly = 1) {
            TangemLogger.e(match { it.contains("lending") && it.contains("UnknownError") }, any(), any())
        }
    }

    private fun provideTestModels() = listOf(
        AvailableMaxModel(
            productId = EarnProductId("unknown"),
            expected = null,
        ),
        AvailableMaxModel(
            max = EarnSessionError.Failed().left(),
            expected = null,
        ),
        AvailableMaxModel(
            fee = GetFeeError.UnknownError.left(),
            expected = null,
        ),
        AvailableMaxModel(
            fee = TransactionFee.Single(Fee.Common(Amount(currencySymbol = "ETH", value = null, decimals = 18))).right(),
            expected = null,
        ),
        AvailableMaxModel(expected = BigDecimal("0.99")),
        AvailableMaxModel(
            fee = TransactionFee.Choosable(
                minimum = testFee("0.005"),
                normal = testFee("0.01"),
                priority = testFee("0.02"),
            ).right(),
            expected = BigDecimal("0.99"),
        ),
        AvailableMaxModel(isSubtractAvailable = false, expected = BigDecimal.ONE),
        AvailableMaxModel(
            max = BigDecimal("0.00001").right(),
            fee = TransactionFee.Single(testFee("0.0001")).right(),
            expected = BigDecimal.ZERO,
        ),
    )

    data class AvailableMaxModel(
        val productId: EarnProductId = PRODUCT_ID,
        val max: Either<EarnSessionError, BigDecimal> = BigDecimal.ONE.right(),
        val fee: Either<GetFeeError, TransactionFee> = TransactionFee.Single(testFee("0.01")).right(),
        val isSubtractAvailable: Boolean = true,
        val expected: BigDecimal?,
    )

    private companion object {
        val PRODUCT_ID = EarnProductId("lending")
    }
}