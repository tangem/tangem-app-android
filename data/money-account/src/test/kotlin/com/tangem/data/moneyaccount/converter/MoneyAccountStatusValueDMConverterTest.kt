package com.tangem.data.moneyaccount.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.currency.MoneyAccountCurrencyFactory
import com.tangem.spend.datasource.tangempay.entity.MoneyAccountStatusValueDM
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal

internal class MoneyAccountStatusValueDMConverterTest {

    private val cryptoCurrency: CryptoCurrency.Token = mockk()
    private val moneyAccountCurrencyFactory: MoneyAccountCurrencyFactory = mockk {
        every { create(USER_WALLET_ID) } returns cryptoCurrency
    }

    private val converter = MoneyAccountStatusValueDMConverter(moneyAccountCurrencyFactory)

    @Test
    fun `GIVEN active account WHEN cached and restored THEN product instance kept and bank off-ramp unknown`() {
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
            productInstanceId = "instance-1",
            isBankOfframpAvailable = true,
            error = null,
        )

        // Act
        val restored = converter.convertBack(userWalletId = USER_WALLET_ID, value = converter.convert(active))

        // Assert
        assertThat(restored).isEqualTo(active.copy(source = StatusSource.CACHE, isBankOfframpAvailable = null))
    }

    @Test
    fun `GIVEN active account cached without product instance WHEN restored THEN status unavailable`() {
        // Arrange
        val cached = MoneyAccountStatusValueDM.ActiveAccount(customerId = "customer-1", balance = null)

        // Act
        val restored = converter.convertBack(userWalletId = USER_WALLET_ID, value = cached)

        // Assert
        assertThat(restored).isEqualTo(MoneyAccountStatusValue.Error.Unavailable)
    }

    @Test
    fun `GIVEN failed onboarding WHEN cached and restored THEN failed from cache`() {
        // Arrange
        val failed = MoneyAccountStatusValue.Onboarding.Failed(
            source = StatusSource.ACTUAL,
            customerId = "customer-1",
        )

        // Act
        val cached = converter.convert(failed)
        val restored = converter.convertBack(userWalletId = USER_WALLET_ID, value = cached)

        // Assert
        assertThat(cached).isEqualTo(MoneyAccountStatusValueDM.Failed(customerId = "customer-1"))
        assertThat(restored).isEqualTo(failed.copy(source = StatusSource.CACHE))
    }

    @ParameterizedTest
    @EnumSource(MoneyAccountStatusValue.Onboarding.Activating.Step::class)
    fun `GIVEN activating onboarding WHEN cached and restored THEN step kept from cache`(
        step: MoneyAccountStatusValue.Onboarding.Activating.Step,
    ) {
        // Arrange
        val activating = MoneyAccountStatusValue.Onboarding.Activating(
            source = StatusSource.ACTUAL,
            customerId = "customer-1",
            step = step,
        )

        // Act
        val cached = converter.convert(activating)
        val restored = converter.convertBack(userWalletId = USER_WALLET_ID, value = cached)

        // Assert
        assertThat(restored).isEqualTo(activating.copy(source = StatusSource.CACHE))
    }

    @Test
    fun `GIVEN active account with refresh error WHEN cached and restored THEN error dropped`() {
        // Arrange
        val active = MoneyAccountStatusValue.Account.Active(
            source = StatusSource.ONLY_CACHE,
            cryptoCurrency = cryptoCurrency,
            balance = null,
            customerId = "customer-1",
            productInstanceId = "instance-1",
            error = MoneyAccountStatusValue.Error.NotSynced,
        )

        // Act
        val restored = converter.convertBack(userWalletId = USER_WALLET_ID, value = converter.convert(active))

        // Assert
        assertThat(restored).isEqualTo(active.copy(source = StatusSource.CACHE, error = null))
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
    }
}