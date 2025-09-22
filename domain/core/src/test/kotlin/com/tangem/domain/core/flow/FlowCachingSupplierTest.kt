package com.tangem.domain.core.flow

import arrow.core.Option
import arrow.core.some
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
[REDACTED_AUTHOR]
 */
internal class FlowCachingSupplierTest {

    private val factory = mockk<MockFlowProducer.Factory>()
    private val errorFactory = mockk<MockErrorFlowProducer.Factory>()

    @Test
    fun `flow was created before`() = runTest {
        val flowsStore = MutableStateFlow<Map<String, Flow<String>>>(value = emptyMap())
        val supplier = MockFlowCachingSupplier(factory = factory, flowsStore = flowsStore)

        val params = 1

        every { factory.create(params = params) } returns MockFlowProducer(params = params)

        val actual = supplier(params = params)

        verify { factory.create(params = params) }

        Truth.assertThat(actual.first()).isEqualTo("test_1")

        Truth.assertThat(flowsStore.value.size).isEqualTo(1)
        Truth.assertThat(flowsStore.value.containsKey("mock_1")).isTrue()
        Truth.assertThat(flowsStore.value["mock_1"]!!.first()).isEqualTo("test_1")
    }

    @Test
    fun `flow wasn't created before`() = runTest {
        val storedFlow = flowOf("test_1")
        val storedMap = mapOf("mock_1" to storedFlow)
        val flowsStore = MutableStateFlow(value = storedMap)

        val supplier = MockFlowCachingSupplier(factory = factory, flowsStore = flowsStore)

        val params = 1

        val actual = supplier(params = params)

        verify(inverse = true) { factory.create(params = params) }

        Truth.assertThat(actual).isEqualTo(storedFlow)
        Truth.assertThat(flowsStore.value).isEqualTo(storedMap)
    }

    @Test
    fun `flow wasn't created before and store isn't empty`() = runTest {
        val flowsStore = MutableStateFlow(
            value = mapOf("mock_1" to flowOf("test_1")),
        )

        val supplier = MockFlowCachingSupplier(factory = factory, flowsStore = flowsStore)

        val params = 2

        every { factory.create(params = params) } returns MockFlowProducer(params = params)

        val actual = supplier(params = params)

        verify { factory.create(params = params) }

        Truth.assertThat(actual.first()).isEqualTo("test_2")

        Truth.assertThat(flowsStore.value.size).isEqualTo(2)
        Truth.assertThat(flowsStore.value.keys).isEqualTo(setOf("mock_1", "mock_2"))
        Truth.assertThat(flowsStore.value["mock_1"]!!.first()).isEqualTo("test_1")
        Truth.assertThat(flowsStore.value["mock_2"]!!.first()).isEqualTo("test_2")
    }

    @Test
    fun `flow throws exception`() = runTest {
        val flowsStore = MutableStateFlow<Map<String, Flow<String>>>(value = emptyMap())
        val supplier = MockErrorFlowCachingSupplier(factory = errorFactory, flowsStore = flowsStore)

        val params = 1

        every { errorFactory.create(params = params) } returns MockErrorFlowProducer()

        val actual = supplier(params = params)

        verify { errorFactory.create(params = params) }

        Truth.assertThat(actual.first()).isEqualTo("fallback")
        Truth.assertThat(flowsStore.value.size).isEqualTo(1)
    }

    private class MockFlowCachingSupplier(
        override val factory: FlowProducer.Factory<Int, MockFlowProducer>,
        flowsStore: MutableStateFlow<Map<String, Flow<String>>>,
    ) : FlowCachingSupplier<MockFlowProducer, Int, String>(flowsStore = flowsStore) {

        override val keyCreator: (Int) -> String = { "mock_$it" }
    }

    private class MockFlowProducer(private val params: Int) : FlowProducer<String> {

        override val fallback: Option<String>
            get() = "fallback".some()

        override fun produce(): Flow<String> = flowOf("test_$params")

        class Factory : FlowProducer.Factory<Int, MockFlowProducer> {
            override fun create(params: Int): MockFlowProducer = MockFlowProducer(params = params)
        }
    }

    private class MockErrorFlowCachingSupplier(
        override val factory: FlowProducer.Factory<Int, MockErrorFlowProducer>,
        flowsStore: MutableStateFlow<Map<String, Flow<String>>>,
    ) : FlowCachingSupplier<MockErrorFlowProducer, Int, String>(flowsStore = flowsStore) {

        override val keyCreator: (Int) -> String = { "mock_$it" }
    }

    private class MockErrorFlowProducer : FlowProducer<String> {

        override val fallback: Option<String>
            get() = "fallback".some()

        override fun produce(): Flow<String> = flow {
            throw IllegalStateException()
        }

        class Factory : FlowProducer.Factory<Int, MockErrorFlowProducer> {
            override fun create(params: Int): MockErrorFlowProducer = MockErrorFlowProducer()
        }
    }
}