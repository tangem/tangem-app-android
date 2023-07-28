package com.tangem.utils.extensions

/**
* [REDACTED_AUTHOR]
 */

/**
 * Checks if the [Collection] contains a single item.
 *
 * @return [Boolean] indicating whether the [Collection] contains exactly one element.
 */
fun <T> Collection<T>.isSingleItem(): Boolean = this.size == 1

/**
 * Creates a shallow copy of this [Collection].
 *
 * @return A copy of the [Collection].
 */
fun <T> Collection<T>.copy(): Collection<T> {
    return this.map { it }
}

/**
 * Adds the specified element to the collection or replaces an existing element.
 * The predicate defines the condition to replace the existing element.
 *
 * @param item The element to be added or replace the existing one.
 * @param predicate The condition to replace an existing element.
 * @return The modified [List] after adding or replacing the element.
 */
inline fun <T> Collection<T>.plusOrReplace(item: T, predicate: (T) -> Boolean): List<T> {
    val mutableList = this as? MutableList ?: ArrayList(this)

    mutableList.addOrReplace(item, predicate)

    return mutableList
}

/**
 * Adds the specified element to the collection or replaces an existing element.
 * The predicate defines the condition to replace the existing element.
 *
 * @param item The element to be added or replace the existing one.
 * @param predicate The condition to replace an existing element.
 */
inline fun <T> MutableCollection<T>.addOrReplace(item: T, predicate: (T) -> Boolean) {
    val isReplaced = replaceBy(item, predicate)

    if (!isReplaced) {
        add(item)
    }
}

/**
 * Removes an element from the collection based on the provided predicate.
 *
 * @param predicate The condition to remove an element.
 * @return [Boolean] indicating whether an element was removed.
 */
inline fun <T> MutableCollection<T>.removeBy(predicate: (T) -> Boolean): Boolean {
    var removed = false
    val iterator = this.iterator()

    for (e in iterator) {
        if (predicate(e)) {
            iterator.remove()
            removed = true

            break
        }
    }

    return removed
}

/**
 * Replaces an element in the collection with the provided item based on the predicate.
 *
 * @param item The element to replace the existing one.
 * @param predicate The condition to replace an existing element.
 * @return [Boolean] indicating whether an element was replaced.
 */
inline fun <T> MutableCollection<T>.replaceBy(item: T, predicate: (T) -> Boolean): Boolean {
    var replaced = false
    val mutableList = this as? MutableList ?: ArrayList(this)
    val iterator = mutableList.listIterator()

    for (e in iterator) {
        if (predicate(e)) {
            iterator.set(item)
            replaced = true

            break
        }
    }

    return replaced
}
