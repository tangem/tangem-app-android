package com.tangem.datasource.api.jointaccount.models

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class JointAccountDtoTest {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun `GIVEN get response json WHEN parse THEN whole object matches contract`() {
        // Arrange
        @Language("JSON")
        val json = """
            {
              "jointAccounts": [
                {
                  "cryptoAccountId": "4B2F1C8A9E7D6053A1B4C7E2F8D9A0B3C5E7F1A2D4B6C8E0F2A4B6C8D0E2F4A6",
                  "membersCount": 3,
                  "threshold": 2,
                  "safeAddress": null,
                  "status": "pending",
                  "members": [
                    { "name": "Alice", "address": "0x7e5f4552091a69125d5DfCb7b8C2659029395Bdf", "role": "creator" },
                    { "name": "Bob", "address": "0x2B5AD5c4795c026514f8317c7a215E218DcCD6cF", "role": "member" }
                  ]
                }
              ]
            }
        """.trimIndent()

        // Act
        val actual = moshi.adapter(GetJointAccountsResponse::class.java).fromJson(json)

        // Assert
        val expected = GetJointAccountsResponse(
            jointAccounts = listOf(
                JointAccountDto(
                    cryptoAccountId = "4B2F1C8A9E7D6053A1B4C7E2F8D9A0B3C5E7F1A2D4B6C8E0F2A4B6C8D0E2F4A6",
                    membersCount = 3,
                    threshold = 2,
                    safeAddress = null,
                    status = "pending",
                    members = listOf(
                        JointAccountDto.Member(
                            name = "Alice",
                            address = "0x7e5f4552091a69125d5DfCb7b8C2659029395Bdf",
                            role = "creator",
                        ),
                        JointAccountDto.Member(
                            name = "Bob",
                            address = "0x2B5AD5c4795c026514f8317c7a215E218DcCD6cF",
                            role = "member",
                        ),
                    ),
                    invites = null,
                ),
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN create response json with invites WHEN parse THEN invites present`() {
        // Arrange
        @Language("JSON")
        val json = """
            {
              "cryptoAccountId": "4B2F1C8A9E7D6053A1B4C7E2F8D9A0B3C5E7F1A2D4B6C8E0F2A4B6C8D0E2F4A6",
              "membersCount": 3,
              "threshold": 2,
              "safeAddress": null,
              "status": "pending",
              "members": [
                { "name": "Alice", "address": "0x7e5f4552091a69125d5DfCb7b8C2659029395Bdf", "role": "creator" }
              ],
              "invites": [
                { "id": "AA11BB22CC33DD44EE55FF66AA77BB88CC99DD00EE11FF22AA33BB44CC55DD66" },
                { "id": "BB11BB22CC33DD44EE55FF66AA77BB88CC99DD00EE11FF22AA33BB44CC55DD66" }
              ]
            }
        """.trimIndent()

        // Act
        val actual = moshi.adapter(JointAccountDto::class.java).fromJson(json)

        // Assert
        assertThat(actual?.invites).containsExactly(
            JointAccountDto.Invite(id = "AA11BB22CC33DD44EE55FF66AA77BB88CC99DD00EE11FF22AA33BB44CC55DD66"),
            JointAccountDto.Invite(id = "BB11BB22CC33DD44EE55FF66AA77BB88CC99DD00EE11FF22AA33BB44CC55DD66"),
        ).inOrder()
    }

    @Test
    fun `GIVEN status and role unknown to this build WHEN parse THEN values pass through as strings`() {
        // Arrange
        @Language("JSON")
        val json = """
            {
              "cryptoAccountId": "4B2F1C8A9E7D6053A1B4C7E2F8D9A0B3C5E7F1A2D4B6C8E0F2A4B6C8D0E2F4A6",
              "membersCount": 3,
              "threshold": 2,
              "safeAddress": "0x7e5f4552091a69125d5DfCb7b8C2659029395Bdf",
              "status": "frozen",
              "members": [
                { "name": "Alice", "address": "0x7e5f4552091a69125d5DfCb7b8C2659029395Bdf", "role": "owner" }
              ]
            }
        """.trimIndent()

        // Act
        val actual = moshi.adapter(JointAccountDto::class.java).fromJson(json)

        // Assert
        assertThat(actual?.status).isEqualTo("frozen")
        assertThat(actual?.members?.single()?.role).isEqualTo("owner")
    }

    @Test
    fun `GIVEN create request WHEN round trip THEN object survives and field names match contract`() {
        // Arrange
        val request = CreateJointAccountRequest(
            payload = CreateJointAccountRequest.Payload(
                config = JointAccountConfigDto(
                    name = "Family",
                    icon = "Family",
                    iconColor = "Azure",
                    membersCount = 3,
                    threshold = 2,
                ),
                creator = JointAccountParticipantDto(
                    walletId = "4B2F1C8A9E7D6053A1B4C7E2F8D9A0B3C5E7F1A2D4B6C8E0F2A4B6C8D0E2F4A6",
                    name = " Alice ",
                    address = "0x7e5f4552091a69125d5DfCb7b8C2659029395Bdf",
                    derivation = 0,
                ),
            ),
            signature = "0x" + "ab".repeat(65),
        )
        val adapter = moshi.adapter(CreateJointAccountRequest::class.java)

        // Act
        val json = adapter.toJson(request)
        val actual = adapter.fromJson(json)

        // Assert
        assertThat(actual).isEqualTo(request)
        assertThat(json).contains("\"payload\"")
        assertThat(json).contains("\"config\"")
        assertThat(json).contains("\"creator\"")
        assertThat(json).contains("\"membersCount\"")
        assertThat(json).contains("\"derivation\"")
        assertThat(json).contains("\" Alice \"")
    }

    @Test
    fun `GIVEN invite preview json WHEN parse THEN whole object matches contract`() {
        // Arrange
        @Language("JSON")
        val json = """
            {
              "config": {
                "name": "Family",
                "icon": "Family",
                "iconColor": "Azure",
                "membersCount": 3,
                "threshold": 2
              },
              "creator": {
                "name": "Alice",
                "address": "0x7e5f4552091a69125d5DfCb7b8C2659029395Bdf"
              }
            }
        """.trimIndent()

        // Act
        val actual = moshi.adapter(JointAccountInvitePreviewDto::class.java).fromJson(json)

        // Assert
        val expected = JointAccountInvitePreviewDto(
            config = JointAccountConfigDto(
                name = "Family",
                icon = "Family",
                iconColor = "Azure",
                membersCount = 3,
                threshold = 2,
            ),
            creator = JointAccountInvitePreviewDto.Creator(
                name = "Alice",
                address = "0x7e5f4552091a69125d5DfCb7b8C2659029395Bdf",
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN join request WHEN round trip THEN object survives and field names match contract`() {
        // Arrange
        val request = JoinJointAccountRequest(
            payload = JoinJointAccountRequest.Payload(
                inviteId = "AA11BB22CC33DD44EE55FF66AA77BB88CC99DD00EE11FF22AA33BB44CC55DD66",
                config = JointAccountConfigDto(
                    name = "Family",
                    icon = "Family",
                    iconColor = "Azure",
                    membersCount = 3,
                    threshold = 2,
                ),
                member = JointAccountParticipantDto(
                    walletId = "4B2F1C8A9E7D6053A1B4C7E2F8D9A0B3C5E7F1A2D4B6C8E0F2A4B6C8D0E2F4A6",
                    name = "Bob",
                    address = "0x2B5AD5c4795c026514f8317c7a215E218DcCD6cF",
                    derivation = 0,
                ),
            ),
            signature = "0x" + "ab".repeat(65),
        )
        val adapter = moshi.adapter(JoinJointAccountRequest::class.java)

        // Act
        val json = adapter.toJson(request)
        val actual = adapter.fromJson(json)

        // Assert
        assertThat(actual).isEqualTo(request)
        assertThat(json).contains("\"payload\"")
        assertThat(json).contains("\"inviteId\"")
        assertThat(json).contains("\"config\"")
        assertThat(json).contains("\"member\"")
        assertThat(json).contains("\"derivation\"")
    }

    @Test
    fun `GIVEN activate request WHEN round trip THEN object survives and field names match contract`() {
        // Arrange
        val request = ActivateJointAccountRequest(
            payload = ActivateJointAccountRequest.Payload(
                walletId = "4B2F1C8A9E7D6053A1B4C7E2F8D9A0B3C5E7F1A2D4B6C8E0F2A4B6C8D0E2F4A6",
                cryptoAccountId = "AA11BB22CC33DD44EE55FF66AA77BB88CC99DD00EE11FF22AA33BB44CC55DD66",
                config = ActivateJointAccountRequest.ConfirmedConfig(
                    name = "Family",
                    icon = "Family",
                    iconColor = "Azure",
                    membersCount = 3,
                    threshold = 2,
                    safeAddress = "0x7e5f4552091a69125d5DfCb7b8C2659029395Bdf",
                ),
            ),
            signature = "0x" + "ab".repeat(65),
        )
        val adapter = moshi.adapter(ActivateJointAccountRequest::class.java)

        // Act
        val json = adapter.toJson(request)
        val actual = adapter.fromJson(json)

        // Assert
        assertThat(actual).isEqualTo(request)
        assertThat(json).contains("\"payload\"")
        assertThat(json).contains("\"cryptoAccountId\"")
        assertThat(json).contains("\"config\"")
        assertThat(json).contains("\"safeAddress\"")
    }
}