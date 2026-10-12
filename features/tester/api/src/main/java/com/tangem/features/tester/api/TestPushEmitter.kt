package com.tangem.features.tester.api

interface TestPushEmitter {
    fun emit(data: Map<String, String>)
}