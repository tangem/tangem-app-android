package com.tangem.features.survey.impl.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import com.tangem.utils.logging.TangemLogger
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultSurveyDeepLinkHandlerTest {

    private val appRouter = mockk<AppRouter>(relaxed = true)

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
        createHandler(emptyMap())

        verify(exactly = 0) { appRouter.push(any(), any()) }
    }

    @Test
    fun `pushes survey route with token and display id on happy path`() {
        createHandler(mapOf("token" to TOKEN, "display_id" to DISPLAY_ID))

        verify { appRouter.push(route = AppRoute.Survey(token = TOKEN, displayId = DISPLAY_ID), onComplete = any()) }
    }

    @Test
    fun `pushes survey route with null display id when absent`() {
        createHandler(mapOf("token" to TOKEN))

        verify { appRouter.push(route = AppRoute.Survey(token = TOKEN, displayId = null), onComplete = any()) }
    }

    private fun createHandler(queryParams: Map<String, String>) = DefaultSurveyDeepLinkHandler(
        queryParams = queryParams,
        appRouter = appRouter,
    )

    private companion object {
        const val TOKEN = "ntt-84iF22PDajmervYneMW4kv"
        const val DISPLAY_ID = "42"
    }
}