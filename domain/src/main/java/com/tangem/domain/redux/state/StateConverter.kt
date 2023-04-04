package com.tangem.domain.redux.state

import com.tangem.domain.redux.DomainState
import org.rekotlin.Action

/**
 * Created by Anton Zhilenkov on 07/04/2022.
 */
interface StringStateConverter<StateHolder> {
    fun convert(stateHolder: StateHolder): String
}

interface StringActionStateConverter<StateHolder> {
    fun convert(action: Action, stateHolder: StateHolder): String?
}

class ActionStateConvertersFactory {
    private val stateConverters = mutableMapOf<Class<out Action>, StringActionStateConverter<DomainState>>()

    fun addConverter(classOfAction: Class<out Action>, converter: StringActionStateConverter<DomainState>) {
        stateConverters[classOfAction] = converter
    }

    fun getConverter(action: Action): StringActionStateConverter<DomainState>? {
        val converter = stateConverters.firstNotNullOfOrNull { (classOfAction, converter) ->
            if (classOfAction.isAssignableFrom(action::class.java)) {
                converter
            } else {
                null
            }
        } ?: return null

        return converter
    }
}
