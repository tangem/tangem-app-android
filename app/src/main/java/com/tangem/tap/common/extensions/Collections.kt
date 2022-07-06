package com.tangem.tap.common.extensions

/**
 * Created by Anton Zhilenkov on 08/04/2021.
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

fun <T> MutableList<T>.replaceBy(item: T, predicate: (T) -> Boolean): Boolean {
    val toRemove = this.filter(predicate)
    if (toRemove.isEmpty()) return false

    val indexes = toRemove.map { indexOf(it) }
    this.removeAll(toRemove)
    indexes.forEach { this.add(it, item) }
    return true
}

fun <T> MutableList<T>.replaceByOrAdd(item: T, predicate: (T) -> Boolean) {
    if (!replaceBy(item, predicate)) add(item)
}
