package com.tangem.tap.domain.userWalletList.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.moshi.PersistedModelGuard
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.tap.domain.userWalletList.di.UserWalletsListRepositoryModule
import com.tangem.tap.domain.userWalletList.model.UserWalletPublicInformation
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class HotWalletIdMoshiAdapterTest {

    private val moshi = UserWalletsListRepositoryModule.buildMoshi(
        persistedModelGuard = PersistedModelGuard(isStrict = true),
    )
    private val adapter = moshi.adapter(HotWalletId::class.java)
    private val publicInformationAdapter = moshi.adapter(UserWalletPublicInformation::class.java)

    private val hotWalletInformation = UserWalletPublicInformation(
        name = "Wallet",
        walletId = UserWalletId(stringValue = "AB"),
        cardsInWallet = emptySet(),
        scanResponse = null,
        isMultiCurrency = true,
        hotWalletId = HotWalletId(value = "hot-wallet-id", authType = HotWalletId.AuthType.Password),
        backedUp = true,
    )
    private val walletInformationWithoutHotWalletId = hotWalletInformation.copy(hotWalletId = null, backedUp = null)

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN stored hot wallet id WHEN read THEN it is kept`(model: FormatModel) {
        // Act
        val actual = adapter.fromJson(model.json)

        // Assert
        assertThat(actual).isEqualTo(model.hotWalletId)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN hot wallet id WHEN written THEN it keeps the stored format`(model: FormatModel) {
        // Act
        val actual = adapter.toJson(model.hotWalletId)

        // Assert
        assertThat(actual).isEqualTo(model.json)
    }

    @Test
    fun `GIVEN stored hot wallet information WHEN read THEN hot wallet id is kept`() {
        // Act
        val actual = publicInformationAdapter.fromJson(STORED_HOT_WALLET_INFORMATION)

        // Assert
        assertThat(actual).isEqualTo(hotWalletInformation)
    }

    @Test
    fun `GIVEN hot wallet information WHEN written THEN it keeps the stored format`() {
        // Act
        val actual = publicInformationAdapter.toJson(hotWalletInformation)

        // Assert
        assertThat(actual).isEqualTo(STORED_HOT_WALLET_INFORMATION)
    }

    @Test
    fun `GIVEN stored wallet information without hot wallet id WHEN read THEN hot wallet id is null`() {
        // Act
        val actual = publicInformationAdapter.fromJson(STORED_WALLET_INFORMATION_WITHOUT_HOT_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(walletInformationWithoutHotWalletId)
    }

    @Test
    fun `GIVEN wallet information without hot wallet id WHEN written THEN hot wallet id is left out`() {
        // Act
        val actual = publicInformationAdapter.toJson(walletInformationWithoutHotWalletId)

        // Assert
        assertThat(actual).isEqualTo(STORED_WALLET_INFORMATION_WITHOUT_HOT_WALLET_ID)
    }

    private fun provideTestModels() = listOf(
        FormatModel(authType = HotWalletId.AuthType.NoPassword, storedAuthType = "NoPassword"),
        FormatModel(authType = HotWalletId.AuthType.Password, storedAuthType = "Password"),
        FormatModel(authType = HotWalletId.AuthType.Biometry, storedAuthType = "Biometry"),
    )

    internal data class FormatModel(val authType: HotWalletId.AuthType, val storedAuthType: String) {
        val json = """{"value":"hot-wallet-id","authType":"$storedAuthType"}"""
        val hotWalletId = HotWalletId(value = "hot-wallet-id", authType = authType)
    }

    private companion object {
        const val STORED_WALLET_INFORMATION_WITHOUT_HOT_WALLET_ID = """{"name":"Wallet",""" +
            """"walletId":{"stringValue":"AB"},"cardsInWallet":[],"isMultiCurrency":true,"hasBackupError":false}"""
        const val STORED_HOT_WALLET_INFORMATION = """{"name":"Wallet","walletId":{"stringValue":"AB"},""" +
            """"cardsInWallet":[],"isMultiCurrency":true,"hasBackupError":false,""" +
            """"hotWalletId":{"value":"hot-wallet-id","authType":"Password"},"backedUp":true}"""
    }
}