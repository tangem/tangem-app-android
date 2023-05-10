package com.tangem.tap.common.extensions

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

fun <T> MutableList<T>.copy(): MutableList<T> {
    return this.map { it }.toMutableList()
}