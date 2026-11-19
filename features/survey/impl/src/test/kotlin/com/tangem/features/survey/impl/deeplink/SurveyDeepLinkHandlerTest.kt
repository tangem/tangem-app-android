package com.tangem.features.survey.impl.deeplink

import android.net.Uri
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.deeplink.DeepLinkRequest
import com.tangem.utils.logging.TangemLogger
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SurveyDeepLinkHandlerTest {

    private val router = mockk<Router>(relaxed = true)

    @BeforeEach
    fun setup() {
        mockkObject(TangemLogger)
        every { TangemLogger.i(any()) } just Runs
        every { TangemLogger.e(any()) } just Runs
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(TangemLogger)
    }

    @Test
    fun `does not navigate when token is missing`() {
        handle(emptyMap())

        verify(exactly = 0) { router.push(any(), any()) }
    }

    @Test
    fun `pushes survey route with token and display id on happy path`() {
        handle(mapOf("token" to TOKEN, "display_id" to DISPLAY_ID))

        verify { router.push(route = AppRoute.Survey(token = TOKEN, displayId = DISPLAY_ID), onComplete = any()) }
    }

    @Test
    fun `pushes survey route with null display id when absent`() {
        handle(mapOf("token" to TOKEN))

        verify { router.push(route = AppRoute.Survey(token = TOKEN, displayId = null), onComplete = any()) }
    }

    private fun handle(queryParams: Map<String, String>) = SurveyDeepLinkHandler()
        .handle(
            DeepLinkRequest(
                uri = mockk<Uri>(),
                queryParams = queryParams,
                scope = mockk<CoroutineScope>(),
                isFromOnNewIntent = false,
                router = router,
            ),
        )

    private companion object {
        const val TOKEN = "ntt-84iF22PDajmervYneMW4kv"
        const val DISPLAY_ID = "42"
    }
}