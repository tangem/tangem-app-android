package com.tangem.utils.converter

interface TwoWayConverter<I, O> : Converter<I, O> {
    fun convertBack(value: O): I
    fun convertListBack(input: List<O>): List<I> {
        return input.map { convertBack(it) }
    }
}
