package com.tangem.tap.features.send.redux.middlewares

import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.send.redux.AddressPayIdActionUi.ChangeAddressOrPayId
import com.tangem.tap.features.send.redux.AmountActionUi.CheckAmountToSend
import com.tangem.tap.features.send.redux.FeeAction.RequestFee
import org.rekotlin.Middleware

/**
[REDACTED_AUTHOR]
 */
val sendMiddleware: Middleware<AppState> = { dispatch, appState ->
    { nextDispatch ->
        { action ->
            when (action) {
                is ChangeAddressOrPayId -> AddressPayIdMiddleware().handle(action.data, appState(), dispatch)
                is CheckAmountToSend -> AmountMiddleware().handle(action.data, appState(), dispatch)
                is RequestFee -> RequestFeeMiddleware().handle(appState(), dispatch)
            }
            nextDispatch(action)
        }
    }
}