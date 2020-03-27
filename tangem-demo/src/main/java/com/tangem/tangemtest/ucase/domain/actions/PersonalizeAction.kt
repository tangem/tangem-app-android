package com.tangem.tangemtest.ucase.domain.actions

import com.tangem.commands.personalization.CardConfig
import com.tangem.tangemtest.ucase.domain.paramsManager.ActionCallback
import com.tangem.tasks.ScanEvent
import com.tangem.tasks.TaskEvent

/**
[REDACTED_AUTHOR]
 */
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
                    val cardConfig = attrs.payload.remove(cardConfig) as? CardConfig
                            ?: throw IllegalArgumentException("CardConfig must be in the payloads of the ParamsManager")

                    attrs.cardManager.personalize(cardConfig, cardId) {
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

    companion object {
        val cardConfig = "cardConfig"
    }
}