package com.tangem.common.test.utils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> TestScope.getEmittedValues(flow: Flow<T>): List<T> {
    val values = mutableListOf<T>()

    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        flow.toList(values)
    }

    return values
}