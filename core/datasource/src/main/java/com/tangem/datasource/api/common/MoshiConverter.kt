package com.tangem.datasource.api.common

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.common.json.TangemSdkAdapter
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * @author Anton Zhilenkov on 02/04/2022
 */
// TODO("Remove after removing Redux")
@Deprecated("Provide by DI")
object MoshiConverter {

    val networkMoshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
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
