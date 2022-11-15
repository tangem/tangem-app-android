package com.tangem.utils.converter

interface Converter<I, O> {
    fun convert(value: I): O
    fun convertList(input: List<I>): List<O> {
        return input.map { convert(it) }
    }
}
