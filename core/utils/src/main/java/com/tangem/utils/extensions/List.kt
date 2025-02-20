package com.tangem.utils.extensions

/**
 * Removes elements from the collection based on the provided predicate.
 *
 * !!!This function is not thread-safe!!!
 *
 * @param predicate The condition to remove an element.
 * @return [Boolean] indicating whether an element was removed.
 */
fun <T> MutableList<T>.removeBy(predicate: (T) -> Boolean): Boolean {
    val toRemove = this.filter(predicate)
    this.removeAll(toRemove)
    return toRemove.isNotEmpty()
}

/**
 * Replaces an element in the list with the provided item based on the predicate.
 *
 * @param item The element to replace the existing one.
 * @param predicate The condition to replace an existing element.
 * @return [Boolean] indicating whether an element was replaced.
 */
inline fun <T> MutableList<T>.replaceBy(item: T, predicate: (T) -> Boolean): Boolean {
    val index = indexOfFirst(predicate)

    if (index == -1) {
        return false
    }

    this[index] = item

    return true
}

/**
 * Adds the specified element to the list or replaces an existing element.
 *
 * !!!This function is not thread-safe!!!
 *
 * @param item The element to be added or replace the existing one.
 * @param predicate The condition to replace an existing element.
 * @return The modified [List] after adding or replacing the element.
 */
inline fun <T> List<T>.addOrReplace(item: T, predicate: (T) -> Boolean): List<T> {
    val mutableList = this.toMutableList()

    mutableList.addOrReplace(item, predicate)

    return mutableList
}

/**
 * Adds the specified element to the mutable list or replaces an existing element.
 *
 * !!!This function is not thread-safe!!!
 *
 * @param item The element to be added or replace the existing one.
 * @param predicate The condition to replace an existing element.
 */
inline fun <T> MutableList<T>.addOrReplace(item: T, predicate: (T) -> Boolean) {
    val isReplaced = replaceBy(item, predicate)

    if (!isReplaced) {
        add(item)
    }
}

fun <T> List<T>.filterIf(condition: Boolean, predicate: (T) -> Boolean): List<T> {
    return if (condition) {
        this.filter(predicate)
    } else {
        this
    }
}