package com.tangem.domain.jointaccount.model

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointAccountSignInputTest {

    @ParameterizedTest
    @ProvideTestModels
    fun create(model: TestModel) {
        // Act
        val exception = runCatching {
            JointAccountSignInput(
                derivationIndex = model.derivationIndex,
                makePayload = { ownerAddress ->
                    JointAccountCreationPayload(
                        config = JointAccountConfig(
                            name = "Family",
                            icon = "Family",
                            iconColor = "Azure",
                            membersCount = 3,
                            threshold = 2,
                        ),
                        creator = JointAccountParticipant(
                            walletId = "4B2F1C8A9E7D6053A1B4C7E2F8D9A0B3C5E7F1A2D4B6C8E0F2A4B6C8D0E2F4A6",
                            name = "Alice",
                            address = ownerAddress,
                            derivation = model.derivationIndex,
                        ),
                    )
                },
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
        TestModel(derivationIndex = 0, isValid = true),
        TestModel(derivationIndex = 19, isValid = true),
        TestModel(derivationIndex = Int.MAX_VALUE, isValid = true),
        TestModel(derivationIndex = -1, isValid = false),
        TestModel(derivationIndex = Int.MIN_VALUE, isValid = false),
    )

    data class TestModel(val derivationIndex: Int, val isValid: Boolean)
}