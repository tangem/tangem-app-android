package com.tangem.tangemtest.card_use_cases.models.params.manager

import com.tangem.CardManager
import com.tangem.common.tlv.TlvTag
import com.tangem.tangemtest.card_use_cases.models.params.manager.modifiers.AfterActionModification
import com.tangem.tangemtest.card_use_cases.models.params.manager.modifiers.AfterScanModifier
import com.tangem.tangemtest.card_use_cases.models.params.manager.modifiers.ParamsChangeConsequence
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
        val hashes = findHashes()
        val cardId = ParamsManager.findParameter(TlvTag.CardId, attrs.paramsList)?.data
        attrs.cardManager.sign(hashes, stringOf(cardId)) { handleResponse(it, null, attrs, callback) }
    }

    override fun getActionByTag(tag: TlvTag, attrs: AttrForAction): ((ActionCallback) -> Unit)? {
        if (tag != TlvTag.CardId) return null
        val pCardId = ParamsManager.findParameter(tag, attrs.paramsList) ?: return null

        return if (pCardId.data == null) { callback -> ScanAction().executeMainAction(attrs, callback) } else null
    }

    private fun findHashes(): Array<ByteArray> {
        val hash1 = ByteArray(32) { 1 }
        val hash2 = ByteArray(32) { 2 }
        return arrayOf(hash1, hash2)
    }
}