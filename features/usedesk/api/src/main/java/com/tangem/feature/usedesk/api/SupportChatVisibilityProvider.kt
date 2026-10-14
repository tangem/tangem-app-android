package com.tangem.feature.usedesk.api

interface SupportChatVisibilityProvider {

    /** `true` while at least one Usedesk chat screen is resumed. */
    val isChatVisible: Boolean
}