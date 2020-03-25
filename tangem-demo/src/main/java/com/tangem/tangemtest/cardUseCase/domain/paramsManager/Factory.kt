package com.tangem.tangemtest.cardUseCase.domain.paramsManager

import com.tangem.tangemtest.cardUseCase.domain.paramsManager.managers.DepersonalizeParamsManager
import com.tangem.tangemtest.cardUseCase.domain.paramsManager.managers.PersonalizeParamsManager
import com.tangem.tangemtest.cardUseCase.domain.paramsManager.managers.ScanParamsManager
import com.tangem.tangemtest.cardUseCase.domain.paramsManager.managers.SignParamsManager
import com.tangem.tangemtest.cardUseCase.resources.ActionType
import ru.dev.gbixahue.eu4d.lib.kotlin.common.BaseTypedHolder

/**
[REDACTED_AUTHOR]
 */
class ParamsManagerFactory : BaseTypedHolder<ActionType, ParamsManager>() {

    companion object {
        fun createFactory(): ParamsManagerFactory {
            return ParamsManagerFactory().apply {
                register(ActionType.Scan, ScanParamsManager())
                register(ActionType.Sign, SignParamsManager())
                register(ActionType.Personalize, PersonalizeParamsManager())
                register(ActionType.Depersonalize, DepersonalizeParamsManager())
            }
        }
    }
}