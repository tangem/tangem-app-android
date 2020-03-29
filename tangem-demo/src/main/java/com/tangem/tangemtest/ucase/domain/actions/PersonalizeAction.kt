package com.tangem.tangemtest.ucase.domain.actions

import com.tangem.commands.personalization.CardConfig
import com.tangem.tangemtest._arch.structure.impl.EditTextItem
import com.tangem.tangemtest._arch.structure.impl.getData
import com.tangem.tangemtest.ucase.domain.paramsManager.ActionCallback
import com.tangem.tangemtest.ucase.variants.TlvId
import com.tangem.tasks.TaskEvent
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log

/**
[REDACTED_AUTHOR]
 */
class PersonalizeAction : BaseCardAction() {
    override fun executeMainAction(attrs: AttrForAction, callback: ActionCallback) {
        var cardId: String? = null
        ScanAction().executeMainAction(attrs) { response, affectedList ->
            when (response) {
                is TaskEvent.Event -> {
                    Log.d(this, "ScanAction:response: TaskEvent.Event")
                    val item = affectedList.firstOrNull { it.id == TlvId.CardId } as? EditTextItem
                            ?: return@executeMainAction
                    cardId = item.getData()
                    if (cardId == null) {
                        handleResponse(response, null, attrs, callback)
                        return@executeMainAction
                    }
                }
                is TaskEvent.Completion -> {
                    Log.d(this, "ScanAction:response: TaskEvent.Completion")
                    if (cardId == null) {
                        handleResponse(response, null, attrs, callback)
                        return@executeMainAction
                    }

                    val cardConfig = attrs.payload.remove(cardConfig) as? CardConfig
                            ?: throw IllegalArgumentException("CardConfig must be in the payloads of the ParamsManager")

                    attrs.cardManager.personalize(cardConfig, cardId!!) {
                        handleResponse(it, null, attrs, callback)
                    }
                }
            }
        }
    }

    companion object {
        val cardConfig = "cardConfig"
    }
}