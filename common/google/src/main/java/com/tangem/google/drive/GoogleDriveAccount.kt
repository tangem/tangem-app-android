package com.tangem.google.drive

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Overrides the pinned account for the [GoogleDriveTokenProvider] calls of a coroutine, `null` leaving the choice
 * to Google. A background operation on a wallet's files thus stays in the wallet's account while a screen re-pins.
 */
class GoogleDriveAccount(val name: String?) : AbstractCoroutineContextElement(Key) {

    companion object Key : CoroutineContext.Key<GoogleDriveAccount>
}