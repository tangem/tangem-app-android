package com.tangem.common.routing.deeplink

/** What a `tangem://onramp` deeplink asks the app to do. */
sealed interface OnrampDeeplinkTarget {

    data class Transaction(val txId: String) : OnrampDeeplinkTarget

    data class Redirect(val isSuccess: Boolean) : OnrampDeeplinkTarget

    /** Opens the Waitlist survey */
    data object ConciergeWaitlist : OnrampDeeplinkTarget

    data object Invalid : OnrampDeeplinkTarget
}

/**
 * Resolves the query of a `tangem://onramp` deeplink. A transaction id wins over a redirect result, which wins over
 * the Waitlist type, so a link that carries several of them is handled once.
 */
fun resolveOnrampDeeplinkTarget(queryParams: Map<String, String>): OnrampDeeplinkTarget {
    val txId = queryParams[DeeplinkConst.TX_ID_KEY]
    val result = queryParams[DeeplinkConst.RESULT_KEY]

    return when {
        !txId.isNullOrEmpty() -> OnrampDeeplinkTarget.Transaction(txId)
        result == RESULT_SUCCESS -> OnrampDeeplinkTarget.Redirect(isSuccess = true)
        result == RESULT_CANCEL -> OnrampDeeplinkTarget.Redirect(isSuccess = false)
        queryParams[DeeplinkConst.TYPE_KEY] == CONCIERGE_WAITLIST_TYPE -> OnrampDeeplinkTarget.ConciergeWaitlist
        else -> OnrampDeeplinkTarget.Invalid
    }
}

private const val RESULT_SUCCESS = "success"
private const val RESULT_CANCEL = "cancel"
private const val CONCIERGE_WAITLIST_TYPE = "concierge_waitlist"