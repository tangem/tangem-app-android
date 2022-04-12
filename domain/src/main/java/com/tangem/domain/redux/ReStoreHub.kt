package com.tangem.domain.redux

import android.webkit.ValueCallback
import com.tangem.domain.common.FeatureCoroutineExceptionHandler
import com.tangem.domain.common.extensions.withIOContext
import com.tangem.domain.common.extensions.withMainContext
import kotlinx.coroutines.*
import org.rekotlin.Action
import org.rekotlin.DispatchFunction
import org.rekotlin.Middleware
import java.util.concurrent.Executors

/**
[REDACTED_AUTHOR]
 * ReStoreHub's should not store the <StoreState> or the <State>, because this can lead to destabilization of
 * a state behavior.
 * All ReStoreHub's must be marked as internal
 */
internal interface ReStoreHub<StoreState, State> : HubMiddleware<StoreState>, HubReducer<StoreState>

internal interface HubMiddleware<StoreState> {
    fun getMiddleware(): Middleware<StoreState>
}

internal interface HubReducer<StoreState> {
    fun reduce(action: Action, storeState: StoreState): StoreState
}

/**
 * ReStoreHub is the entry point for actions. It processes it through middleware and reducer.
 * Actions handled by ReStoreHub go into coroutine scope, which can be canceled while the action is being processed.
 * All action went from the middleware must be dispatched through ReStoreHub.dispatchOnMain(Actions) to prevent
 * concurrent modification in the Store
 * Only the changed hub State will change its state in the DomainState
 * @param name - name of the Hub
 * @param dispatcher - main coroutine dispatcher for actions
 */
internal abstract class BaseStoreHub<State>(
    private val name: String,
    private val dispatcher: CoroutineDispatcher = Executors.newFixedThreadPool(1).asCoroutineDispatcher()
) : ReStoreHub<DomainState, State> {

    val hubScope = CoroutineScope(
        Job() + dispatcher + CoroutineName(name) + FeatureCoroutineExceptionHandler.create(name)
    )

    private val actionsAndJobs = mutableMapOf<Action, Job>()

    override fun getMiddleware(): Middleware<DomainState> {
        return { dispatch, state ->
            { next ->
                { action ->
                    handle(state, action, dispatch)
                    next(action)
                }
            }
        }
    }

    /**
     * Launches new coroutine and stores the action with it's coroutine job. (Coroutine can be cancelled
     * through invoking the cancelActionJob() function inside a middleware).
     * Removes the action when job is completed.
     */
    protected open fun handle(storeStateHolder: () -> DomainState?, action: Action, dispatch: DispatchFunction) {
        val storeState = storeStateHolder()
            ?: throw UnsupportedOperationException("StoreState for the $name can't be NULL")

        hubScope.launch {
            actionsAndJobs[action] = this.coroutineContext.job
            actionsAndJobs[action]?.invokeOnCompletion { actionsAndJobs.remove(action) }

            handleAction(action, storeState) {
                actionsAndJobs.remove(it)?.cancel()
            }
        }
    }

    /**
     * Reduce the action and check it. If the action hasn't updated the hubState, then it doesn't need to update
     * storeState
     */
    override fun reduce(action: Action, storeState: DomainState): DomainState {
        val oldState = getHubState(storeState)
        val newState = reduceAction(action, oldState)
        return if (oldState === newState) {
            storeState
        } else {
            updateStoreState(storeState, newState)
        }
    }

    protected abstract suspend fun handleAction(action: Action, storeState: DomainState, cancel: ValueCallback<Action>)
    protected abstract fun reduceAction(action: Action, state: State): State

    protected abstract fun getHubState(storeState: DomainState): State
    protected abstract fun updateStoreState(storeState: DomainState, newHubState: State): DomainState

}

internal suspend inline fun ReStoreHub<*, *>.dispatchOnMain(vararg actions: Action) {
    withMainContext { actions.forEach { domainStore.dispatch(it) } }
}

internal suspend inline fun ReStoreHub<*, *>.dispatchOnIO(vararg actions: Action) {
    withIOContext { actions.forEach { domainStore.dispatch(it) } }
}
