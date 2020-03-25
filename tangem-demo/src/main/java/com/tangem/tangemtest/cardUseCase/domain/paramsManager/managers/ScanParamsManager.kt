package com.tangem.tangemtest.cardUseCase.domain.paramsManager.managers

import com.tangem.CardManager
import com.tangem.common.tlv.TlvTag
import com.tangem.tangemtest.cardUseCase.domain.actions.ScanAction
import com.tangem.tangemtest.cardUseCase.domain.paramsManager.ActionCallback
import com.tangem.tangemtest.cardUseCase.domain.paramsManager.BaseParamsManager
import com.tangem.tangemtest.cardUseCase.domain.paramsManager.IncomingParameter

class ScanParamsManager : BaseParamsManager(ScanAction()) {
    override fun createParamsList(): List<IncomingParameter> {
        return listOf(IncomingParameter(TlvTag.CardId, null))
    }

    override fun invokeMainAction(cardManager: CardManager, callback: ActionCallback) {
        action.executeMainAction(getAttrsForAction(cardManager), callback)
    }
}