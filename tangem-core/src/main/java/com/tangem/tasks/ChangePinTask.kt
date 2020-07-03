package com.tangem.tasks

import com.tangem.CardSession
import com.tangem.CardSessionRunnable
import com.tangem.SessionEnvironment
import com.tangem.SessionViewDelegate
import com.tangem.commands.SetPinCommand
import com.tangem.commands.SetPinResponse
import com.tangem.common.CompletionResult
import com.tangem.common.PinCode
import com.tangem.common.extensions.calculateSha256
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

enum class PinType {
    Pin1,
    Pin2,
    Pin3,
    ;
}

class ChangePinTask(
        private val pinType: PinType,
        private val pin: ByteArray? = null
) : CardSessionRunnable<SetPinResponse> {
    override val performPreflightRead = true
    override val requiresPin2 = false

    override fun run(session: CardSession, callback: (result: CompletionResult<SetPinResponse>) -> Unit) {
        session.scope.launch {
            val pin = this@ChangePinTask.pin
                    ?: requestNewPin(pinType, session.viewDelegate).calculateSha256()
            runSetPin(pin, session, callback)
        }
    }

    private suspend fun runSetPin(pin: ByteArray, session: CardSession, callback: (result: CompletionResult<SetPinResponse>) -> Unit) {
        val pin1: ByteArray
        val pin2: ByteArray
        val pin3: ByteArray?

        if (session.environment.pin1 == null) {
            session.environment.pin1 = PinCode(requestPin(PinType.Pin1, session.viewDelegate))
        }
        if (session.environment.pin2 == null) {
            session.environment.pin2 = PinCode(requestPin(PinType.Pin2, session.viewDelegate))
        }

        when (pinType) {
            PinType.Pin1 -> {
                pin1 = pin
                pin2 = session.environment.pin2!!.value
                pin3 = null
            }
            PinType.Pin2 -> {
                pin1 = session.environment.pin1!!.value
                pin2 = pin
                pin3 = null
            }
            PinType.Pin3 -> {
                pin1 = session.environment.pin1!!.value
                pin2 = session.environment.pin2!!.value
                pin3 = pin
            }
        }
        val command = SetPinCommand(pin1, pin2, pin3)
        command.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> savePin(pin, session.environment)
                is CompletionResult.Failure -> callback(result)
            }
        }
    }

    private suspend fun requestPin(pinType: PinType, viewDelegate: SessionViewDelegate): String =
            suspendCancellableCoroutine { continuation ->
                viewDelegate.onPinRequested(pinType) { result ->
                    if (continuation.isActive) continuation.resume(result)
                }
            }

    private suspend fun requestNewPin(pinType: PinType, viewDelegate: SessionViewDelegate): String =
            suspendCancellableCoroutine { continuation ->
                viewDelegate.onPinChangeRequested(pinType) { result ->
                    if (continuation.isActive) continuation.resume(result)
                }
            }

    private fun savePin(pin: ByteArray, environment: SessionEnvironment) {
        when (pinType) {
            PinType.Pin1 -> environment.pin1 = PinCode(pin, false)
            PinType.Pin2 -> environment.pin2 = PinCode(pin, false)
            PinType.Pin3 -> {}
        }
    }


}