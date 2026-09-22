package com.tangem.spend.datasource.pay

import com.squareup.moshi.Moshi
import com.tangem.core.remote.moshi.UnknownEnumMoshiAdapter
import com.tangem.spend.datasource.pay.models.response.OrderResponse

/**
 * Registers the enum fallbacks of the TangemPay BFF responses.
 *
 * The backend keeps adding order statuses, and without a fallback a single unknown string makes the whole
 * response unparseable — for every order type at once.
 */
fun Moshi.Builder.addTangemPayEnumFallbackAdapters(): Moshi.Builder {
    val fallbacks = mapOf<Class<out Enum<*>>, Enum<*>>(
        OrderResponse.Result.Status::class.java to OrderResponse.Result.Status.UNKNOWN,
    )

    return apply {
        fallbacks.forEach { (enumClass, unknownValue) ->
            add(enumClass, UnknownEnumMoshiAdapter.create(enumClass, unknownValue))
        }
    }
}