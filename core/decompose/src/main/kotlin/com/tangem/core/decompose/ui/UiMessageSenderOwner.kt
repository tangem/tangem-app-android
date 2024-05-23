package com.tangem.core.decompose.ui

/**
 * Interface for owning a [UiMessageSender].
 * */
interface UiMessageSenderOwner {

    /**
     * The [UiMessageSender] instance.
     * */
    val messageSender: UiMessageSender
}