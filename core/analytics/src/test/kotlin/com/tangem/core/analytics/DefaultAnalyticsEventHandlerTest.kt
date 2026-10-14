package com.tangem.core.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsErrorHandler
import com.tangem.core.analytics.api.AnalyticsEventFilter
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.core.analytics.api.AnalyticsUserIdHandler
import com.tangem.core.analytics.api.ParamsInterceptor
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import com.tangem.core.analytics.models.OneTimePerSessionEvent
import dagger.Lazy
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.security.MessageDigest

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultAnalyticsEventHandlerTest {

    private val plainHandler: AnalyticsHandler = mockk(relaxed = true)
    private val userIdHandler: UserIdTestHandler = mockk(relaxed = true)
    private val errorHandler: ErrorTestHandler = mockk(relaxed = true)
    private val exceptionHandler: ExceptionTestHandler = mockk(relaxed = true)
    private val allHandlers = setOf(plainHandler, userIdHandler, errorHandler, exceptionHandler)

    @BeforeEach
    fun resetMocks() {
        clearMocks(plainHandler, userIdHandler, errorHandler, exceptionHandler)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Send {

        @Test
        fun `GIVEN no applicable filter WHEN send THEN every handler receives event`() = runTest {
            // Arrange
            val handler = createHandler(filters = listOf(createFilter(isApplicable = false)))
            val event = AnalyticsEvent(category = "Test", event = "Event")

            // Act
            handler.send(event)
            advanceUntilIdle()

            // Assert
            allHandlers.forEach { verify(exactly = 1) { it.send(event) } }
        }

        @Test
        fun `GIVEN applicable filter rejects event WHEN send THEN no handler receives it`() = runTest {
            // Arrange
            val handler = createHandler(filters = listOf(createFilter(isApplicable = true, canBeSent = false)))

            // Act
            handler.send(AnalyticsEvent(category = "Test", event = "Event"))
            advanceUntilIdle()

            // Assert
            allHandlers.forEach { verify(exactly = 0) { it.send(any()) } }
        }

        @Test
        fun `GIVEN filter accepts one handler WHEN send THEN only that handler receives event`() = runTest {
            // Arrange
            val filter = createFilter(isApplicable = true, canBeSent = true, consumer = userIdHandler)
            val handler = createHandler(filters = listOf(filter))
            val event = AnalyticsEvent(category = "Test", event = "Event")

            // Act
            handler.send(event)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { userIdHandler.send(event) }
            listOf(plainHandler, errorHandler, exceptionHandler).forEach { verify(exactly = 0) { it.send(any()) } }
        }

        @Test
        fun `GIVEN two applicable filters WHEN send THEN first one decides`() = runTest {
            // Arrange
            val rejectingFilter = createFilter(isApplicable = true, canBeSent = false)
            val acceptingFilter = createFilter(isApplicable = true, canBeSent = true, consumer = plainHandler)
            val handler = createHandler(filters = listOf(rejectingFilter, acceptingFilter))

            // Act
            handler.send(AnalyticsEvent(category = "Test", event = "Event"))
            advanceUntilIdle()

            // Assert
            allHandlers.forEach { verify(exactly = 0) { it.send(any()) } }
        }

        @Test
        fun `GIVEN one-time-per-session event WHEN sent twice THEN handlers receive it once`() = runTest {
            // Arrange
            val handler = createHandler()

            // Act
            handler.send(OneTimePerSessionTestEvent())
            handler.send(OneTimePerSessionTestEvent())
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { plainHandler.send(any()) }
        }

        @Test
        fun `GIVEN static and added interceptors WHEN send THEN applicable ones enrich params`() = runTest {
            // Arrange
            val handler = createHandler(
                staticInterceptors = setOf(createInterceptor(id = "static", key = "static", isApplicable = true)),
            )
            handler.addParamsInterceptor(createInterceptor(id = "added", key = "added", isApplicable = true))
            handler.addParamsInterceptor(createInterceptor(id = "skipped", key = "skipped", isApplicable = false))
            val sentEvent = slot<AnalyticsEvent>()
            every { plainHandler.send(capture(sentEvent)) } returns Unit

            // Act
            handler.send(AnalyticsEvent(category = "Test", event = "Event", params = mapOf("own" to "value")))
            advanceUntilIdle()

            // Assert
            assertThat(sentEvent.captured.params).containsExactly("own", "value", "static", "static", "added", "added")
        }

        @Test
        fun `GIVEN removed interceptor WHEN send THEN it is not applied`() = runTest {
            // Arrange
            val handler = createHandler()
            handler.addParamsInterceptor(createInterceptor(id = "context", key = "context", isApplicable = true))
            val sentEvent = slot<AnalyticsEvent>()
            every { plainHandler.send(capture(sentEvent)) } returns Unit

            // Act
            handler.removeParamsInterceptor("context")
            handler.send(AnalyticsEvent(category = "Test", event = "Event"))
            advanceUntilIdle()

            // Assert
            assertThat(sentEvent.captured.params).isEmpty()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class HandlersResolution {

        @Test
        fun `GIVEN nothing sent WHEN created THEN handlers are not resolved`() = runTest {
            // Arrange
            var resolutions = 0
            val lazyHandlers = Lazy {
                resolutions++
                allHandlers
            }

            // Act
            createHandler(handlers = lazyHandlers)
            advanceUntilIdle()

            // Assert
            assertThat(resolutions).isEqualTo(0)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SendErrorEvent {

        @Test
        fun `GIVEN error event WHEN sendErrorEvent THEN only error handlers receive it`() = runTest {
            // Arrange
            val handler = createHandler()
            val event = AnalyticsEvent(category = "Test", event = "Error")

            // Act
            handler.sendErrorEvent(event)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { errorHandler.sendErrorEvent(event) }
            allHandlers.forEach { verify(exactly = 0) { it.send(any()) } }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SendException {

        @Test
        fun `GIVEN exception event WHEN sendException THEN only exception handlers receive it`() = runTest {
            // Arrange
            val handler = createHandler()
            val event = ExceptionAnalyticsEvent(exception = IllegalStateException("test"))

            // Act
            handler.sendException(event)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { exceptionHandler.sendException(event) }
            allHandlers.forEach { verify(exactly = 0) { it.send(any()) } }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class UserId {

        @Test
        fun `GIVEN hex user id WHEN setUserId THEN user id handlers receive its sha256`() = runTest {
            // Arrange
            val handler = createHandler()
            val sentUserId = slot<String>()
            every { userIdHandler.setUserId(capture(sentUserId)) } returns Unit
            val expectedHash = MessageDigest.getInstance("SHA-256")
                .digest(byteArrayOf(0x0A, 0x1B))
                .joinToString(separator = "") { "%02x".format(it) }

            // Act
            handler.setUserId("0A1B")
            advanceUntilIdle()

            // Assert
            assertThat(sentUserId.captured).ignoringCase().isEqualTo(expectedHash)
        }

        @Test
        fun `GIVEN user id set WHEN clearUserId THEN user id handlers are cleared`() = runTest {
            // Arrange
            val handler = createHandler()

            // Act
            handler.clearUserId()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { userIdHandler.clearUserId() }
        }
    }

    private fun TestScope.createHandler(
        handlers: Lazy<Set<AnalyticsHandler>> = Lazy { allHandlers },
        filters: List<AnalyticsEventFilter> = emptyList(),
        staticInterceptors: Set<ParamsInterceptor> = emptySet(),
    ) = DefaultAnalyticsEventHandler(
        handlers = handlers,
        filters = Lazy { filters },
        staticInterceptors = staticInterceptors,
        dispatcher = StandardTestDispatcher(testScheduler),
    )

    private fun createFilter(
        isApplicable: Boolean,
        canBeSent: Boolean = true,
        consumer: AnalyticsHandler? = null,
    ): AnalyticsEventFilter = mockk {
        every { canBeAppliedTo(any()) } returns isApplicable
        coEvery { canBeSent(any()) } returns canBeSent
        every { canBeConsumedByHandler(any(), any()) } answers { firstArg<AnalyticsHandler>() == consumer }
    }

    private fun createInterceptor(id: String, key: String, isApplicable: Boolean): ParamsInterceptor = mockk {
        every { id() } returns id
        every { canBeAppliedTo(any()) } returns isApplicable
        every { intercept(any()) } answers { firstArg<MutableMap<String, String>>()[key] = key }
    }

    private interface UserIdTestHandler : AnalyticsHandler, AnalyticsUserIdHandler

    private interface ErrorTestHandler : AnalyticsHandler, AnalyticsErrorHandler

    private interface ExceptionTestHandler : AnalyticsHandler, AnalyticsExceptionHandler

    private class OneTimePerSessionTestEvent :
        AnalyticsEvent(category = "Test", event = "OneTime"),
        OneTimePerSessionEvent {

        override val oneTimeEventId: String = "one-time"
    }
}