package com.tangem.datasource.api.tangemTech.models

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.SaveWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class WalletAccountsDtoTest {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun `GIVEN v12 response json WHEN parse THEN type and joint counter present`() {
        // Arrange
        @Language("JSON")
        val json = """
            {
              "wallet": {
                "version": 3,
                "group": "none",
                "sort": "manual",
                "totalAccounts": 2,
                "totalArchivedAccounts": 0,
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
        val actual = moshi.adapter(GetWalletAccountsResponse::class.java).fromJson(json)

        // Assert
        assertThat(actual?.wallet?.totalJointAccounts).isEqualTo(1)
        assertThat(actual?.accounts?.map(WalletAccountDTO::type)).containsExactly("crypto", "joint").inOrder()
    }

    @Test
    fun `GIVEN pre v12 response json WHEN parse THEN type and joint counter default to null`() {
        // Arrange
        @Language("JSON")
        val json = """
            {
              "wallet": {
                "version": 3,
                "group": "none",
                "sort": "manual",
                "totalAccounts": 1,
                "totalArchivedAccounts": 0
              },
              "accounts": [
                { "id": "A1", "name": null, "derivation": 0, "icon": "Star", "iconColor": "Azure" }
              ],
              "unassignedTokens": []
            }
        """.trimIndent()

        // Act
        val actual = moshi.adapter(GetWalletAccountsResponse::class.java).fromJson(json)

        // Assert
        assertThat(actual?.wallet?.totalJointAccounts).isNull()
        assertThat(actual?.accounts?.single()?.type).isNull()
    }

    @Test
    fun `GIVEN put body from get rows WHEN serialize THEN type explicit and absent one degrades to crypto`() {
        // Arrange
        val body = SaveWalletAccountsResponse(
            accounts = listOf(
                WalletAccountDTO(
                    id = "A1",
                    name = null,
                    derivationIndex = 0,
                    icon = "Star",
                    iconColor = "Azure",
                ),
                WalletAccountDTO(
                    id = "A2",
                    name = "Family",
                    derivationIndex = 0,
                    icon = "Family",
                    iconColor = "Azure",
                    type = "joint",
                ),
            ),
        )

        // Act
        val json = moshi.adapter(SaveWalletAccountsResponse::class.java).toJson(body)

        // Assert
        assertThat(json).contains("\"type\":\"crypto\"")
        assertThat(json).contains("\"type\":\"joint\"")
    }
}