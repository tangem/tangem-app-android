package com.tangem.domain.store

import android.webkit.ValueCallback
import com.tangem.domain.common.FeatureCoroutineExceptionHandler
import com.tangem.domain.common.extensions.withIOContext
import com.tangem.domain.common.extensions.withMainContext
import kotlinx.coroutines.*
import org.rekotlin.Action
import org.rekotlin.DispatchFunction
import org.rekotlin.Middleware

/**
[REDACTED_AUTHOR]
 */
interface StoreHub<StoreState, State> {
    val initialState: State
    val middleware: Middleware<StoreState>
    fun reduceAction(action: Action, state: State): State
}

/**
 * Hub contains the entry points for actions. It processes it through middleware and reducer.
 * All action went from the middleware must be dispatched through StoreHub.dispatchOnMain(Actions)
 * and StoreHub.dispatchOnIO(Actions)

 * Hub is the provider of an initial state of a State.
 *
 * @param name - name of the Hub
 */
abstract class BaseStoreHub<State>(
    private val name: String,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : StoreHub<DomainState, State> {

    protected val actionsAndJobs = mutableMapOf<Action, Job>()
    protected val hubScope = CoroutineScope(
        Job() + dispatcher + CoroutineName(name) + FeatureCoroutineExceptionHandler.create(name)
    )

    /**
     * Main entry point for the all actions
     */
    override val middleware: Middleware<DomainState> = { dispatch, state ->
        { next ->
            { action ->
                handle(state, action, dispatch)
                next(action)
            }
        }
    }

    /**
     * Launches new coroutine and stores the action with it's coroutine job. (Coroutine can be cancelled
     * through invoking the cancelActionJob() function inside a middleware).
     * Removes the action when job is completed.
     */
    protected open fun handle(state: () -> DomainState?, action: Action, dispatch: DispatchFunction) {
        val domainState = state() ?: throw UnsupportedOperationException("State for the $name can't be NULL")

        hubScope.launch {
            actionsAndJobs[action] = this.coroutineContext.job
            actionsAndJobs[action]?.invokeOnCompletion { actionsAndJobs.remove(action) }

            handleAction(
                state = domainState,
                action = action,
                dispatch = dispatch,
                cancel = { actionsAndJobs.remove(it)?.cancel() }
            )
        }
    }

    protected abstract suspend fun handleAction(
        state: DomainState,
        action: Action,
        dispatch: DispatchFunction,
        cancel: ValueCallback<Action>,
    )
}

internal suspend inline fun StoreHub<*, *>.dispatchOnMain(vararg actions: Action) {
    withMainContext { actions.forEach { domainStore.dispatch(it) } }
}

internal suspend inline fun StoreHub<*, *>.dispatchOnIO(vararg actions: Action) {
    withIOContext { actions.forEach { domainStore.dispatch(it) } }
}