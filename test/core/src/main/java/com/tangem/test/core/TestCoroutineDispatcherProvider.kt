package com.tangem.test.core

import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope

@OptIn(ExperimentalCoroutinesApi::class)
fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider =
    testScheduler.createTestingCoroutineDispatcherProvider()

@OptIn(ExperimentalCoroutinesApi::class)
fun TestCoroutineScheduler.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
    val testDispatcher = StandardTestDispatcher(scheduler = this)

    return TestingCoroutineDispatcherProvider(
        main = testDispatcher,
        mainImmediate = testDispatcher,
        io = testDispatcher,
        default = testDispatcher,
        single = testDispatcher,
    )
}