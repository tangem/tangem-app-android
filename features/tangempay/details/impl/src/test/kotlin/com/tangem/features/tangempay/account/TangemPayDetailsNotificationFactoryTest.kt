package com.tangem.features.tangempay.account

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.account.PaymentAccountStatusValue
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
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayDetailsNotificationFactoryTest {

    private val intents: TangemPayDetailIntents = mockk(relaxed = true)

    private val factory = TangemPayDetailsNotificationFactory(intents = intents, isColdWallet = true)

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
    @ValueSource(booleans = [true, false])
    fun `GIVEN not synced error WHEN createErrorConfig THEN tangem icon shown only for cold wallet`(
        isColdWallet: Boolean,
    ) {
        // Arrange
        val factory = TangemPayDetailsNotificationFactory(intents = intents, isColdWallet = isColdWallet)

        // Act
        val config = factory.createErrorConfig(PaymentAccountStatusValue.Error.NotSynced)

        // Assert
        val icon = requireNotNull(config?.secondaryButton).iconEnd
        if (isColdWallet) {
            assertThat(icon).isInstanceOf(TangemIconUM.Icon::class.java)
        } else {
            assertThat(icon).isNull()
        }
    }

    @Test
    fun `GIVEN factory WHEN createKycAdditionalInfoBannerState THEN warning banner whose button opens kyc`() {
        // Act
        val state = factory.createKycAdditionalInfoBannerState()

        // Assert
        assertThat(state.copy(secondaryButton = null)).isEqualTo(
            TangemMessageBanner.State(
                title = resourceReference(R.string.tangempay_kyc_additional_info_title),
                variant = TangemMessageBanner.Variant.Warning,
                description = resourceReference(R.string.tangempay_kyc_additional_info_banner_description),
            ),
        )
        val button = requireNotNull(state.secondaryButton)
        assertThat(button.text).isEqualTo(resourceReference(R.string.tangempay_kyc_additional_info_button))
        button.onClick()
        verify(exactly = 1) { intents.onProvideKycInformationClick() }
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