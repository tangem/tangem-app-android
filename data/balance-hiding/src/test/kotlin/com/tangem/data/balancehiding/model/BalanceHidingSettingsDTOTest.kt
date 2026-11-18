package com.tangem.data.balancehiding.model

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BalanceHidingSettingsDTOTest {

    private val adapter = Moshi.Builder().build().adapter(BalanceHidingSettingsDTO::class.java)

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN stored settings WHEN read THEN they are kept`(model: ReadModel) {
        // Act
        val actual = adapter.fromJson(model.json)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN settings WHEN written THEN they keep the stored format`() {
        // Act
        val actual = adapter.toJson(SETTINGS)

        // Assert
        assertThat(actual).isEqualTo(STORED_JSON)
    }

    private fun provideTestModels() = listOf(
        ReadModel(json = STORED_JSON, expected = SETTINGS),
        ReadModel(json = STORED_BEFORE_TOAST_FLAG_JSON, expected = SETTINGS.copy(isUpdateFromToast = false)),
    )

    internal data class ReadModel(val json: String, val expected: BalanceHidingSettingsDTO)

    private companion object {
        const val STORED_JSON = "{\"isHidingEnabledInSettings\":true,\"isBalanceHidden\":true," +
            "\"isBalanceHidingNotificationEnabled\":false,\"isUpdateFromToast\":true}"
        const val STORED_BEFORE_TOAST_FLAG_JSON = "{\"isHidingEnabledInSettings\":true,\"isBalanceHidden\":true," +
            "\"isBalanceHidingNotificationEnabled\":false}"

        val SETTINGS = BalanceHidingSettingsDTO(
            isHidingEnabledInSettings = true,
            isBalanceHidden = true,
            isBalanceHidingNotificationEnabled = false,
            isUpdateFromToast = true,
        )
    }
}