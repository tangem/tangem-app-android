package com.tangem.tangemtest.card_use_cases.domain.params_manager

import com.tangem.tangemtest.card_use_cases.resources.ActionType
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