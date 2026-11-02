package com.tangem.features.polymarket.common.ui.counter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs

internal fun isCounterSettled(isScrolling: Boolean, pageOffsetFraction: Float, pageSize: Int): Boolean {
    // The last page's center padding can differ by a fraction of a pixel after layout rounding.
    return !isScrolling && abs(pageOffsetFraction * pageSize) <= 1f
}

internal data class CounterInput(val ids: List<String>, val selectedId: String?) {
    val selectedIndex: Int get() = ids.indexOf(selectedId)
}

internal class CounterSelection {

    private var synchronizedInput by mutableStateOf<CounterInput?>(null)
    private var previousInput: CounterInput? = null
    private var lastEmittedId: String? = null

    fun needsPositioning(input: CounterInput): Boolean {
        val previous = previousInput
        previousInput = input
        val isAcknowledgement = input.selectedId == lastEmittedId && input.ids == previous?.ids
        return !isAcknowledgement && input != previous
    }

    fun synchronized(input: CounterInput) {
        synchronizedInput = input
        lastEmittedId = input.selectedId
    }

    fun isSynchronized(input: CounterInput): Boolean = synchronizedInput == input

    fun selection(input: CounterInput, settledIndex: Int): String? {
        if (synchronizedInput != input || input.selectedIndex < 0) return null
        val id = input.ids.getOrNull(settledIndex) ?: return null
        if (id == input.selectedId || id == lastEmittedId) return null
        lastEmittedId = id
        return id
    }
}