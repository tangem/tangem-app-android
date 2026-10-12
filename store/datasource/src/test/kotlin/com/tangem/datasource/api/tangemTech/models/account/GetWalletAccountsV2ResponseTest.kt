package com.tangem.datasource.api.tangemTech.models.account

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class GetWalletAccountsV2ResponseTest {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun `GIVEN api v2 json WHEN parse and map THEN crypto counter lands in totalAccounts`() {
        // Arrange
        @Language("JSON")
        val json = """
            {
              "wallet": {
                "version": 3,
                "group": "none",
                "sort": "manual",
                "totalCryptoAccounts": 2,
                "totalArchivedAccounts": 1,
                "totalJointAccounts": 1
              },
              "accounts": [
                { "id": "A1", "name": null, "derivation": 0, "icon": "Star", "iconColor": "Azure", "type": "crypto" },
                { "id": "A2", "name": "Family", "derivation": 0, "icon": "Family", "iconColor": "Azure", "type": "joint" }
              ],
              "unassignedTokens": []
            }
        """.trimIndent()

        // Act
        val actual = moshi.adapter(GetWalletAccountsV2Response::class.java).fromJson(json)!!.toResponse()

        // Assert
        val expected = GetWalletAccountsResponse(
            wallet = GetWalletAccountsResponse.Wallet(
                version = 3,
                group = UserTokensResponse.GroupType.NONE,
                sort = UserTokensResponse.SortType.MANUAL,
                totalAccounts = 2,
                totalArchivedAccounts = 1,
                totalJointAccounts = 1,
            ),
            accounts = listOf(
                WalletAccountDTO(
                    id = "A1",
                    name = null,
                    derivationIndex = 0,
                    icon = "Star",
                    iconColor = "Azure",
                    type = WalletAccountDTO.Type.CRYPTO,
                ),
                WalletAccountDTO(
                    id = "A2",
                    name = "Family",
                    derivationIndex = 0,
                    icon = "Family",
                    iconColor = "Azure",
                    type = WalletAccountDTO.Type.JOINT,
                ),
            ),
            unassignedTokens = emptyList(),
        )
        assertThat(actual).isEqualTo(expected)
    }
}