package com.tangem.core.analytics.handlers.firebase

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FirebasePerformanceTracerTest {

    private val firebasePerformance: FirebasePerformance = mockk()
    private val trace: Trace = mockk(relaxUnitFun = true)

    private val tracer = FirebasePerformanceTracer()

    @BeforeAll
    fun mockFirebasePerformance() {
        mockkStatic(FirebasePerformance::class)
        every { FirebasePerformance.getInstance() } returns firebasePerformance
    }

    @AfterAll
    fun unmockFirebasePerformance() {
        unmockkStatic(FirebasePerformance::class)
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(firebasePerformance, trace)
        every { firebasePerformance.newTrace(TRACE_NAME) } returns trace
    }

    @Test
    fun `GIVEN tracer WHEN start THEN named trace is created and started`() {
        // Act
        tracer.start(TRACE_NAME)

        // Assert
        verify(exactly = 1) { firebasePerformance.newTrace(TRACE_NAME) }
        verify(exactly = 1) { trace.start() }
    }

    @Test
    fun `GIVEN started trace WHEN putAttribute and stop THEN both reach the Firebase trace`() {
        // Arrange
        val performanceTrace = tracer.start(TRACE_NAME)

        // Act
        performanceTrace.putAttribute(ATTRIBUTE_NAME, ATTRIBUTE_VALUE)
        performanceTrace.stop()

        // Assert
        verifyOrder {
            trace.start()
            trace.putAttribute(ATTRIBUTE_NAME, ATTRIBUTE_VALUE)
            trace.stop()
        }
    }

    private companion object {
        const val TRACE_NAME = "trace"
        const val ATTRIBUTE_NAME = "attribute"
        const val ATTRIBUTE_VALUE = "value"
    }
}