package com.tangem.tangemtest.cardUseCase.domain.actions

import com.tangem.CardManager
import com.tangem.commands.personalization.CardConfig
import com.tangem.common.tlv.TlvTag
import com.tangem.tangemtest.AppTangemDemo
import com.tangem.tangemtest.cardUseCase.domain.paramsManager.ActionCallback
import com.tangem.tangemtest.cardUseCase.domain.paramsManager.IncomingParameter
import com.tangem.tangemtest.cardUseCase.domain.paramsManager.findParameter
import com.tangem.tangemtest.cardUseCase.domain.paramsManager.modifiers.AfterActionModification
import com.tangem.tangemtest.cardUseCase.domain.paramsManager.modifiers.AfterScanModifier
import com.tangem.tangemtest.cardUseCase.domain.paramsManager.modifiers.ParamsChangeConsequence
import com.tangem.tangemtest.extensions.init
import com.tangem.tasks.ScanEvent
import com.tangem.tasks.TaskEvent
import ru.dev.gbixahue.eu4d.lib.kotlin.stringOf

/**
[REDACTED_AUTHOR]
 *
 * The Card Action class family is designed for calling Card Manager functions
 * and then processing the response. It also allows you to extract the main action as a lambda expression
 */
data class AttrForAction(
        val cardManager: CardManager,
        val paramsList: List<IncomingParameter>,
        val consequence: ParamsChangeConsequence?
)

interface CardAction {
    fun executeMainAction(attrs: AttrForAction, callback: ActionCallback)
    fun getActionByTag(tag: TlvTag, attrs: AttrForAction): ((ActionCallback) -> Unit)? = null
}

abstract class BaseCardAction : CardAction {
    protected open fun handleResponse(
            taskEvent: TaskEvent<*>,
            modifier: AfterActionModification?,
            attrs: AttrForAction,
            callback: ActionCallback
    ) {
        val allModifiedParams = mutableListOf<IncomingParameter>()
        modifier?.modify(taskEvent, attrs.paramsList)?.forEach { parameter ->
            allModifiedParams.add(parameter)
            attrs.consequence?.affectChanges(parameter, attrs.paramsList)?.let { allModifiedParams.addAll(it) }
        }
        callback(taskEvent, allModifiedParams)
    }
}

class ScanAction : BaseCardAction() {
    override fun executeMainAction(attrs: AttrForAction, callback: ActionCallback) {
        attrs.cardManager.scanCard { handleResponse(it, AfterScanModifier(), attrs, callback) }
    }
}

class SignAction : BaseCardAction() {
    override fun executeMainAction(attrs: AttrForAction, callback: ActionCallback) {
        val dataForHashing = attrs.paramsList.findParameter(TlvTag.TransactionOutHash) ?: return

        val arHashes = createHashes(stringOf(dataForHashing.data))
        val cardId = attrs.paramsList.findParameter(TlvTag.CardId)?.data ?: return

        attrs.cardManager.sign(arHashes, stringOf(cardId)) { handleResponse(it, null, attrs, callback) }
    }

    override fun getActionByTag(tag: TlvTag, attrs: AttrForAction): ((ActionCallback) -> Unit)? {
        return when (tag) {
            TlvTag.CardId -> { callback -> ScanAction().executeMainAction(attrs, callback) }
            else -> null
        }
    }

    private fun createHashes(value: String): Array<ByteArray> {
        return arrayOf(value.toByteArray())
    }
}

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

class DepesonalizeAction : BaseCardAction() {
    override fun executeMainAction(attrs: AttrForAction, callback: ActionCallback) {
        val cardId = attrs.paramsList.findParameter(TlvTag.CardId)?.data as? String ?: return

        attrs.cardManager.depersonalize(cardId) { handleResponse(it, null, attrs, callback) }
    }

    override fun getActionByTag(tag: TlvTag, attrs: AttrForAction): ((ActionCallback) -> Unit)? {
        return when (tag) {
            TlvTag.CardId -> { callback -> ScanAction().executeMainAction(attrs, callback) }
            else -> null
        }
    }
}