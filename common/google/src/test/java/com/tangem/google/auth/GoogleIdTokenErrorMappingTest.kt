package com.tangem.google.auth

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GoogleIdTokenErrorMappingTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN credential exception WHEN mapped THEN expected GoogleIdTokenError`(model: MappingModel) {
        // Act
        val actual = googleIdTokenErrorForException(model.exception)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    data class MappingModel(val exception: GetCredentialException, val expected: GoogleIdTokenError)

    private fun provideTestModels() = listOf(
        MappingModel(GetCredentialCancellationException(), GoogleIdTokenError.Canceled),
        MappingModel(NoCredentialException(), GoogleIdTokenError.NoCredential),
        MappingModel(GetCredentialProviderConfigurationException(), GoogleIdTokenError.Unavailable),
        MappingModel(GetCredentialUnsupportedException(), GoogleIdTokenError.Unavailable),
        interruptedExceptionModel(),
        unknownExceptionModel(),
    )

    private fun interruptedExceptionModel(): MappingModel {
        val exception = GetCredentialInterruptedException()
        return MappingModel(exception, GoogleIdTokenError.Unknown(exception))
    }

    private fun unknownExceptionModel(): MappingModel {
        val exception = GetCredentialUnknownException()
        return MappingModel(exception, GoogleIdTokenError.Unknown(exception))
    }
}