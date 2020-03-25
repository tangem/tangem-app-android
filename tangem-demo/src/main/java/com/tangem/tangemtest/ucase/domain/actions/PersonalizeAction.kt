package com.tangem.tangemtest.ucase.domain.actions

import com.tangem.commands.personalization.CardConfig
import com.tangem.tangemtest.AppTangemDemo
import com.tangem.tangemtest.extensions.init
import com.tangem.tangemtest.ucase.domain.paramsManager.ActionCallback
import com.tangem.tasks.ScanEvent
import com.tangem.tasks.TaskEvent

class PersonalizeAction : BaseCardAction() {
    override fun executeMainAction(attrs: AttrForAction, callback: ActionCallback) {
        ScanAction().executeMainAction(attrs) { response, b ->
            when (response) {
                is TaskEvent.Event -> {
                    val taskEvent = response as TaskEvent.Event
                    val cardId = handleDataEvent(taskEvent.data)
                    if (cardId == null) {
                        handleResponse(response, null, attrs, callback)
                        return@executeMainAction
                    }

                    attrs.cardManager.personalize(CardConfig.init(AppTangemDemo.appInstance), cardId) {
                        handleResponse(it, null, attrs, callback)
                    }
                }
                is TaskEvent.Completion -> {
                    handleResponse(response, null, attrs, callback)
                }
            }


        }
    }

    private fun handleDataEvent(event: Any?): String? {
        return when (event) {
            is ScanEvent.OnReadEvent -> {
                event.card.cardId
            }
            else -> null
        }

    }
}