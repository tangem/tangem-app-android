package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.child.wallet.model.intents.MoneyAccountIntents
import com.tangem.features.moneyaccount.main.entity.MoneyAccountMainUM
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MoneyAccountMainBlockConverterTest {

    private val moneyAccountIntents: MoneyAccountIntents = mockk(relaxed = true)

    private val converter = MoneyAccountMainBlockConverter(moneyAccountIntents)

    @BeforeEach
    fun resetMocks() {
        clearMocks(moneyAccountIntents)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN activating step WHEN convert THEN onboarding with step subtitle opens activating dialog`(
        model: ActivatingModel,
    ) {
        // Arrange
        val status = AccountStatus.Money(
            account = Account.Money(UserWalletId("0011")),
            value = MoneyAccountStatusValue.Onboarding.Activating(
                source = StatusSource.ACTUAL,
                customerId = "customer",
                step = model.step,
            ),
        )

        // Act
        val result = converter.convert(status) as MoneyAccountMainUM.Onboarding
        result.onClick()

        // Assert
        assertThat(result.subtitle).isEqualTo(resourceReference(model.expectedSubtitleRes))
        verify(exactly = 1) { moneyAccountIntents.onMoneyAccountActivatingClicked() }
    }

    @Test
    fun `GIVEN active account with kyc additional info WHEN convert THEN content with action required subtitle`() {
        // Act
        val result = converter.convert(moneyAccount(activeValue(isKycAdditionalInfoRequired = true)))

        // Assert
        assertThat(result).isInstanceOf(MoneyAccountMainUM.Content::class.java)
        val content = result as MoneyAccountMainUM.Content
        assertThat(content.subtitle).isEqualTo(resourceReference(R.string.tangempay_action_required_title))
        assertThat(content.isActionRequired).isTrue()
        assertThat(content.balance).isNotNull()
    }

    @Test
    fun `GIVEN active account without flag WHEN convert THEN content with active subtitle`() {
        // Act
        val result = converter.convert(moneyAccount(activeValue(isKycAdditionalInfoRequired = false)))

        // Assert
        val content = result as MoneyAccountMainUM.Content
        assertThat(content.subtitle).isEqualTo(resourceReference(R.string.money_account_main_status_active_v2))
        assertThat(content.isActionRequired).isFalse()
    }

    @Test
    fun `GIVEN active account WHEN content clicked THEN money account opened`() {
        // Arrange
        val content = converter.convert(moneyAccount(activeValue())) as MoneyAccountMainUM.Content

        // Act
        content.onClick()

        // Assert
        verify(exactly = 1) { moneyAccountIntents.onMoneyAccountClicked(USER_WALLET_ID) }
    }

    data class ActivatingModel(
        val step: MoneyAccountStatusValue.Onboarding.Activating.Step,
        val expectedSubtitleRes: Int,
    )

    private fun provideTestModels() = listOf(
        ActivatingModel(
            step = MoneyAccountStatusValue.Onboarding.Activating.Step.ISSUING,
            expectedSubtitleRes = R.string.money_account_main_status_activating,
        ),
        ActivatingModel(
            step = MoneyAccountStatusValue.Onboarding.Activating.Step.AWAITING_DEPOSIT,
            expectedSubtitleRes = R.string.money_account_main_status_awaiting_deposit,
        ),
    )

    private fun moneyAccount(value: MoneyAccountStatusValue): AccountStatus.Money {
        return AccountStatus.Money(account = Account.Money(USER_WALLET_ID), value = value)
    }

    private fun activeValue(isKycAdditionalInfoRequired: Boolean = false): MoneyAccountStatusValue.Account.Active {
        return MoneyAccountStatusValue.Account.Active(
            source = StatusSource.ACTUAL,
            cryptoCurrency = mockk(relaxed = true),
            balance = MoneyAccountStatusValue.Balance(
                cryptoAmount = BigDecimal.TEN,
                depositAddress = "0xdeposit",
                currencyCode = "USD",
                fiatRate = BigDecimal.ONE,
            ),
            customerId = "customer",
            productInstanceId = "instance",
            error = null,
            isBankOfframpAvailable = null,
            isKycAdditionalInfoRequired = isKycAdditionalInfoRequired,
        )
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("0011")
    }
}