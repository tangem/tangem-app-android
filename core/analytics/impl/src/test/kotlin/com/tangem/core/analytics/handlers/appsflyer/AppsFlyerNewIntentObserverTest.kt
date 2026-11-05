package com.tangem.core.analytics.handlers.appsflyer

import android.app.Activity
import android.content.Intent
import androidx.core.app.OnNewIntentProvider
import androidx.core.util.Consumer
import com.appsflyer.AppsFlyerLib
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AppsFlyerNewIntentObserverTest {

    private val appsFlyerLib: AppsFlyerLib = mockk(relaxUnitFun = true)
    private val activity: Activity = mockk(relaxUnitFun = true, moreInterfaces = arrayOf(OnNewIntentProvider::class))
    private val newIntentProvider = activity as OnNewIntentProvider
    private val intent: Intent = mockk()

    private val observer = AppsFlyerNewIntentObserver()

    @BeforeAll
    fun mockAppsFlyer() {
        mockkStatic(AppsFlyerLib::class)
        every { AppsFlyerLib.getInstance() } returns appsFlyerLib
    }

    @AfterAll
    fun unmockAppsFlyer() {
        unmockkStatic(AppsFlyerLib::class)
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(appsFlyerLib, activity)
    }

    @Test
    fun `GIVEN activity providing new intents WHEN it receives a new intent THEN SDK resolves the deep link`() {
        // Arrange
        val listener = slot<Consumer<Intent>>()
        every { newIntentProvider.addOnNewIntentListener(capture(listener)) } returns Unit
        observer.onActivityCreated(activity, null)

        // Act
        listener.captured.accept(intent)

        // Assert
        verify(exactly = 1) { appsFlyerLib.performOnDeepLinking(intent, activity) }
    }

    @Test
    fun `GIVEN activity providing new intents WHEN created THEN no deep link is resolved before a new intent`() {
        // Act
        observer.onActivityCreated(activity, null)

        // Assert
        verify(exactly = 1) { newIntentProvider.addOnNewIntentListener(any()) }
        verify(exactly = 0) { appsFlyerLib.performOnDeepLinking(any(), any()) }
    }

    @Test
    fun `GIVEN activity without new intents WHEN created THEN nothing is subscribed`() {
        // Arrange
        val plainActivity: Activity = mockk()

        // Act
        observer.onActivityCreated(plainActivity, null)

        // Assert
        verify(exactly = 0) { appsFlyerLib.performOnDeepLinking(any(), any()) }
    }
}