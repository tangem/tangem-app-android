package com.tangem.core.decompose.ui

/**
 * Interface for sending messages to UI.
 *
 * @see UiMessage
 * @see UiMessageHandler
 * */
interface UiMessageSender {

    /**
     * Sends the given UI message.
     *
     * @param message The UI message to send.
     * */
    fun send(message: UiMessage)
}