package com.tangem.tangem_sdk_new

import com.tangem.Message
import com.tangem.TangemSdkError

sealed class SessionViewDelegateState() {
    data class Error(val error: TangemSdkError) : SessionViewDelegateState()
    data class Success(val message: Message?) : SessionViewDelegateState()
    data class SecurityDelay(val ms: Int, val totalDurationSeconds: Int) : SessionViewDelegateState()
    data class Delay(val total: Int, val current: Int, val step: Int) : SessionViewDelegateState()
    data class Ready(val cardId: String?, val message: Message?) : SessionViewDelegateState()
    data class PinRequested(val message: String, val callback: (pin: String) -> Unit) : SessionViewDelegateState()
    data class PinChangeRequested(val message: String, val callback: (pin: String) -> Unit) : SessionViewDelegateState()
    object TagLost : SessionViewDelegateState()
    object TagConnected : SessionViewDelegateState()
    object WrongCard : SessionViewDelegateState()
}