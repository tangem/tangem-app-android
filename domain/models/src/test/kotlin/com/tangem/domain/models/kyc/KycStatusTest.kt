package com.tangem.domain.models.kyc

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class KycStatusTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN backend status WHEN fromString THEN mapped to expected status`(model: FromStringModel) {
        // Act
        val actual = KycStatus.fromString(model.status)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        FromStringModel(status = "required", expected = KycStatus.REQUIRED),
        FromStringModel(status = "init", expected = KycStatus.INIT),
        FromStringModel(status = "in_progress", expected = KycStatus.PENDING),
        FromStringModel(status = "approved", expected = KycStatus.APPROVED),
        FromStringModel(status = "declined", expected = KycStatus.REJECTED),
        FromStringModel(status = "APPROVED", expected = KycStatus.APPROVED),
        FromStringModel(status = "something_new", expected = KycStatus.UNKNOWN),
        FromStringModel(status = null, expected = KycStatus.UNKNOWN),
    )

    data class FromStringModel(val status: String?, val expected: KycStatus)
}