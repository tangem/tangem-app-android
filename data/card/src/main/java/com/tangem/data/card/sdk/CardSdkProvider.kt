package com.tangem.data.card.sdk

import com.tangem.TangemSdk

/**
 * CardSDK instance provider
 *
[REDACTED_AUTHOR]
 */
interface CardSdkProvider {

    /** CardSDK instance */
    val sdk: TangemSdk

    /**
     * CardSDK instance, or `null` when there is none registered — unlike [sdk], never tries to recreate it.
     *
     * Use it where the caller can do without the SDK, most of all while an activity is being destroyed: the owner
     * is unsubscribed on `ON_DESTROY` before the retained instances are cleared, so a teardown that reaches for
     * [sdk] there re-registers against a dead activity and throws.
     */
    val sdkOrNull: TangemSdk?
}