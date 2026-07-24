package com.tangem.data.polymarket.signer

import android.util.Base64

/** URL-safe base64 with standard `=` padding — kept behind a port so the signer is JVM-testable (minSdk 24). */
internal interface Base64UrlCodec {

    fun decode(value: String): ByteArray

    fun encode(bytes: ByteArray): String
}

internal class AndroidBase64UrlCodec : Base64UrlCodec {

    override fun decode(value: String): ByteArray = Base64.decode(value, Base64.URL_SAFE)

    override fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP)
}