package com.tangem.tangemtest.ucase.domain.paramsManager.managers

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import com.tangem.CardManager
import com.tangem.tangemtest.commons.Store
import com.tangem.tangemtest.ucase.domain.actions.PersonalizeAction
import com.tangem.tangemtest.ucase.domain.paramsManager.ActionCallback
import com.tangem.tangemtest.ucase.variants.personalize.converter.PersonalizeConfigConverter
import com.tangem.tangemtest.ucase.variants.personalize.dto.PersonalizeConfig

/**
[REDACTED_AUTHOR]
 */
class PersonalizeItemsManager(
        private val store: Store<PersonalizeConfig>
) : BaseItemsManager(PersonalizeAction()) {

    private val converter = PersonalizeConfigConverter()

    init {
        val config = store.restore()
        setItems(converter.convert(config))
    }

    override fun invokeMainAction(cardManager: CardManager, callback: ActionCallback) {
        action.executeMainAction(this, getAttrsForAction(cardManager), callback)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        val config = converter.convert(itemList, PersonalizeConfig())
        store.save(config)
    }
}