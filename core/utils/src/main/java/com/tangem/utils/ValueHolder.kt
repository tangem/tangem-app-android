package com.tangem.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Executes a block of code within the context of a [ValueHolder] and returns the updated value.
 *
 * @param T The type of the value being held and updated.
 * @param default The default value to initialize the [ValueHolder] with.
 * @param block The block of code to execute, which can modify the [ValueHolder].
 * @return The updated value after executing the block.
 */
fun <T> savableContext(default: T, block: ValueHolder<T>.() -> Unit): T {
    val holder = ValueHolder(default = default)

    holder.block()

    return holder.get()
}

/**
 * A holder for a value of type [T] that allows updating the value through functions or direct assignment.
 *
 * @param default The default value to initialize the `ValueHolder` with.
 * @param T The type of the value being held.
 */
class ValueHolder<T>(default: T) {

    private val flow: MutableStateFlow<T> = MutableStateFlow(value = default)

    /**
     * Retrieves the current value held by the [ValueHolder].
     *
     * @return The current value.
     */
    fun get(): T = flow.value

    /**
     * Updates the current value by applying a transformation function to it.
     *
     * @param transform A function that takes the current value and returns the updated value.
     */
    fun update(transform: (T) -> T) {
        flow.update(transform)
    }

    /**
     * Updates the current value by directly assigning a new value.
     *
     * @param value The new value to assign.
     */
    fun update(value: T) {
        flow.value = value
    }
}