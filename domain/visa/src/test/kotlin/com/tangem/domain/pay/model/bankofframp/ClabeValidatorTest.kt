package com.tangem.domain.pay.model.bankofframp

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ClabeValidatorTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN CLABE WHEN isValid THEN only 18 digits with a matching check digit pass`(model: ClabeModel) {
        // Act
        val actual = ClabeValidator.isValid(model.clabe)

        // Assert
        assertThat(actual).isEqualTo(model.isValid)
    }

    internal data class ClabeModel(val description: String, val clabe: String, val isValid: Boolean)

    private fun provideTestModels() = listOf(
        ClabeModel(description = "published sample of bank 002", clabe = "002010077777777771", isValid = true),
        ClabeModel(description = "published sample of bank 032", clabe = "032180000118359719", isValid = true),
        ClabeModel(description = "computed CLABE with check digit 0", clabe = "003010077777777770", isValid = true),
        ClabeModel(description = "wrong check digit", clabe = "002010077777777770", isValid = false),
        ClabeModel(description = "another wrong check digit", clabe = "032180000118359718", isValid = false),
        ClabeModel(description = "17 digits", clabe = "03218000011835971", isValid = false),
        ClabeModel(description = "19 digits", clabe = "0321800001183597190", isValid = false),
        ClabeModel(description = "a letter", clabe = "03218000011835971A", isValid = false),
        ClabeModel(description = "grouped with spaces", clabe = "032 180 00011835971", isValid = false),
        ClabeModel(description = "empty", clabe = "", isValid = false),
    )
}