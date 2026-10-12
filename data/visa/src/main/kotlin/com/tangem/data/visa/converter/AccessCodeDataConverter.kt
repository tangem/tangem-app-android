package com.tangem.data.visa.converter

import com.tangem.core.remote.json.NetworkJson
import com.tangem.data.visa.model.AccessCodeData
import com.tangem.domain.visa.model.VisaAuthTokens
import com.tangem.utils.converter.Converter
import kotlinx.serialization.json.Json
import okio.ByteString.Companion.decodeBase64
import javax.inject.Inject
import javax.inject.Singleton

private const val JWT_PAYLOAD_INDEX = 1

@Singleton
internal class AccessCodeDataConverter @Inject constructor(
    @NetworkJson private val json: Json,
) : Converter<VisaAuthTokens, AccessCodeData> {

    override fun convert(value: VisaAuthTokens): AccessCodeData {
        val payloadBase64 = value.accessToken.split(".").getOrNull(JWT_PAYLOAD_INDEX)
        val payload = requireNotNull(payloadBase64?.decodeBase64()?.utf8()) { "Invalid access token" }
        return json.decodeFromString(AccessCodeData.serializer(), payload)
    }
}