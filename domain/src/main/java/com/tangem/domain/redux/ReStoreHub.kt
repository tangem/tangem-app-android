package com.tangem.domain.redux

import android.webkit.ValueCallback
import com.tangem.domain.redux.global.DomainGlobalState
import com.tangem.utils.coroutines.FeatureCoroutineExceptionHandler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.DispatchFunction
import org.rekotlin.Middleware
import java.util.concurrent.Executors

/**
 * Created by Anton Zhilenkov on 30/03/2022.
 * ReStoreHub's should not store the <StoreState> or the <State>, because this can lead to destabilization of
 * a state behavior.
 * All ReStoreHub's must be marked as internal
 */
internal interface ReStoreHub<StoreState, State> {
    fun getMiddleware(): Middleware<StoreState>
    fun reduce(action: Action, domainState: StoreState): StoreState
}

internal interface ReStoreReducer<State> {
    fun reduceAction(action: Action, state: State): State
}

/**
 * ReStoreHub is the entry point for actions. It processes it through middleware and reducer.
 * Actions handled by ReStoreHub go into coroutine scope, which can be canceled while the action is being processed.
 * All action went from the middleware must be dispatched through ReStoreHub.dispatchOnMain(Actions) to prevent
 * concurrent modification in the Store
 * Only the changed hub State will change its state in the DomainState
 * Do not implement other states like as DomainGlobalState. Because it can dilute the responsibility of
 * states.
 * @param name - name of the Hub
 * @param dispatcher - main coroutine dispatcher for actions
 * @property globalState - state witch produce accessibility to global variables
 */
internal abstract class BaseStoreHub<State>(
    private val name: String,
    private val dispatcher: CoroutineDispatcher = Executors.newFixedThreadPool(1).asCoroutineDispatcher(),
) : ReStoreHub<DomainState, State> {

    val globalState: DomainGlobalState
        get() = domainStore.state.globalState

    val hubScope = CoroutineScope(
        Job() + dispatcher + CoroutineName(name) + FeatureCoroutineExceptionHandler.create(name),
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
    override fun reduce(action: Action, domainState: DomainState): DomainState {
        val hubOldState = getHubState(domainState)
        val hubNewState = getReducer().reduceAction(action, hubOldState)
        return if (hubOldState === hubNewState) {
            domainState
        } else {
            updateStoreState(domainState, hubNewState)
        }
    }

    protected fun cancelAll() {
        actionsAndJobs.forEach { (_, job) -> job.cancel() }
    }

    protected abstract suspend fun handleAction(action: Action, storeState: DomainState, cancel: ValueCallback<Action>)
    protected abstract fun getReducer(): ReStoreReducer<State>

    protected abstract fun getHubState(storeState: DomainState): State
    protected abstract fun updateStoreState(storeState: DomainState, newHubState: State): DomainState
}
