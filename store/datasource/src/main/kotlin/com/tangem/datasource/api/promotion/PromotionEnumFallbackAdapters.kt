package com.tangem.datasource.api.promotion

import com.squareup.moshi.Moshi
import com.tangem.core.remote.moshi.UnknownEnumMoshiAdapter
import com.tangem.datasource.api.promotion.models.PromotionsV3Response.BlockType
import com.tangem.datasource.api.promotion.models.PromotionsV3Response.MatchMode
import com.tangem.datasource.api.promotion.models.PromotionsV3Response.Status
import com.tangem.datasource.api.promotion.models.PromotionsV3Response.ValueType

/** Campaigns are configured on the backend without an app release, so new enum values must not fail parsing. */
fun Moshi.Builder.addPromotionEnumFallbackAdapters(): Moshi.Builder {
    val map = mapOf(
        Status::class.java to Status.UNKNOWN,
        MatchMode::class.java to MatchMode.UNKNOWN,
        BlockType::class.java to BlockType.UNKNOWN,
        ValueType::class.java to ValueType.UNKNOWN,
    )

    return apply {
        map.forEach { (enumClass, unknownValue) ->
            add(enumClass, UnknownEnumMoshiAdapter.create(enumClass, unknownValue))
        }
    }
}