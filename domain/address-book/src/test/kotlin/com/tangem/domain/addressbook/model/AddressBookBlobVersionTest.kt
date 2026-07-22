package com.tangem.domain.addressbook.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AddressBookBlobVersionTest {

    @ParameterizedTest
    @MethodSource("provideVersions")
    fun `isVersionCompatible against CURRENT_VERSION`(model: VersionModel) {
        // CURRENT_VERSION is 1.0 — the cases below are written relative to it.
        assertThat(AddressBookBlob.isVersionCompatible(model.version)).isEqualTo(model.expectedCompatible)
    }

    @Test
    fun `blob property mirrors the version function`() {
        val newer = AddressBookBlob(
            version = "2.0",
            walletId = "w",
            updatedAt = "t",
            nonce = "n",
            ciphertext = "c",
            authTag = "a",
        )

        assertThat(newer.isVersionCompatible).isFalse()
    }

    internal data class VersionModel(val version: String, val expectedCompatible: Boolean)

    private fun provideVersions() = listOf(
        VersionModel(version = "1.0", expectedCompatible = true), // equal
        VersionModel(version = "0.9", expectedCompatible = true), // lower
        VersionModel(version = "0.5", expectedCompatible = true), // lower
        VersionModel(version = "1", expectedCompatible = true), // 1 == 1.0
        VersionModel(version = "1.1", expectedCompatible = false), // higher
        VersionModel(version = "2.0", expectedCompatible = false), // higher
        VersionModel(version = "1.10", expectedCompatible = false), // 1.10 == 1.1 as a number, higher than 1.0
        VersionModel(version = "", expectedCompatible = false), // not a number
        VersionModel(version = "1.0.0", expectedCompatible = false), // not a number
        VersionModel(version = "abc", expectedCompatible = false) // not a number
    )
}