package com.tangem.google.auth

import com.google.android.gms.common.api.CommonStatusCodes
import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GoogleAuthErrorMappingTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN GMS status code WHEN mapped THEN expected GoogleAuthError`(model: MappingModel) {
        // Act
        val actual = googleAuthErrorForStatusCode(model.statusCode)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    data class MappingModel(val statusCode: Int, val expected: GoogleAuthError?)

    private fun provideTestModels() = listOf(
        MappingModel(CommonStatusCodes.NETWORK_ERROR, GoogleAuthError.NetworkError),
        MappingModel(CommonStatusCodes.TIMEOUT, GoogleAuthError.NetworkError),
        // interaction required -> AuthRequired (not PermissionsMissing)
        MappingModel(CommonStatusCodes.SIGN_IN_REQUIRED, GoogleAuthError.AuthRequired),
        MappingModel(CommonStatusCodes.RESOLUTION_REQUIRED, GoogleAuthError.AuthRequired),
        MappingModel(CommonStatusCodes.INVALID_ACCOUNT, GoogleAuthError.PermissionsMissing),
        MappingModel(CommonStatusCodes.API_NOT_CONNECTED, GoogleAuthError.Unavailable),
        MappingModel(CommonStatusCodes.SERVICE_DISABLED, GoogleAuthError.Unavailable),
        MappingModel(CommonStatusCodes.SERVICE_VERSION_UPDATE_REQUIRED, GoogleAuthError.Unavailable),
        MappingModel(CommonStatusCodes.CANCELED, GoogleAuthError.AuthCanceled),
        // unrecognized codes fall through to null so the caller wraps them as Unknown(cause)
        MappingModel(CommonStatusCodes.INTERNAL_ERROR, null),
        MappingModel(CommonStatusCodes.DEVELOPER_ERROR, null),
    )
}