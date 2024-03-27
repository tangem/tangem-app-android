package com.tangem.domain.core.lce

import arrow.core.raise.Raise
import arrow.core.raise.recover
import com.tangem.domain.core.utils.lceContent
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.core.utils.lceLoading
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlin.experimental.ExperimentalTypeInference

typealias LceFlow<E, T> = Flow<Lce<E, T>>

class LceFlowScope<E : Any, A : Any>(
    val raise: LceRaise<E>,
    val scope: ProducerScope<Lce<E, A>>,
) : Raise<Lce<E, Nothing>> by raise,
    ProducerScope<Lce<E, A>> by scope {

    suspend fun raise(error: E) {
        send(error.lceError())
    }

    suspend fun send(content: A, isStillLoading: Boolean = false) {
        val value = if (isStillLoading) {
            lceLoading(partialContent = content)
        } else {
            content.lceContent()
        }

        send(value)
    }
}

@OptIn(ExperimentalTypeInference::class)
fun <E : Any, A : Any> lceFlow(@BuilderInference block: suspend LceFlowScope<E, A>.() -> Unit): LceFlow<E, A> =
    channelFlow {
        send(lceLoading())

        recover(
            block = {
                val scope = LceFlowScope(
                    raise = LceRaise(this),
                    scope = this@channelFlow,
                )

                block(scope)
            },
            recover = { send(it) },
        )
    }
