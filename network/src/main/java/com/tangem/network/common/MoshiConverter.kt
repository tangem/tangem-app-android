package com.tangem.network.common

import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.common.json.TangemSdkAdapter
import retrofit2.Converter
import retrofit2.converter.moshi.MoshiConverterFactory
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
class MoshiConverter {

    companion object {
        fun createFactory(moshi: Moshi = defaultMoshi()): Converter.Factory = MoshiConverterFactory.create(moshi)

        fun defaultMoshi(): Moshi = Moshi.Builder()
            .add(BigDecimalAdapter)
            .add(KotlinJsonAdapterFactory())
            .add(TangemSdkAdapter.DerivationPathAdapter())
            .add(TangemSdkAdapter.DerivationNodeAdapter())
            .build()

        fun sdkMoshi(): Moshi = MoshiJsonConverter.INSTANCE.moshi
    }
}

object BigDecimalAdapter {
    @FromJson
    fun fromJson(string: String) = BigDecimal(string)

    @ToJson
    fun toJson(value: BigDecimal) = value.toString()
}