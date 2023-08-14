package com.tangem.utils.converter

interface TwoWayConverter<I : Any, O> : Converter<I, O> {

    fun convertBack(value: O): I

    fun convertListBack(input: Collection<O>): List<I> {
        return input.map { convertBack(it) }
    }
}
