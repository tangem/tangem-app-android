package com.tangem.features.pushnotifications.api.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsParam
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class PushNotificationAnalyticEventsTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PermissionStatus {

        @ParameterizedTest
        @ValueSource(booleans = [true, false])
        fun `GIVEN joint account source WHEN event created THEN id and params match the spec`(isAllowed: Boolean) {
            // Act
            val event = PushNotificationAnalyticEvents.PermissionStatus(
                isAllowed = isAllowed,
                source = AnalyticsParam.ScreensSources.JointAccount,
            )

            // Assert
            assertThat(event.id).isEqualTo("[Push] Permission Status")
            assertThat(event.params).isEqualTo(
                mapOf(
                    "State" to if (isAllowed) "Allow" else "Cancel",
                    "Source" to "Joint Account",
                ),
            )
        }
    }
}