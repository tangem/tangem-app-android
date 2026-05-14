package com.tangem.common.ui.extensions

import androidx.annotation.DrawableRes
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network

/**
 * Retrieves the active icon drawable resource for the network of a [CryptoCurrency].
 */
@get:DrawableRes
val CryptoCurrency.networkIconResId: Int
    get() = network.iconResId

/**
 * Retrieves the greyed-out icon drawable resource for the network of a [CryptoCurrency].
 */
@get:DrawableRes
val CryptoCurrency.networkGreyedOutIconResId: Int
    get() = network.greyedOutIconResId

/**
 * Retrieves the active icon drawable resource for this [Network].
 */
@get:DrawableRes
val Network.iconResId: Int
    get() = id.iconResId

/**
 * Retrieves the greyed-out icon drawable resource for this [Network].
 */
@get:DrawableRes
val Network.greyedOutIconResId: Int
    get() = id.greyedOutIconResId

/**
 * Retrieves the active icon drawable resource for this [Network.ID].
 */
@get:DrawableRes
val Network.ID.iconResId: Int
    get() = rawId.iconResId

/**
 * Retrieves the greyed-out icon drawable resource for this [Network.ID].
 */
@get:DrawableRes
val Network.ID.greyedOutIconResId: Int
    get() = rawId.greyedOutIconResId

/**
 * Retrieves the active icon drawable resource for this [Network.RawID].
 */
@get:DrawableRes
val Network.RawID.iconResId: Int
    get() = getActiveIconRes(toBlockchain())

/**
 * Retrieves the greyed-out icon drawable resource for this [Network.RawID].
 */
@get:DrawableRes
val Network.RawID.greyedOutIconResId: Int
    get() = getGreyedOutIconRes(toBlockchain())