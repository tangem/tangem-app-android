package com.tangem.features.onramp.main.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.features.marketing.api.MoneyAccountBannerAction
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MoneyAccountBannerActionsTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN money account status WHEN mapped THEN button action follows whether the user has an account`(
        model: StatusModel,
    ) {
        // Act
        val actual = model.status.toBannerAction()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class StatusModel(val status: MoneyAccountStatusValue, val expected: MoneyAccountBannerAction?)

    private fun provideTestModels() = listOf(
        StatusModel(status = mockk<MoneyAccountStatusValue.Account.Active>(), expected = MoneyAccountBannerAction.CONTINUE),
        StatusModel(status = mockk<MoneyAccountStatusValue.Error.NotSynced>(), expected = null),
        StatusModel(status = MoneyAccountStatusValue.Empty, expected = MoneyAccountBannerAction.ACTIVATE),
        StatusModel(
            status = MoneyAccountStatusValue.Onboarding.Failed(source = StatusSource.ACTUAL, customerId = "id"),
            expected = MoneyAccountBannerAction.ACTIVATE,
        ),
        StatusModel(
            status = MoneyAccountStatusValue.Onboarding.Activating(
                source = StatusSource.ACTUAL,
                customerId = "id",
                step = MoneyAccountStatusValue.Onboarding.Activating.Step.ISSUING,
            ),
            expected = MoneyAccountBannerAction.ACTIVATE,
        ),
        StatusModel(status = MoneyAccountStatusValue.Error.Unavailable, expected = null),
        StatusModel(status = MoneyAccountStatusValue.Error.NotSynced, expected = null),
        StatusModel(status = MoneyAccountStatusValue.Error.ExposedDevice, expected = null),
    )
}