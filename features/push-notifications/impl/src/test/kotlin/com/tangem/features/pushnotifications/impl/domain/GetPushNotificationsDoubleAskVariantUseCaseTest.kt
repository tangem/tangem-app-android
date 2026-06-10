package com.tangem.features.pushnotifications.impl.domain

import com.google.common.truth.Truth.assertThat
import com.tangem.core.abtests.manager.ABTestsManager
import com.tangem.features.pushnotifications.PushNotificationsFeatureToggles
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

internal class GetPushNotificationsDoubleAskVariantUseCaseTest {

    private val featureToggles: PushNotificationsFeatureToggles = mockk()
    private val abTestsManager: ABTestsManager = mockk()

    private val useCase = GetPushNotificationsDoubleAskVariantUseCase(
        pushNotificationsFeatureToggles = featureToggles,
        abTestsManager = abTestsManager,
    )

    @Test
    fun `GIVEN toggle disabled WHEN invoke THEN returns Off and AB not queried`() {
        every { featureToggles.isOnboardingPushDoubleAskAbEnabled } returns false

        val result = useCase()

        assertThat(result).isEqualTo(DoubleAskVariant.Off)
        verify(exactly = 0) { abTestsManager.getValue(any(), any()) }
    }

    @Test
    fun `GIVEN toggle enabled AND AB returns treatment WHEN invoke THEN returns On`() {
        every { featureToggles.isOnboardingPushDoubleAskAbEnabled } returns true
        every { abTestsManager.getValue(KEY, "control") } returns "treatment"

        val result = useCase()

        assertThat(result).isEqualTo(DoubleAskVariant.On)
        verify(exactly = 1) { abTestsManager.getValue(KEY, "control") }
    }

    @Test
    fun `GIVEN toggle enabled AND AB returns control WHEN invoke THEN returns Off`() {
        every { featureToggles.isOnboardingPushDoubleAskAbEnabled } returns true
        every { abTestsManager.getValue(KEY, "control") } returns "control"

        assertThat(useCase()).isEqualTo(DoubleAskVariant.Off)
    }

    @Test
    fun `GIVEN toggle enabled AND AB returns unknown WHEN invoke THEN returns Off`() {
        every { featureToggles.isOnboardingPushDoubleAskAbEnabled } returns true
        every { abTestsManager.getValue(KEY, "control") } returns "unexpected_value"

        assertThat(useCase()).isEqualTo(DoubleAskVariant.Off)
    }

    private companion object {
        const val KEY = "twi_1403_onboarding_push_notification_double_ask"
    }
}