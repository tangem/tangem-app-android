package com.tangem.domain.core.lce

import arrow.atomic.Atomic
import arrow.core.raise.Raise
import arrow.core.raise.recover
import com.tangem.domain.core.utils.lceContent
import com.tangem.domain.core.utils.lceLoading
import kotlin.experimental.ExperimentalTypeInference

class LceRaise<E : Any>(val raise: Raise<Lce<E, Nothing>>) : Raise<Lce<E, Nothing>> by raise {

    var isLoading: Atomic<Boolean> = Atomic(false)

    fun <A : Any> Lce<E, A>.bind(): A? = when (this) {
        is Lce.Loading -> {
            isLoading.set(true)

            partialContent
        }
        is Lce.Content -> content
        is Lce.Error -> raise.raise(r = this)
    }
}

@OptIn(ExperimentalTypeInference::class)
inline fun <E : Any, A : Any> lce(@BuilderInference block: LceRaise<E>.() -> A): Lce<E, A> = recover(
    block = {
        val raise = LceRaise(raise = this)
        val value = block(raise)

        if (raise.isLoading.get()) {
            lceLoading(partialContent = value)
        } else {
            value.lceContent()
        }
    },
    recover = { e: Lce<E, Nothing> -> e },
)
