package com.tangem.domain.core.lce

import arrow.core.raise.Raise
import com.tangem.domain.core.utils.lceContent
import com.tangem.domain.core.utils.lceLoading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
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
 * @property scope The [ProducerScope] that this class operates within.
 * @property ifLoading The function to call if a loading state is raised.
 */
class LceFlowScope<E : Any, C : Any> @PublishedApi internal constructor(
    private val raise: LceRaise<E>,
    private val scope: ProducerScope<Lce<E, C>>,
    private val ifLoading: LceRaise<E>.(C) -> Lce<E, C>,
) : Raise<Lce<E, Nothing>> by raise,
    CoroutineScope by scope {

    /**
     * Raises an [Lce] instance within the [ProducerScope].
     * It closes the [ProducerScope] after raise.
     *
     * @param r The [Lce] instance to raise.
     */
    override fun raise(r: Lce<E, Nothing>): Nothing {
        scope.launch(NonCancellable) {
            scope.send(r)
            scope.close()
        }

        raise.raise(r)
    }

    /**
     * Sends a content value within the [ProducerScope].
     * If the content is still loading, it calls [ifLoading] lambda to retrieve a state.
     * Otherwise, it wraps the content in a [Lce.Content] state.
     *
     * @param content The content value to send.
     * @param isStillLoading A flag indicating whether the content is still loading.
     */
    suspend fun send(content: C, isStillLoading: Boolean = false) {
        val value = if (isStillLoading) {
            ifLoading(raise, content)
        } else {
            content.lceContent()
        }

        scope.send(value)
    }
}

/**
 * Creates a [LceFlow] by executing the given [block] within a [LceFlowScope] context.
 *
 * Flow starts with a [Lce.Loading] state.
 *
 * @param ifLoading The function to call if the [block] raises a [Lce.Loading] state.
 * By default, it creates a new [Lce.Loading] state with the value returned by the [block].
 * @param block The block to execute within a [LceFlowScope] context.
 * @return A [LceFlow] representing the result of the [block].
 */
@OptIn(ExperimentalTypeInference::class)
fun <E : Any, C : Any> lceFlow(
    ifLoading: LceRaise<E>.(C) -> Lce<E, C> = { lceLoading(partialContent = it) },
    @BuilderInference block: suspend LceFlowScope<E, C>.() -> Unit,
): LceFlow<E, C> {
    return channelFlow {
        trySend(lceLoading())

        lce {
            val scope = LceFlowScope(
                raise = this@lce,
                scope = this@channelFlow,
                ifLoading = ifLoading,
            )

            block(scope)
        }
    }
}