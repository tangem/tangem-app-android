package com.tangem.features.pushnotifications.impl.domain

import com.google.common.truth.Truth.assertThat
import com.tangem.core.abtests.manager.ABTestsManager
import com.tangem.features.pushnotifications.PushNotificationsFeatureToggles
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class GetPushNotificationsDoubleAskVariantUseCaseTest {

    private val featureToggles: PushNotificationsFeatureToggles = mockk()
    private val abTestsManager: ABTestsManager = mockk()

    private val useCase = GetPushNotificationsDoubleAskVariantUseCase(
        pushNotificationsFeatureToggles = featureToggles,
        abTestsManager = abTestsManager,
    )

    @Test
    fun `GIVEN toggle disabled WHEN invoke THEN returns Off and AB not queried`() = runTest {
        every { featureToggles.isOnboardingPushDoubleAskAbEnabled } returns false

        val result = useCase()

        assertThat(result).isEqualTo(DoubleAskVariant.Off)
        coVerify(exactly = 0) { abTestsManager.getValue(any(), any()) }
    }

    @Test
    fun `GIVEN toggle enabled AND AB returns treatment WHEN invoke THEN returns On`() = runTest {
        every { featureToggles.isOnboardingPushDoubleAskAbEnabled } returns true
        coEvery { abTestsManager.getValue(KEY, "control") } returns "treatment"

        val result = useCase()

        assertThat(result).isEqualTo(DoubleAskVariant.On)
        coVerify(exactly = 1) { abTestsManager.getValue(KEY, "control") }
    }

    @Test
    fun `GIVEN toggle enabled AND AB returns control WHEN invoke THEN returns Off`() = runTest {
        every { featureToggles.isOnboardingPushDoubleAskAbEnabled } returns true
        coEvery { abTestsManager.getValue(KEY, "control") } returns "control"

        assertThat(useCase()).isEqualTo(DoubleAskVariant.Off)
    }

    @Test
    fun `GIVEN toggle enabled AND AB returns unknown WHEN invoke THEN returns Off`() = runTest {
        every { featureToggles.isOnboardingPushDoubleAskAbEnabled } returns true
        coEvery { abTestsManager.getValue(KEY, "control") } returns "unexpected_value"

        assertThat(useCase()).isEqualTo(DoubleAskVariant.Off)
    }

    private companion object {
        const val KEY = "twi_1403_onboarding_push_notification_double_ask"
    }
}