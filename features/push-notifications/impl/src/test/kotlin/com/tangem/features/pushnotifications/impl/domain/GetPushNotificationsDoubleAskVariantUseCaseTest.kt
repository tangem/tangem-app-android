package com.tangem.features.pushnotifications.impl.domain

import com.google.common.truth.Truth.assertThat
import com.tangem.core.abtests.manager.ABTestsManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class GetPushNotificationsDoubleAskVariantUseCaseTest {

    private val abTestsManager: ABTestsManager = mockk()

    private val useCase = GetPushNotificationsDoubleAskVariantUseCase(
        abTestsManager = abTestsManager,
    )

    @Test
    fun `GIVEN AB returns treatment WHEN invoke THEN returns On`() = runTest {
        coEvery { abTestsManager.getValue(KEY, "control") } returns "treatment"

        val result = useCase()

        assertThat(result).isEqualTo(DoubleAskVariant.On)
        coVerify(exactly = 1) { abTestsManager.getValue(KEY, "control") }
    }

    @Test
    fun `GIVEN AB returns control WHEN invoke THEN returns Off`() = runTest {
        coEvery { abTestsManager.getValue(KEY, "control") } returns "control"

        assertThat(useCase()).isEqualTo(DoubleAskVariant.Off)
    }

    @Test
    fun `GIVEN AB returns unknown WHEN invoke THEN returns Off`() = runTest {
        coEvery { abTestsManager.getValue(KEY, "control") } returns "unexpected_value"

        assertThat(useCase()).isEqualTo(DoubleAskVariant.Off)
    }

    private companion object {
        const val KEY = "twi_1403_onboarding_push_notification_double_ask"
    }
}