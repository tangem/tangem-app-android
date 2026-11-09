package com.tangem.tap.domain.userWalletList.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.moshi.PersistedModelGuard
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.tap.domain.userWalletList.di.UserWalletsListRepositoryModule
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class HotWalletIdDTOTest {

    private val adapter = UserWalletsListRepositoryModule
        .buildMoshi(persistedModelGuard = PersistedModelGuard(isStrict = true))
        .adapter(HotWalletIdDTO::class.java)

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN stored hot wallet id WHEN read THEN it is kept`(model: FormatModel) {
        // Act
        val actual = adapter.fromJson(model.json)

        // Assert
        assertThat(actual).isEqualTo(model.dto)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN hot wallet id WHEN written THEN it keeps the stored format`(model: FormatModel) {
        // Act
        val actual = adapter.toJson(model.dto)

        // Assert
        assertThat(actual).isEqualTo(model.json)
    }

    private fun provideTestModels() = listOf(
        FormatModel(authType = HotWalletId.AuthType.NoPassword, storedAuthType = "NoPassword"),
        FormatModel(authType = HotWalletId.AuthType.Password, storedAuthType = "Password"),
        FormatModel(authType = HotWalletId.AuthType.Biometry, storedAuthType = "Biometry"),
    )

    internal data class FormatModel(val authType: HotWalletId.AuthType, val storedAuthType: String) {
        val json = """{"value":"hot-wallet-id","authType":"$storedAuthType"}"""
        val dto = HotWalletIdDTO(value = "hot-wallet-id", authType = authType)
    }
}