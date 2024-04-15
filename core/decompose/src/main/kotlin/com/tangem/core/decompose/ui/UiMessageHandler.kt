package com.tangem.core.decompose.ui

/**
 * Interface for handling UI messages.
 *
 * @see UiMessage
 * @see UiMessageSender
 */
interface UiMessageHandler {

    /**
     * Handles the given UI message.
     *
     * @param message The UI message to handle.
     */
    fun handleMessage(message: UiMessage)

    /**
     * Handles the removal of a UI message.
     *
     * @param key The key of the UI message to remove.
     */
    fun handleMessageRemove(key: String)
}
