package com.tangem.utils.extensions

/**
 * Replaces an element in the set with the provided item based on the predicate.
 *
 * !!!This function is not thread-safe!!!
 *
 * @param item The element to replace the existing one.
 * @param predicate The condition to replace an existing element.
 * @return [Boolean] indicating whether an element was replaced.
 */
inline fun <T> MutableSet<T>.replaceBy(item: T, predicate: (T) -> Boolean): Boolean {
    val foundItem = firstOrNull(predicate) ?: return false

    remove(foundItem)
    add(item)

    return true
}

/**
 * Adds the specified element to the set or replaces an existing element.
 *
 * !!!This function is not thread-safe!!!
 *
 * @param item The element to be added or replace the existing one.
 * @param predicate The condition to replace an existing element.
 * @return The modified [Set] after adding or replacing the element.
 */
inline fun <T> Set<T>.addOrReplace(item: T, predicate: (T) -> Boolean): Set<T> {
    val mutableList = this.toMutableSet()
    val isReplaced = mutableList.replaceBy(item, predicate)

    if (!isReplaced) {
        mutableList.add(item)
    }

    return mutableList
}

inline fun <T> Set<T>.addOrReplace(items: Set<T>, predicate: (T, T) -> Boolean): Set<T> {
    val updatedValues = this.toMutableSet()

    items.forEach { newValue ->
        val isReplaced = updatedValues.replaceBy(item = newValue, predicate = { predicate(it, newValue) })

        if (!isReplaced) updatedValues.add(newValue)
    }

    return updatedValues
}