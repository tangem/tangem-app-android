package com.tangem.core.analytics.handlers.firebase

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.common.truth.Truth.assertThat
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FirebaseAppInstanceIdProviderTest {

    private val firebaseAnalytics: FirebaseAnalytics = mockk()
    private val appInstanceIdTask: Task<String?> = mockk()

    private val provider = FirebaseAppInstanceIdProvider()

    @BeforeAll
    fun mockFirebase() {
        mockkStatic(FIREBASE_ANALYTICS_KTX)
        every { Firebase.analytics } returns firebaseAnalytics
    }

    @AfterAll
    fun unmockFirebase() {
        unmockkStatic(FIREBASE_ANALYTICS_KTX)
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(firebaseAnalytics, appInstanceIdTask)
        every { firebaseAnalytics.appInstanceId } returns appInstanceIdTask
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetAppInstanceId {

        @Test
        fun `GIVEN task succeeds WHEN getAppInstanceId THEN returns the id`() = runTest {
            // Arrange
            every { appInstanceIdTask.addOnSuccessListener(any<OnSuccessListener<String?>>()) } answers {
                firstArg<OnSuccessListener<String?>>().onSuccess(APP_INSTANCE_ID)
                appInstanceIdTask
            }
            every { appInstanceIdTask.addOnFailureListener(any<OnFailureListener>()) } returns appInstanceIdTask

            // Act
            val actual = provider.getAppInstanceId()

            // Assert
            assertThat(actual).isEqualTo(APP_INSTANCE_ID)
        }

        @Test
        fun `GIVEN task fails WHEN getAppInstanceId THEN returns null`() = runTest {
            // Arrange
            every { appInstanceIdTask.addOnSuccessListener(any<OnSuccessListener<String?>>()) } returns appInstanceIdTask
            every { appInstanceIdTask.addOnFailureListener(any<OnFailureListener>()) } answers {
                firstArg<OnFailureListener>().onFailure(IllegalStateException())
                appInstanceIdTask
            }

            // Act
            val actual = provider.getAppInstanceId()

            // Assert
            assertThat(actual).isNull()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetAppInstanceIdSync {

        @Test
        fun `GIVEN task completed WHEN getAppInstanceIdSync THEN returns the id`() {
            // Arrange
            every { appInstanceIdTask.result } returns APP_INSTANCE_ID

            // Act
            val actual = provider.getAppInstanceIdSync()

            // Assert
            assertThat(actual).isEqualTo(APP_INSTANCE_ID)
        }

        @Test
        fun `GIVEN task not completed WHEN getAppInstanceIdSync THEN returns null`() {
            // Arrange
            every { appInstanceIdTask.result } throws IllegalStateException("Task is not yet complete")

            // Act
            val actual = provider.getAppInstanceIdSync()

            // Assert
            assertThat(actual).isNull()
        }
    }

    private companion object {
        const val FIREBASE_ANALYTICS_KTX = "com.google.firebase.analytics.ktx.AnalyticsKt"
        const val APP_INSTANCE_ID = "app_instance_id"
    }
}