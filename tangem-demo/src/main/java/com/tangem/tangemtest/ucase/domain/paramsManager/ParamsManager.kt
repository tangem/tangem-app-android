package com.tangem.tangemtest.ucase.domain.paramsManager

import com.tangem.CardManager
import com.tangem.common.tlv.TlvTag
import com.tangem.tasks.TaskEvent

typealias ActionResponse = TaskEvent<*>
typealias AffectedList = List<IncomingParameter>
typealias AffectedParamsCallback = (AffectedList) -> Unit
typealias ActionCallback = (ActionResponse, AffectedList) -> Unit

/**
[REDACTED_AUTHOR]
 */
interface ParamsManager {

    fun parameterChanged(tag: TlvTag, value: Any?, callback: AffectedParamsCallback? = null)
    fun getParams(): List<IncomingParameter>
    fun invokeMainAction(cardManager: CardManager, callback: ActionCallback)
    fun getActionByTag(tag: TlvTag, cardManager: CardManager): ((ActionCallback) -> Unit)?

}

class IncomingParameter(val tlvTag: TlvTag, var data: Any? = null)

fun List<IncomingParameter>.findParameter(tag: TlvTag): IncomingParameter? {
    return firstOrNull { it.tlvTag == tag }
}