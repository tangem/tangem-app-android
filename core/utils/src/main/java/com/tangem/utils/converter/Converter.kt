package com.tangem.utils.converter

interface Converter<I : Any, O : Any?> {

    fun convert(value: I): O

    fun convertList(input: Collection<I>): List<O> {
        return input.map(::convert)
    }

    fun convertSet(input: Collection<I>): Set<O> {
        return input.mapTo(hashSetOf(), ::convert)
    }

    fun convertListIgnoreErrors(input: Collection<I>, onError: ((Throwable) -> Unit)? = null): List<O> {
        return input.mapNotNull {
            try {
                convert(it)
            } catch (throwable: Throwable) {
                onError?.invoke(throwable)
                null
            }
        }
    }

    fun <T> T?.asMandatory(name: String): T {
        return this ?: error("$name must not be null")
    }
}