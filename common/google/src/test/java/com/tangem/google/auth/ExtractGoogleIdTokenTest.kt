package com.tangem.google.auth

import android.os.Bundle
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class ExtractGoogleIdTokenTest {

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `GIVEN a google credential WHEN extracted THEN the token and its account travel together`() {
        // Arrange — the account decides which hidden folder the backup lands in
        val googleCredential: GoogleIdTokenCredential = mockk {
            every { idToken } returns TOKEN
            every { id } returns ACCOUNT_NAME
        }
        mockkObject(GoogleIdTokenCredential.Companion)
        every { GoogleIdTokenCredential.createFrom(any()) } returns googleCredential
        val credential = CustomCredential(
            type = GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
            data = Bundle(),
        )

        // Act
        val actual = extractGoogleIdToken(credential)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(GoogleIdToken(value = TOKEN, accountName = ACCOUNT_NAME))
    }

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

    private companion object {
        const val TOKEN = "google-id-token"
        const val ACCOUNT_NAME = "user@gmail.com"
    }
}