package com.tangem.data.wallets.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.tangemTech.models.CardInfoBody
import com.tangem.datasource.api.tangemTech.models.WalletIdBody
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.mockk
import org.junit.Test

class WalletIdBodyConverterTest {

    @Test
    fun `GIVEN user wallet with cards WHEN convert THEN should return correct WalletIdBody`() {
        // GIVEN
        val walletId = UserWalletId("1234567890abcdef")
        val walletName = "Test Wallet"
        val userWallet = UserWallet.Cold(
            walletId = walletId,
            name = walletName,
            cardsInWallet = setOf("card1", "card2"),
            isMultiCurrency = true,
            hasBackupError = false,
            scanResponse = mockk(),
        )
        val publicKeys = mapOf(
            "card1" to "public_key_1",
            "card2" to "public_key_2",
        )

        // WHEN
        val result = WalletIdBodyConverter.convert(userWallet, publicKeys)

        // THEN
        assertThat(result).isEqualTo(
            WalletIdBody(
                walletId = walletId.stringValue,
                name = walletName,
                cards = listOf(
                    CardInfoBody(
                        cardId = "card1",
                        cardPublicKey = "public_key_1",
                    ),
                    CardInfoBody(
                        cardId = "card2",
                        cardPublicKey = "public_key_2",
                    ),
                ),
            ),
        )
    }

    @Test
    fun `GIVEN user wallet without cards WHEN convert THEN should return WalletIdBody with empty cards list`() {
        // GIVEN
        val walletId = UserWalletId("1234567890abcdef")
        val walletName = "Test Wallet"
        val userWallet = UserWallet.Cold(
            walletId = walletId,
            name = walletName,
            cardsInWallet = emptySet(),
            isMultiCurrency = true,
            hasBackupError = false,
            scanResponse = mockk(),
        )
        val publicKeys = emptyMap<String, String>()

        // WHEN
        val result = WalletIdBodyConverter.convert(userWallet, publicKeys)

        // THEN
        assertThat(result).isEqualTo(
            WalletIdBody(
                walletId = walletId.stringValue,
                name = walletName,
                cards = emptyList(),
            ),
        )
    }
}