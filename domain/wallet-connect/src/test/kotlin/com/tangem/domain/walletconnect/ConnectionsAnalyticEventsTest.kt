package com.tangem.domain.walletconnect

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AppsFlyerIncludedEvent
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ConnectionsAnalyticEventsTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN connections event WHEN created THEN id and params match the spec`(model: ContractModel) {
        // Assert
        assertThat(model.event.id to model.event.params).isEqualTo(model.expectedId to emptyMap<String, String>())
    }

    @Test
    fun `GIVEN screen opened WHEN created THEN it is sent to AppsFlyer like the WC one`() {
        // Assert
        assertThat(ConnectionsAnalyticEvents.ScreenOpened()).isInstanceOf(AppsFlyerIncludedEvent::class.java)
    }

    private fun provideTestModels() = listOf(
        ContractModel(
            event = ConnectionsAnalyticEvents.ScreenOpened(),
            expectedId = "[Connections] Connections Screen Opened",
        ),
        ContractModel(
            event = ConnectionsAnalyticEvents.ButtonDisconnectAll(),
            expectedId = "[Connections] Button - Disconnect All",
        ),
    )

    data class ContractModel(val event: AnalyticsEvent, val expectedId: String) {
        override fun toString(): String = expectedId
    }
}