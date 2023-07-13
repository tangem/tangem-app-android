package com.tangem.data.card.sdk

import com.tangem.TangemSdk

/**
 * CardSDK instance provider
 *
 * @author Andrew Khokhlov on 12/07/2023
 */
internal interface CardSdkProvider {

    /** CardSDK instance */
    val sdk: TangemSdk
}
