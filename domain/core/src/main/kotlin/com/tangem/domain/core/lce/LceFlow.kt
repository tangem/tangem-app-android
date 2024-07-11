package com.tangem.domain.core.lce

import arrow.core.raise.Raise
import com.tangem.domain.core.utils.lceContent
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.core.utils.lceLoading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlin.experimental.ExperimentalTypeInference

/**
 * A [Flow] of [Lce]
 *
 * @param E The type of the error object.
 * @param C The type of the content object.
 * */
typealias LceFlow<E, C> = Flow<Lce<E, C>>

/**
 * A class that wraps a [LceRaise] instance for [Lce] type within a [ProducerScope].
 * It provides methods to handle [Lce] instances and raise errors within a [Flow].
 *
 * @property raise The [LceRaise] instance that this class wraps.
 * @property producerScope The [ProducerScope] instance that this class wraps.
 * @property ifLoading The function to call if a loading state is raised.
 */
class LceFlowScope<E : Any, C : Any> @PublishedApi internal constructor(
    private val raise: LceRaise<E>,
    private val producerScope: ProducerScope<Lce<E, C>>,
    private val ifLoading: suspend LceFlowScope<E, C>.(C?) -> Unit,
) : Raise<E>, CoroutineScope by producerScope {

    /**
     * Sends a error of type [E] within the [ProducerScope] and then closes it for send.
     * All subsequent sends will be ignored.
     *
     * This method blocks the coroutine until a error is handled by the receiver.
     *
     * If the [ProducerScope] is already closed for send (e.g. after rising another error), it just raises [r]
     * without closing.
     *
     * @param r Error to raise.
     */
    override fun raise(r: E): Nothing {
        producerScope.trySendBlocking(r.lceError())
        producerScope.close()

        raise.raise(r.lceError())
    }

    /**
     * Sends a [content] value within the [ProducerScope].
     *
     * If the content is still loading, it calls [ifLoading] lambda to retrieve a state.
     * Otherwise, it wraps the content in a [Lce.Content] state.
     *
     * This method suspends until the [Lce] instance is handled by the receiver.
     *
     * If the [ProducerScope] is closed for send (e.g. after rising a error), it does nothing.
     *
     * @param content The content value to send.
     * @param isStillLoading A flag indicating whether the content is still loading.
     */
    suspend fun send(content: C, isStillLoading: Boolean = false) {
        val value = if (isStillLoading) {
            ifLoading(content)
            return
        } else {
            content.lceContent()
        }

        send(value)
    }

    /**
     * Sends an [Lce] instance within the [ProducerScope].
     *
     * This method suspends until the [Lce] instance is handled by the receiver.
     *
     * If the [ProducerScope] is closed for send (e.g. after rising a error), it does nothing.
     *
     * @param value The [Lce] instance to send.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun send(value: Lce<E, C>) {
        if (producerScope.isClosedForSend) return

        producerScope.send(value)
    }
}

/**
 * Creates a [LceFlow] by executing the given [block] within a [LceFlowScope] context.
 *
 * Flow starts with a [Lce.Loading] state.
 *
 * @param ifLoading The function to call if received a loading content.
 * By default, it creates a new [Lce.Loading] state with the value returned by the [block].
 * @param block The block to execute within a [LceFlowScope] context.
 * @return A [LceFlow] representing the result of the [block].
 */
@OptIn(ExperimentalTypeInference::class)
fun <E : Any, C : Any> lceFlow(
    ifLoading: suspend LceFlowScope<E, C>.(C?) -> Unit = { send(lceLoading(partialContent = it)) },
    @BuilderInference block: suspend LceFlowScope<E, C>.() -> Unit,
): LceFlow<E, C> {
    return channelFlow {
        trySend(lceLoading())

        lce {
            val scope = LceFlowScope(
                raise = this@lce,
                producerScope = this@channelFlow,
                ifLoading = ifLoading,
            )

            block(scope)
        }
    }
}
