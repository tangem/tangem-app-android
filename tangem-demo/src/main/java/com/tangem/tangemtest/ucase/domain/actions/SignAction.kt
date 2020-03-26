package com.tangem.tangemtest.ucase.domain.actions

import com.tangem.common.tlv.TlvTag
import com.tangem.tangemtest.ucase.domain.paramsManager.ActionCallback
import com.tangem.tangemtest.ucase.domain.paramsManager.findParameter
import ru.dev.gbixahue.eu4d.lib.kotlin.stringOf

/**
[REDACTED_AUTHOR]
 */
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