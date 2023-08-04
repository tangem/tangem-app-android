package com.tangem.data.card.sdk

import com.tangem.TangemSdk

/**
 * CardSDK instance provider
 *
* [REDACTED_AUTHOR]
 */
internal interface CardSdkProvider {

    /** CardSDK instance */
    val sdk: TangemSdk
}
