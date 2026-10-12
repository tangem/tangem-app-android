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

    /** CardSDK instance, or `null` when none is registered — unlike [sdk], never re-registers to create one. */
    val sdkOrNull: TangemSdk?
}