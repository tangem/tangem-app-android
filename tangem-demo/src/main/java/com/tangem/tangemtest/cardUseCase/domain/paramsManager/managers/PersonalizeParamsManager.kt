package com.tangem.tangemtest.cardUseCase.domain.paramsManager.managers

import com.tangem.CardManager
import com.tangem.tangemtest.cardUseCase.domain.actions.PersonalizeAction
import com.tangem.tangemtest.cardUseCase.domain.paramsManager.ActionCallback
import com.tangem.tangemtest.cardUseCase.domain.paramsManager.BaseParamsManager
import com.tangem.tangemtest.cardUseCase.domain.paramsManager.IncomingParameter

class PersonalizeParamsManager : BaseParamsManager(PersonalizeAction()) {
    override fun createParamsList(): List<IncomingParameter> {
        return listOf()
    }

    override fun invokeMainAction(cardManager: CardManager, callback: ActionCallback) {
        action.executeMainAction(getAttrsForAction(cardManager), callback)
    }
}