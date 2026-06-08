package com.tangem.lib.auth.session.internal

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.auth.models.request.AuthenticationPayload
import com.tangem.datasource.api.auth.models.request.DeviceMetadata
import com.tangem.datasource.api.auth.models.request.RegisterPayload
import com.tangem.utils.info.AppInfoProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SignedRequestPayloadTest {

    private val appInfoProvider: AppInfoProvider = mockk {
        every { device } returns "Pixel 8"
        every { platform } returns "Android"
        every { osVersion } returns "14"
        every { appVersion } returns "5.40.0"
        every { language } returns "en-US"
        every { timezone } returns "Europe/Moscow"
    }

    private val signedRequestPayload = SignedRequestPayload(appInfoProvider)

    @Test
    fun `deviceMetadata wires AppInfoProvider fields, forces userAgent to null, lowercases platform`() {
        val metadata = signedRequestPayload.deviceMetadata

        // Backend contract is lowercase `android`/`ios` — verify normalization at the source.
        assertThat(metadata).isEqualTo(
            DeviceMetadata(
                deviceModel = "Pixel 8",
                os = "android",
                osVersion = "14",
                appVersion = "5.40.0",
                userAgent = null,
                locale = "en-US",
                timezone = "Europe/Moscow",
            ),
        )
    }

    @Test
    fun `canonicalize produces newline-separated representation in the documented field order`() {
        val metadata = DeviceMetadata(
            deviceModel = "Pixel 8",
            os = "Android",
            osVersion = "14",
            appVersion = "5.40.0",
            userAgent = null,
            locale = "en-US",
            timezone = "Europe/Moscow",
        )
        val payload = RegisterPayload(
            devicePublicKey = "pub",
            nonce = "nonce-1",
            attestationToken = "attestation",
            metadata = metadata,
        )

        val bytes = signedRequestPayload.canonicalize(payload)

        assertThat(bytes.toString(Charsets.UTF_8)).isEqualTo(
            """
            pub
            nonce-1
            attestation
            Pixel 8
            Android
            14
            5.40.0
            en-US
            Europe/Moscow
            """.trimIndent(),
        )
    }

    @Test
    fun `canonicalize replaces null fields with empty string`() {
        val metadata = DeviceMetadata(
            deviceModel = null,
            os = "Android",
            osVersion = null,
            appVersion = null,
            userAgent = null,
            locale = null,
            timezone = null,
        )
        val payload = RegisterPayload(
            devicePublicKey = "pub",
            nonce = "nonce-1",
            attestationToken = null,
            metadata = metadata,
        )

        val bytes = signedRequestPayload.canonicalize(payload)

        // 8 newlines separate 9 logical slots; all but `devicePublicKey`, `nonce`, and `os` are empty.
        assertThat(bytes.toString(Charsets.UTF_8)).isEqualTo("pub\nnonce-1\n\n\nAndroid\n\n\n\n")
    }

    @Test
    fun `canonicalize AuthenticationPayload and RegisterPayload with same fields produces same bytes`() {
        // Identical canonicalisation across the two DTOs is the whole point of the shared helper —
        // verify the overloads can't drift apart silently.
        val metadata = DeviceMetadata(
            deviceModel = "Pixel 8",
            os = "Android",
            osVersion = "14",
            appVersion = "5.40.0",
            userAgent = null,
            locale = "en-US",
            timezone = "Europe/Moscow",
        )
        val auth = AuthenticationPayload(
            devicePublicKey = "pub",
            nonce = "nonce-1",
            attestationToken = "attestation",
            metadata = metadata,
        )
        val register = RegisterPayload(
            devicePublicKey = "pub",
            nonce = "nonce-1",
            attestationToken = "attestation",
            metadata = metadata,
        )

        val authBytes = signedRequestPayload.canonicalize(auth)
        val registerBytes = signedRequestPayload.canonicalize(register)

        assertThat(authBytes).isEqualTo(registerBytes)
    }
}