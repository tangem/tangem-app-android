package com.tangem.tap.common

import timber.log.Timber

class CompositionCounter(
    val id: String,
    count: Int = 0,
) {
    var count: Int = count
        private set

    fun increase(id: String): CompositionCounter {
        if (this.id != id) return this

        count += 1
        return CompositionCounter(id, count)
    }
}

class CompositionLogger(
    private val recomposeViewId: String,
    private val tag: String = recomposeViewId,
    private var turnOnForIds: List<String> = listOf(recomposeViewId),
) {
    val count: Int
        get() = compositionCounter.count

    private var compositionCounter: CompositionCounter = CompositionCounter(recomposeViewId)

    fun nextComposition() {
        compositionCounter = compositionCounter.increase(recomposeViewId)
        log("")
    }

    fun log(message: String) {
        if (!turnOnForIds.contains(recomposeViewId)) return

        Timber.d("$tag[$recomposeViewId]:[${compositionCounter.count}]: $message")
    }
}
