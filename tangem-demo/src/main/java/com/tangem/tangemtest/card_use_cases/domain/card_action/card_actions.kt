package com.tangem.tangemtest.card_use_cases.domain.card_action

import com.tangem.CardManager
import com.tangem.common.tlv.TlvTag
import com.tangem.tangemtest.card_use_cases.domain.params_manager.ActionCallback
import com.tangem.tangemtest.card_use_cases.domain.params_manager.IncomingParameter
import com.tangem.tangemtest.card_use_cases.domain.params_manager.findParameter
import com.tangem.tangemtest.card_use_cases.domain.params_manager.modifiers.AfterActionModification
import com.tangem.tangemtest.card_use_cases.domain.params_manager.modifiers.AfterScanModifier
import com.tangem.tangemtest.card_use_cases.domain.params_manager.modifiers.ParamsChangeConsequence
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
        TODO("Not yet implemented")
    }
}