package com.tangem.spend.pushProvisioning.impl

import android.app.Activity
import android.content.Intent
import com.google.common.truth.Truth.assertThat
import com.meawallet.mpp.MppErrorCode
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Job
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class DefaultPushProvisioningActivityBridgeTest {

    private val sdk: MeaWalletGooglePay = mockk(relaxUnitFun = true)
    private val activity: Activity = mockk()
    private lateinit var bridge: DefaultPushProvisioningActivityBridge

    @BeforeEach
    fun setUp() {
        clearMocks(sdk)
        bridge = DefaultPushProvisioningActivityBridge(sdk)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Forwarding {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN SDK flow awaits a result WHEN onActivityResult THEN SDK receives the result as is`(
            model: ActivityResultModel,
        ) {
            // Arrange
            bridge.attach(activity)
            bridge.forwardResultsDuring(Job())

            // Act
            bridge.onActivityResult(requestCode = REQUEST_CODE, resultCode = model.resultCode, data = model.data)

            // Assert
            verify(exactly = 1) { sdk.handleOnActivityResult(REQUEST_CODE, model.resultCode, model.data, activity) }
        }

        private fun provideTestModels() = listOf(
            ActivityResultModel(resultCode = Activity.RESULT_OK, data = mockk()),
            ActivityResultModel(resultCode = Activity.RESULT_CANCELED, data = null),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class NotForwarding {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN no awaiting SDK flow or no activity WHEN onActivityResult THEN result is not forwarded`(
            model: BridgeStateModel,
        ) {
            // Arrange
            if (model.isActivityAttached) bridge.attach(activity)
            if (model.isFlowAwaiting) bridge.forwardResultsDuring(Job())

            // Act
            bridge.onActivityResult(requestCode = REQUEST_CODE, resultCode = Activity.RESULT_OK, data = null)

            // Assert
            verify(exactly = 0) { sdk.handleOnActivityResult(any(), any(), any(), any()) }
        }

        private fun provideTestModels() = listOf(
            BridgeStateModel(isActivityAttached = true, isFlowAwaiting = false),
            BridgeStateModel(isActivityAttached = false, isFlowAwaiting = true),
            BridgeStateModel(isActivityAttached = false, isFlowAwaiting = false),
        )

        @Test
        fun `GIVEN awaited SDK flow completed WHEN onActivityResult THEN result is not forwarded`() {
            // Arrange
            val flow = Job()
            bridge.attach(activity)
            bridge.forwardResultsDuring(flow)
            flow.complete()

            // Act
            bridge.onActivityResult(requestCode = REQUEST_CODE, resultCode = Activity.RESULT_OK, data = null)

            // Assert
            verify(exactly = 0) { sdk.handleOnActivityResult(any(), any(), any(), any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ForwardResultsDuring {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN earlier SDK flow WHEN another flow starts awaiting THEN it is accepted once the earlier one ends`(
            model: EarlierFlowModel,
        ) {
            // Arrange
            val earlierFlow = Job()
            bridge.forwardResultsDuring(earlierFlow)
            if (!model.isEarlierFlowActive) earlierFlow.complete()

            // Act
            val actual = bridge.forwardResultsDuring(Job())

            // Assert
            assertThat(actual).isEqualTo(model.isAccepted)
        }

        private fun provideTestModels() = listOf(
            EarlierFlowModel(isEarlierFlowActive = true, isAccepted = false),
            EarlierFlowModel(isEarlierFlowActive = false, isAccepted = true),
        )
    }

    @Test
    fun `GIVEN attached activity WHEN another activity detached THEN attached activity kept`() {
        // Arrange
        bridge.attach(activity)

        // Act
        bridge.detach(mockk())

        // Assert
        assertThat(bridge.activity).isSameInstanceAs(activity)
    }

    @Test
    fun `GIVEN attached activity WHEN it is detached THEN no activity kept`() {
        // Arrange
        bridge.attach(activity)

        // Act
        bridge.detach(activity)

        // Assert
        assertThat(bridge.activity).isNull()
    }

    @Test
    fun `GIVEN SDK throws WHEN onActivityResult THEN failure is not propagated`() {
        // Arrange
        bridge.attach(activity)
        bridge.forwardResultsDuring(Job())
        every { sdk.handleOnActivityResult(any(), any(), any(), any()) } throws
            createMppException(FakeMppError(errorCode = MppErrorCode.INTERNAL_ERROR))

        // Act
        val actual = runCatching {
            bridge.onActivityResult(requestCode = REQUEST_CODE, resultCode = Activity.RESULT_OK, data = null)
        }

        // Assert
        assertThat(actual.exceptionOrNull()).isNull()
        verify(exactly = 1) { sdk.handleOnActivityResult(REQUEST_CODE, Activity.RESULT_OK, null, activity) }
    }

    internal data class ActivityResultModel(val resultCode: Int, val data: Intent?)

    internal data class BridgeStateModel(val isActivityAttached: Boolean, val isFlowAwaiting: Boolean)

    internal data class EarlierFlowModel(val isEarlierFlowActive: Boolean, val isAccepted: Boolean)

    private companion object {
        const val REQUEST_CODE = 1001
    }
}