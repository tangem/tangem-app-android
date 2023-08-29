package com.tangem.core.ui.extensions

import androidx.annotation.DrawableRes
import com.tangem.core.ui.R
import com.tangem.domain.tokens.models.CryptoCurrency

/**
 * Retrieves the resource ID for the network badge of a [CryptoCurrency].
 *
 * This property provides a way to fetch the appropriate drawable resource ID
 * for the network badge of a given cryptocurrency. For coins, this will typically
 * return null as they do not have network badges, while tokens will fetch the icon
 * based on their associated network ID.
 *
 * @return Drawable resource ID for the network badge or null if the cryptocurrency is a coin.
 */
@get:DrawableRes
val CryptoCurrency.networkBadgeIconResId: Int?
    get() = when (this) {
        is CryptoCurrency.Coin -> null
        is CryptoCurrency.Token -> getActiveIconRes(network.id.value)
    }

/**
 * Retrieves the resource ID for the icon of a [CryptoCurrency].
 *
 * This property provides a way to fetch the appropriate drawable resource ID
 * for the icon of a given cryptocurrency.
 *
 * @return Drawable resource ID for the cryptocurrency icon.
 */
@get:DrawableRes
val CryptoCurrency.iconResId: Int
    get() = when (this) {
        is CryptoCurrency.Coin -> {
            val rawCoinId = id.rawCurrencyId

            if (rawCoinId != null) {
                getActiveIconResByCoinId(rawCoinId, network.id.value)
            } else {
                R.drawable.ic_alert_24
            }
        }
        is CryptoCurrency.Token -> R.drawable.ic_alert_24
    }