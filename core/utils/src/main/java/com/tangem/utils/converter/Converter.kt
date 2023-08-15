package com.tangem.utils.converter

interface Converter<I : Any, O : Any?> {

    fun convert(value: I): O

    fun convertList(input: Collection<I>): List<O> {
        return input.map(::convert)
    }

    fun convertSet(input: Collection<I>): Set<O> {
        return input.mapTo(hashSetOf(), ::convert)
    }
}
