package com.tangem.data.moneyaccount.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.currency.MoneyAccountCurrencyFactory
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class MoneyAccountStatusValueDMConverterTest {

    private val cryptoCurrency: CryptoCurrency.Token = mockk()
    private val moneyAccountCurrencyFactory: MoneyAccountCurrencyFactory = mockk {
        every { create(USER_WALLET_ID) } returns cryptoCurrency
    }

    private val converter = MoneyAccountStatusValueDMConverter(moneyAccountCurrencyFactory)

    @Test
    fun `GIVEN active account with known bank off-ramp WHEN cached and restored THEN availability is unknown`() {
        // Arrange
        val active = MoneyAccountStatusValue.Account.Active(
            source = StatusSource.ACTUAL,
            cryptoCurrency = cryptoCurrency,
            balance = MoneyAccountStatusValue.Balance(
                cryptoAmount = BigDecimal.TEN,
                depositAddress = "0xdeposit",
                currencyCode = "USD",
                fiatRate = BigDecimal.ONE,
            ),
            customerId = "customer-1",
            isBankOfframpAvailable = true,
        )

        // Act
        val restored = converter.convertBack(userWalletId = USER_WALLET_ID, value = converter.convert(active))

        // Assert
        assertThat(restored).isEqualTo(active.copy(source = StatusSource.CACHE, isBankOfframpAvailable = null))
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
    }
}