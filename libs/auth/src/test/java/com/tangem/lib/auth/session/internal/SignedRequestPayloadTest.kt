package com.tangem.lib.auth.session.internal

import com.google.common.truth.Truth.assertThat
import com.tangem.lib.auth.api.models.request.AuthenticationPayload
import com.tangem.lib.auth.api.models.request.DeviceMetadata
import com.tangem.lib.auth.api.models.request.RegisterPayload
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.info.AppInfoProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

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

    private val signedRequestPayload = SignedRequestPayload(appInfoProvider, buildType = "release")

    @Test
    fun `deviceMetadata wires AppInfoProvider fields, builds userAgent, lowercases platform`() {
        val metadata = signedRequestPayload.deviceMetadata

        // Backend contract is lowercase `android`/`ios` — verify normalization at the source.
        assertThat(metadata).isEqualTo(
            DeviceMetadata(
                deviceModel = "Pixel 8",
                os = "android",
                osVersion = "14",
                appVersion = "5.40.0",
                userAgent = "Tangem/5.40.0 (Pixel 8; Android 14)",
                locale = "en-US",
                timezone = "Europe/Moscow",
            ),
        )
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN build type WHEN deviceMetadata THEN appVersion carries the build type suffix`(model: AppVersionModel) {
        // Arrange
        val appInfoProvider: AppInfoProvider = mockk(relaxed = true) {
            every { appVersion } returns model.versionName
        }

        // Act
        val metadata = SignedRequestPayload(appInfoProvider, model.buildType).deviceMetadata

        // Assert
        assertThat(metadata.appVersion).isEqualTo(model.expected)
    }

    @Test
    fun `canonicalize produces colon-separated representation in the documented field order`() {
        val metadata = DeviceMetadata(
            deviceModel = "Pixel 8",
            os = "Android",
            osVersion = "14",
            appVersion = "5.40.0",
            userAgent = "Tangem/5.40.0 (Pixel 8; Android 14)",
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

        // Must match the backend's `:`-joined canonicalisation byte-for-byte.
        assertThat(bytes.toString(Charsets.UTF_8)).isEqualTo(
            "pub:nonce-1:attestation:Pixel 8:Android:14:5.40.0:Tangem/5.40.0 (Pixel 8; Android 14):en-US:Europe/Moscow",
        )
    }

    @Test
    fun `canonicalize replaces null attestationToken with empty string`() {
        val metadata = DeviceMetadata(
            deviceModel = "Pixel 8",
            os = "Android",
            osVersion = "14",
            appVersion = "5.40.0",
            userAgent = "Tangem/5.40.0 (Pixel 8; Android 14)",
            locale = "en-US",
            timezone = "Europe/Moscow",
        )
        val payload = RegisterPayload(
            devicePublicKey = "pub",
            nonce = "nonce-1",
            attestationToken = null,
            metadata = metadata,
        )

        val bytes = signedRequestPayload.canonicalize(payload)

        // The null attestationToken collapses to an empty segment between `nonce` and `deviceModel`.
        assertThat(bytes.toString(Charsets.UTF_8)).isEqualTo(
            "pub:nonce-1::Pixel 8:Android:14:5.40.0:Tangem/5.40.0 (Pixel 8; Android 14):en-US:Europe/Moscow",
        )
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
            userAgent = "Tangem/5.40.0 (Pixel 8; Android 14)",
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

    data class AppVersionModel(val buildType: String, val versionName: String, val expected: String)

    private fun provideTestModels() = listOf(
        AppVersionModel(buildType = "release", versionName = "5.40.0", expected = "5.40.0"),
        AppVersionModel(buildType = "debug", versionName = "5.40.0", expected = "5.40.0-debug"),
        AppVersionModel(buildType = "internal", versionName = "5.40.0-internal", expected = "5.40.0-internal"),
        AppVersionModel(buildType = "external", versionName = "5.40.0-external", expected = "5.40.0-external"),
        AppVersionModel(buildType = "mocked", versionName = "5.40.0-mocked", expected = "5.40.0-mocked"),
        AppVersionModel(buildType = "debug", versionName = "1.0.0-SNAPSHOT", expected = "1.0.0-SNAPSHOT-debug"),
    )
}