package com.tangem.domain.jointaccount.model

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointAccountCreationSignInputTest {

    @ParameterizedTest
    @ProvideTestModels
    fun create(model: TestModel) {
        // Act
        val exception = runCatching {
            JointAccountCreationSignInput(
                walletId = "4B2F1C8A9E7D6053A1B4C7E2F8D9A0B3C5E7F1A2D4B6C8E0F2A4B6C8D0E2F4A6",
                creatorName = "Alice",
                config = JointAccountCreationPayload.Config(
                    name = "Family",
                    icon = "Family",
                    iconColor = "Azure",
                    membersCount = 3,
                    threshold = 2,
                ),
                firstCandidateIndex = model.firstCandidateIndex,
                occupiedOwnerAddresses = emptySet(),
                maxIndexAttempts = model.maxIndexAttempts,
            )
        }.exceptionOrNull()

        // Assert
        if (model.isValid) {
            assertThat(exception).isNull()
        } else {
            assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    private fun provideTestModels() = listOf(
        TestModel(firstCandidateIndex = 0, maxIndexAttempts = 20, isValid = true),
        TestModel(firstCandidateIndex = 19, maxIndexAttempts = 1, isValid = true),
        TestModel(firstCandidateIndex = -1, maxIndexAttempts = 20, isValid = false),
        TestModel(firstCandidateIndex = 0, maxIndexAttempts = 0, isValid = false),
        TestModel(firstCandidateIndex = 0, maxIndexAttempts = -5, isValid = false),
        // The candidate range must not overflow Int
        TestModel(firstCandidateIndex = Int.MAX_VALUE - 5, maxIndexAttempts = 20, isValid = false),
        TestModel(firstCandidateIndex = Int.MAX_VALUE - 20, maxIndexAttempts = 20, isValid = true),
    )

    data class TestModel(val firstCandidateIndex: Int, val maxIndexAttempts: Int, val isValid: Boolean)
}