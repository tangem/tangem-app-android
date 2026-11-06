package com.tangem.features.tangempay.withdraw.bank.amount.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawSourceAccount
import com.tangem.features.tangempay.withdraw.bank.model.MoneyAccountTransfer
import com.tangem.features.tangempay.withdraw.bank.paymentToken
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class BankWithdrawNetworkFeeLoaderTest {

    private val userWalletId = UserWalletId("011")
    private val token = paymentToken()
    private val moneyAccountTransfer: MoneyAccountTransfer = mockk()
    private val loader = BankWithdrawNetworkFeeLoader(moneyAccountTransfer)

    @BeforeEach
    fun resetMocks() {
        clearMocks(moneyAccountTransfer)
    }

    @Test
    fun `GIVEN payment account WHEN load THEN no network fee and nothing requested`() = runTest {
        // Act
        val actual = loader.load(BankWithdrawSourceAccount.Payment(userWalletId), token)

        // Assert
        assertThat(actual).isEqualTo(BigDecimal.ZERO.right())
        coVerify(exactly = 0) { moneyAccountTransfer.estimateFee(any(), any()) }
    }

    @Test
    fun `GIVEN money account WHEN load THEN the fee estimated for an account transfer`() = runTest {
        // Arrange
        coEvery { moneyAccountTransfer.estimateFee(userWalletId, token) } returns BigDecimal("0.01107").right()

        // Act
        val actual = loader.load(BankWithdrawSourceAccount.Money(userWalletId, productInstanceId = "instance-1"), token)

        // Assert
        assertThat(actual).isEqualTo(BigDecimal("0.01107").right())
    }
}