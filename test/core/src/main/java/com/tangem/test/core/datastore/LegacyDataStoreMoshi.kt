package com.tangem.test.core.datastore

import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Moshi as the Moshi DataStore serializer wrote files: the production number adapters (as strings via `toString`)
 * and nulls skipped. Lets format tests prove their expected JSON is what is already stored on devices.
 */
val legacyDataStoreMoshi: Moshi = Moshi.Builder()
    .add(NumberAdapters)
    .addLast(KotlinJsonAdapterFactory())
    .build()

private object NumberAdapters {

    @FromJson
    fun bigDecimalFromJson(value: String): BigDecimal = BigDecimal(value)

    @ToJson
    fun bigDecimalToJson(value: BigDecimal): String = value.toString()

    @FromJson
    fun bigIntegerFromJson(value: String): BigInteger = BigInteger(value)

    @ToJson
    fun bigIntegerToJson(value: BigInteger): String = value.toString()
}