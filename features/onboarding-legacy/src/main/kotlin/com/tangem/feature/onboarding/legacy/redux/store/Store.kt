package com.tangem.feature.onboarding.legacy.redux.store

import com.tangem.common.routing.AppRouter
import com.tangem.feature.onboarding.legacy.redux.DaggerGraphState
import com.tangem.feature.onboarding.legacy.redux.OnboardingGlobalAction
import com.tangem.feature.onboarding.legacy.redux.OnboardingReduxState
import com.tangem.feature.onboarding.legacy.redux.inject
import com.tangem.utils.coroutines.FeatureCoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.rekotlin.Store
import kotlin.coroutines.CoroutineContext

private val coroutineContext: CoroutineContext
    get() = Job() + Dispatchers.IO + FeatureCoroutineExceptionHandler.create("scope")
internal val scope = CoroutineScope(coroutineContext)

private val mainCoroutineContext: CoroutineContext
    get() = Job() + Dispatchers.Main + FeatureCoroutineExceptionHandler.create("mainScope")
internal val mainScope = CoroutineScope(mainCoroutineContext)

internal lateinit var store: Store<OnboardingReduxState>

internal fun Store<OnboardingReduxState>.dispatchLegacy(action: OnboardingGlobalAction) {
    dispatch(action)
    inject(DaggerGraphState::bridges).globalActionBridge.dispatch(action)
}

internal fun Store<OnboardingReduxState>.dispatchNavigationAction(action: AppRouter.() -> Unit) {
    inject(DaggerGraphState::appRouter).action()
}