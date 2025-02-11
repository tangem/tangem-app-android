package com.tangem.datasource.api.common

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.common.json.TangemSdkAdapter
import com.tangem.datasource.api.common.adapter.BigDecimalAdapter
import retrofit2.converter.moshi.MoshiConverterFactory

/**
[REDACTED_AUTHOR]
 */
// TODO("Remove after removing Redux")
@Deprecated("Provide by DI")
object MoshiConverter {

    val networkMoshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .add(BigDecimalAdapter())
        .add(TangemSdkAdapter.ByteArrayAdapter())
        .build()

    val networkMoshiConverter: MoshiConverterFactory = MoshiConverterFactory.create(networkMoshi)

    val sdkMoshiConverter: MoshiJsonConverter = MoshiJsonConverter(
        adapters = listOf(BigDecimalAdapter()) + MoshiJsonConverter.getTangemSdkAdapters(),
        typedAdapters = MoshiJsonConverter.getTangemSdkTypedAdapters(),
    )

    val sdkMoshi: Moshi = sdkMoshiConverter.moshi
}