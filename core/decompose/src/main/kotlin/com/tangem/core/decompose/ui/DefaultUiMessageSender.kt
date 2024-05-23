package com.tangem.core.decompose.ui

internal class DefaultUiMessageSender(
    private val handler: UiMessageHandler,
) : UiMessageSender {

    override fun send(message: UiMessage) {
        handler.handleMessage(message)
    }
}