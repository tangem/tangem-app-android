package com.tangem.tap.common.extensions

/**
[REDACTED_AUTHOR]
 */
fun <T> List<T>.containsAny(list: List<T>): Boolean {
    this.forEach { mainItem ->
        list.forEach { if (it == mainItem) return true }
    }
    return false
}

fun <T> MutableList<T>.removeBy(predicate: (T) -> Boolean): Boolean {
    val toRemove = this.filter(predicate)
    this.removeAll(toRemove)
    return toRemove.isNotEmpty()
}

fun <T> MutableList<T>.replaceBy(item: T, predicate: (T) -> Boolean) {
    val toRemove = this.filter(predicate)
    val indexes = toRemove.map { indexOf(it) }
    this.removeAll(toRemove)
    indexes.forEach { this.add(it, item) }
}