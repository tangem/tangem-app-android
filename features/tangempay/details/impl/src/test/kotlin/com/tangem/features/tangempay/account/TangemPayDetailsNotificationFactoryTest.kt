package com.tangem.features.tangempay.account

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.tangempay.awaitingDepositOrder
import com.tangem.features.tangempay.customerTariffPlan
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.tariffPlan
import com.tangem.features.tangempay.tariffPlanState
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TangemPayDetailsNotificationFactoryTest {

    private val intents: TangemPayDetailIntents = mockk(relaxed = true)

    private val factory = TangemPayDetailsNotificationFactory(
        intents = intents,
        isTiersPlusPlanEnabled = true,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(intents)
    }

    @Test
    fun `GIVEN awaiting deposit order WHEN createTiersConfig THEN banner offers add funds instead of cancelling`() {
        // GIVEN
        val planState = tariffPlanState(order = awaitingDepositOrder())

        // WHEN
        val config = factory.createTiersConfig(planState)

        // THEN
        val button = config?.buttonsState as? NotificationConfig.ButtonsState.SecondaryButtonConfig
        assertThat(button?.text).isEqualTo(resourceReference(R.string.tangempay_card_details_add_funds))
        button?.onClick?.invoke()
        verify(exactly = 1) { intents.onClickAddFunds() }
        verify(exactly = 0) { intents.onRenewSession() }
    }

    @Test
    fun `GIVEN plan without recurring fee WHEN createTiersConfig THEN no banner`() {
        // GIVEN
        val planState = tariffPlanState(
            order = awaitingDepositOrder(toPlan = tariffPlan(tierId = "PLUS", fees = emptyList())),
        )

        // WHEN
        val config = factory.createTiersConfig(planState)

        // THEN
        assertThat(config).isNull()
    }

    @Test
    fun `GIVEN tiers toggle disabled WHEN createTiersConfig THEN no banner`() {
        // GIVEN
        val disabledFactory = TangemPayDetailsNotificationFactory(
            intents = intents,
            isTiersPlusPlanEnabled = false,
        )

        // WHEN
        val config = disabledFactory.createTiersConfig(tariffPlanState(order = awaitingDepositOrder()))

        // THEN
        assertThat(config).isNull()
    }

    @Test
    fun `GIVEN active plan without order WHEN createTiersConfig THEN no banner`() {
        // WHEN
        val config = factory.createTiersConfig(tariffPlanState(tariff = customerTariffPlan()))

        // THEN
        assertThat(config).isNull()
    }
}