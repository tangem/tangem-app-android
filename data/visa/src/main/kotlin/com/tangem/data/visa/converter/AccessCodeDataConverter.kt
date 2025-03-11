package com.tangem.data.visa.converter

import com.squareup.moshi.Moshi
import com.tangem.data.visa.model.AccessCodeData
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.domain.visa.model.VisaAuthTokens
import com.tangem.utils.converter.Converter
import okio.ByteString.Companion.decodeBase64
import javax.inject.Inject
import javax.inject.Singleton

private const val JWT_PAYLOAD_INDEX = 1

@Singleton
internal class AccessCodeDataConverter @Inject constructor(
    @NetworkMoshi private val moshi: Moshi,
) : Converter<VisaAuthTokens, AccessCodeData> {

    private val adapter = moshi.adapter(AccessCodeData::class.java)

    override fun convert(value: VisaAuthTokens): AccessCodeData {
        val payloadBase64 = value.accessToken.split(".").getOrNull(JWT_PAYLOAD_INDEX)
        val decodedString = payloadBase64?.decodeBase64()?.utf8()
        val data = decodedString?.let { adapter.fromJson(it) }
        requireNotNull(data) { "Invalid access token" }
        return data
    }
}