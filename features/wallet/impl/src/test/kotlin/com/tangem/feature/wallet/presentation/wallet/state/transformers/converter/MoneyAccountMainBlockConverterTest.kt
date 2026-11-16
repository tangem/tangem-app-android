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
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

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
}