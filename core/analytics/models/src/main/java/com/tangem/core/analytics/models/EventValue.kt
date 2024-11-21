package com.tangem.core.analytics.models

sealed class EventValue {
    data class StringValue(val value: String) : EventValue()
    data class ListValue(val value: List<String>) : EventValue()

    fun getValue(): Any {
        return when (this) {
            is StringValue -> this.value
            is ListValue -> this.value
        }
    }
}