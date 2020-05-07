package com.tangem.devkit._arch.structure.abstraction

import ru.dev.gbixahue.eu4d.lib.kotlin.common.Converter

/**
[REDACTED_AUTHOR]
 */
interface DefaultConverter<A, B> {
    fun convert(from: A, default: B): B
}

interface TwoWayConverter<A, B> {
    fun aToB(from: A): B
    fun bToA(from: B): A
}

interface ItemsToModel<M> : DefaultConverter<List<Item>, M>
interface ModelToItems<M> : Converter<M, List<Item>>

interface ModelConverter<M> : DefaultConverter<List<Item>, M>, Converter<M, List<Item>>