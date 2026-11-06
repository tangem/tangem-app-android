package com.tangem.domain.pay.usecase

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.AccountFee
import com.tangem.domain.pay.model.AccountFeeGroupType
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class GetOnrampFeesUseCaseTest {

    private val getAccountFeesUseCase: GetAccountFeesUseCase = mockk()

    private val useCase = GetOnrampFeesUseCase(getAccountFeesUseCase = getAccountFeesUseCase)

    private val userWalletId = UserWalletId("011")
    private val product = TangemPayProduct.BANKING_ACCOUNT

    @BeforeEach
    fun resetMocks() {
        clearMocks(getAccountFeesUseCase)
    }

    @Test
    fun `GIVEN onramp fees WHEN invoke THEN only onramp group of the product requested`() = runTest {
        // Arrange
        val fees = listOf(
            AccountFee(
                type = "ACH_ONRAMP",
                name = "ACH",
                paymentMethodId = "ACH",
                group = AccountFeeGroupType.ONRAMP,
                calculationType = AccountFee.CalculationType.FIXED,
                amount = BigDecimal.ONE,
                currency = "USD",
            ),
        )
        coEvery { getAccountFeesUseCase(userWalletId, product, setOf(AccountFeeGroupType.ONRAMP)) } returns fees.right()

        // Act
        val actual = useCase(userWalletId, product)

        // Assert
        assertThat(actual).isEqualTo(fees.right())
        coVerify(exactly = 1) { getAccountFeesUseCase(userWalletId, product, setOf(AccountFeeGroupType.ONRAMP)) }
    }
}