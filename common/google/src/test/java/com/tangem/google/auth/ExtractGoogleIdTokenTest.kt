package com.tangem.google.auth

import android.os.Bundle
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class ExtractGoogleIdTokenTest {

    @Test
    fun `GIVEN credential of another type WHEN extracted THEN NoCredential`() {
        // Arrange
        val credential = CustomCredential(type = "com.example.OTHER_CREDENTIAL", data = Bundle())

        // Act
        val actual = extractGoogleIdToken(credential)

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(GoogleIdTokenError.NoCredential)
    }

    @Test
    fun `GIVEN google credential without token data WHEN extracted THEN Unknown`() {
        // Arrange
        val credential = CustomCredential(
            type = GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
            data = Bundle(),
        )

        // Act
        val actual = extractGoogleIdToken(credential)

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(GoogleIdTokenError.Unknown::class.java)
    }
}