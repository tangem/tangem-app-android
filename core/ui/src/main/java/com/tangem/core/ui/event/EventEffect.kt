package com.tangem.core.ui.event

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import kotlinx.coroutines.launch

/**
 * A Composable function that reacts to a given [StateEvent], executing the provided action only once when the event
 * is triggered.
 *
 * @param event The [StateEvent] to listen to.
 * @param onTrigger The action to execute when the event is triggered.
 */
@Composable
@NonRestartableComposable
@Suppress("UnnecessaryEventHandlerParameter")
fun <A> EventEffect(event: StateEvent<A>, onTrigger: suspend (data: A) -> Unit) {
    LaunchedEffect(event) {
        if (event is StateEvent.Triggered<A>) {
            launch { onTrigger(event.data) }
                .invokeOnCompletion { event.onConsume() }
        }
    }
}