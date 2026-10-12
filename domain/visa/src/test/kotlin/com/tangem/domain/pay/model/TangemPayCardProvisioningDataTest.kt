package com.tangem.domain.pay.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class TangemPayCardProvisioningDataTest {

    @Test
    fun `GIVEN provisioning data WHEN toString THEN card secret is not exposed`() {
        // Arrange
        val data = TangemPayCardProvisioningData(processorCardId = "processor-card-id", cardSecret = "time-based-secret")

        // Act
        val actual = data.toString()

        // Assert
        assertThat(actual).doesNotContain("time-based-secret")
    }
}