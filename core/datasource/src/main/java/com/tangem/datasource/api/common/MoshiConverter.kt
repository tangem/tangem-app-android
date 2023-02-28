package com.tangem.datasource.api.common

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.common.json.MoshiJsonConverter
import retrofit2.converter.moshi.MoshiConverterFactory

/**
* [REDACTED_AUTHOR]
 */
// [REDACTED_TODO_COMMENT]
@Deprecated("Provide by DI")
object MoshiConverter {

    val networkMoshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .add(BigDecimalAdapter())
        .build()

    val networkMoshiConverter: MoshiConverterFactory = MoshiConverterFactory.create(networkMoshi)

    val sdkMoshiConverter: MoshiJsonConverter = MoshiJsonConverter(
        adapters = listOf(BigDecimalAdapter()) + MoshiJsonConverter.getTangemSdkAdapters(),
        typedAdapters = MoshiJsonConverter.getTangemSdkTypedAdapters(),
    )

    val sdkMoshi: Moshi = sdkMoshiConverter.moshi
}
