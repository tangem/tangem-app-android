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

    /**
     * Removes the UI message from UI with the given key.
     *
     * @param key The key of the UI message to remove.
     * */
    fun remove(key: String)
}
