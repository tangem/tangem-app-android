package com.tangem.features.tangempay.account

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.account.TangemPayTariffPlanState
import com.tangem.features.tangempay.awaitingDepositOrder
import com.tangem.features.tangempay.customerTariffPlan
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.tariffPlan
import com.tangem.features.tangempay.tariffPlanState
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayDetailsNotificationFactoryTest {

    private val intents: TangemPayDetailIntents = mockk(relaxed = true)

    private val factory = TangemPayDetailsNotificationFactory(intents = intents)

    @BeforeEach
    fun resetMocks() {
        clearMocks(intents)
    }

    @Test
    fun `GIVEN awaiting deposit order WHEN createTiersConfig THEN banner offers add funds instead of cancelling`() {
        // Arrange
        val planState = tariffPlanState(order = awaitingDepositOrder())

        // Act
        val config = factory.createTiersConfig(planState)

        // Assert
        val button = config?.secondaryButton
        assertThat(button?.text).isEqualTo(resourceReference(R.string.tangempay_card_details_add_funds))
        button?.onClick?.invoke()
        verify(exactly = 1) { intents.onClickAddFunds() }
        verify(exactly = 0) { intents.onRenewSession() }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN no pending plan with a recurring fee WHEN createTiersConfig THEN no banner`(case: NoBannerModel) {
        // Act
        val config = factory.createTiersConfig(case.planState)

        // Assert
        assertThat(config).isNull()
    }

    private fun provideTestModels() = listOf(
        NoBannerModel(
            name = "awaiting deposit for a plan without recurring fee",
            planState = tariffPlanState(
                order = awaitingDepositOrder(toPlan = tariffPlan(tierId = "PLUS", fees = emptyList())),
            ),
        ),
        NoBannerModel(
            name = "active plan without order",
            planState = tariffPlanState(tariff = customerTariffPlan()),
        ),
    )

    internal data class NoBannerModel(val name: String, val planState: TangemPayTariffPlanState) {
        override fun toString(): String = name
    }
}