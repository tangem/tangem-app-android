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

/**
 * Adds an element to the mutable list if the specified condition is true.
 *
 * @param condition The condition to evaluate.
 * @param create A lambda function that creates the element to be added.
 */
inline fun <T> MutableCollection<T>.addIf(condition: Boolean, create: () -> T) {
    addIf(condition = condition, element = create())
}

/**
 * Adds an element to the mutable list if the specified condition is true.
 *
 * @param condition The condition to evaluate.
 * @param element The element to be added.
 */
fun <T> MutableCollection<T>.addIf(condition: Boolean, element: T) {
    if (condition) this.add(element)
}