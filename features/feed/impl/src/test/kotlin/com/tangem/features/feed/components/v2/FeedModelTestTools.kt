package com.tangem.features.feed.components.v2

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope

/**
 * One dispatcher for all five roles, built from the test's own scheduler, so `advanceUntilIdle()`
 * drives everything the model launches.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
    val testDispatcher = StandardTestDispatcher(testScheduler)
    return TestingCoroutineDispatcherProvider(
        main = testDispatcher,
        mainImmediate = testDispatcher,
        io = testDispatcher,
        default = testDispatcher,
        single = testDispatcher,
    )
}

/**
 * Records what the slot holds after every navigation event, starting from empty — `null` is a
 * dismissal. Lets a test assert the sheets a click opened and closed, in order.
 */
internal fun <C : Any> SlotNavigation<C>.trackSlot(): List<C?> {
    val tracked = mutableListOf<C?>()
    subscribe { event -> tracked.add(event.transformer(tracked.lastOrNull())) }
    return tracked
}