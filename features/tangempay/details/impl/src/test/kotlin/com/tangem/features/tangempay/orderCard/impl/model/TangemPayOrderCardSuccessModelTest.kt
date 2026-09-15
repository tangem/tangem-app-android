package com.tangem.features.tangempay.orderCard.impl.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.orderCard.api.TangemPayOrderCardIntent
import com.tangem.features.tangempay.orderCard.impl.TangemPayOrderCardSuccessComponent
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class TangemPayOrderCardSuccessModelTest {

    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)
    private val onFinish: () -> Unit = mockk(relaxed = true)

    @Test
    fun `GIVEN the issue intent WHEN model created THEN the button shows the card`() = runTest {
        // Act
        val model = createModel(intent = TangemPayOrderCardIntent.Issue)

        // Assert
        assertThat(model.state.value.buttonText)
            .isEqualTo(resourceReference(R.string.tangempay_order_success_show_card))
    }

    @Test
    fun `GIVEN the reissue intent WHEN model created THEN the button closes the flow`() = runTest {
        // Act
        val model = createModel(intent = reissueIntent())

        // Assert
        assertThat(model.state.value.buttonText).isEqualTo(resourceReference(R.string.common_close))
    }

    private fun reissueIntent() = TangemPayOrderCardIntent.ReissuePlastic(
        sourceProductInstanceId = "pi_1",
        sourceCardId = "card_1",
        deliveryEtaMaxBusinessDays = ETA_DAYS,
    )

    private fun TestScope.createModel(intent: TangemPayOrderCardIntent) = TangemPayOrderCardSuccessModel(
        paramsContainer = MutableParamsContainer(
            TangemPayOrderCardSuccessComponent.Params(
                deliveryEtaMaxBusinessDays = ETA_DAYS,
                email = "panampalmer@gmail.com",
                intent = intent,
                onFinish = onFinish,
            ),
        ),
        dispatchers = createTestingCoroutineDispatcherProvider(),
        analytics = analytics,
    )

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }

    private companion object {
        const val ETA_DAYS = 20
    }
}