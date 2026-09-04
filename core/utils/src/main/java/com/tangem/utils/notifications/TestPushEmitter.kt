package com.tangem.utils.notifications

interface TestPushEmitter {
    fun emit(data: Map<String, String>)
}