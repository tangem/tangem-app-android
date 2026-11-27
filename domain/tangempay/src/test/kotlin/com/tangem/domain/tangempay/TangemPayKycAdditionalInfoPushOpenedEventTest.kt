package com.tangem.domain.tangempay

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class TangemPayKycAdditionalInfoPushOpenedEventTest {

    @Test
    fun `GIVEN kyc additional info push opened event WHEN built THEN id and params match the spec`() {
        // Act
        val event = TangemPayKycAdditionalInfoPushOpenedEvent()

        // Assert
        assertThat(event.id).isEqualTo("[Push] Push Notification Opened")
        assertThat(event.params).isEqualTo(mapOf("verification_context" to "additional_info"))
    }
}