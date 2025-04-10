package com.tangem.common.test.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> CoroutineScope.getEmittedValues(testScheduler: TestCoroutineScheduler, actual: Flow<T>): List<T> {
    val values = mutableListOf<T>()

    launch(UnconfinedTestDispatcher(testScheduler)) {
        actual.toList(values)
    }

    return values
}