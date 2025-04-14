package com.tangem.core.decompose.model

/**
 * Lazy container for [Model] params.
 *
 * This contrainer can be accessed by DI because it's provided via [com.tangem.core.decompose.di.ModelComponent].
 * */
interface ParamsContainer {

    /** Returns stored value if it is of type [T], otherwise returns null. */
    fun <T> get(): T?

    /** Returns stored value if it is of type [T], otherwise throws an exception. */
    fun <T> require(): T
}

/**
 * Mutable implementation of [ParamsContainer].
 *
 * ***You should not use this class directly in other modules, use immutable [ParamsContainer] instead.***
 * */
class MutableParamsContainer private constructor() : ParamsContainer {

    private var value: Any? = null

    /** Stores [value] inside a container, replaces any previous stored value. */
    fun set(value: Any) {
        this.value = value
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(): T? = value as? T

    override fun <T> require(): T = get() ?: error("Contrainer is empty or contains a value of a different type.")

    companion object {

        /** Creates a new [MutableParamsContainer] and stores [value] inside it. */
        operator fun <T : Any> invoke(value: T): MutableParamsContainer {
            return MutableParamsContainer().apply {
                set(value)
            }
        }
    }
}