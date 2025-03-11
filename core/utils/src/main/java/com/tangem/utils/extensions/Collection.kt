package com.tangem.utils.extensions

/**
[REDACTED_AUTHOR]
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

inline fun <T> List<T>.indexOfFirstOrNull(predicate: (T) -> Boolean): Int? {
    val index = indexOfFirst(predicate)
    return if (index == -1) null else index
}