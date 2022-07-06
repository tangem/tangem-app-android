package com.tangem.domain.redux.state

import com.tangem.domain.features.BuildConfig
import com.tangem.domain.redux.DomainState
import org.rekotlin.Action
import timber.log.Timber

/**
 * Created by Anton Zhilenkov on 07/04/2022.
 * Use it only in debug mode!
 */
internal interface ActionStateLogger {
    fun log(reducedSates: List<Pair<Action, DomainState>>)
}

internal class ActionStateLoggerImpl : ActionStateLogger {

    val actionStateConvertersFactory = ActionStateConvertersFactory()

    override fun log(reducedSates: List<Pair<Action, DomainState>>) {
        if (!BuildConfig.LOG_ENABLED) return

        logStates(reducedSates)
    }

    private fun logStates(reducedSates: List<Pair<Action, DomainState>>) {
        reducedSates.forEach { (action, domainState) ->
            val messageToPrint = actionStateConvertersFactory.getConverter(action)
                ?.convert(action, domainState)
                ?: return@forEach

            Timber.d(messageToPrint)
        }
    }
}
