package com.tangem.tangemtest.ucase.domain.actions

import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest.ucase.domain.paramsManager.ActionCallback
import com.tangem.tangemtest.ucase.domain.paramsManager.findDataParameter
import com.tangem.tangemtest.ucase.variants.TlvId
import ru.dev.gbixahue.eu4d.lib.kotlin.stringOf

/**
[REDACTED_AUTHOR]
 */
class SignAction : BaseCardAction() {
    override fun executeMainAction(attrs: AttrForAction, callback: ActionCallback) {
        val dataForHashing = attrs.paramsList.findDataParameter(TlvId.TransactionOutHash) ?: return

        val arHashes = createHashes(stringOf(dataForHashing.viewModel.data))
        val cardId = attrs.paramsList.findDataParameter(TlvId.CardId)?.viewModel?.data ?: return

        attrs.cardManager.sign(arHashes, stringOf(cardId)) { handleResponse(it, null, attrs, callback) }
    }

    override fun getActionByTag(id: Id, attrs: AttrForAction): ((ActionCallback) -> Unit)? {
        return when (id) {
            TlvId.CardId -> { callback -> ScanAction().executeMainAction(attrs, callback) }
            else -> null
        }
    }

    private fun createHashes(value: String): Array<ByteArray> {
        return arrayOf(value.toByteArray())
    }
}