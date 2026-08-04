package com.tangem.data.polymarket.builder

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.approval.PolymarketApprovalCalls
import org.json.JSONObject
import org.junit.jupiter.api.Test

internal class PolymarketTypedDataBuilderTest {

    private fun fieldNames(json: JSONObject, typeName: String): List<String> {
        val arr = json.getJSONObject("types").getJSONArray(typeName)
        return (0 until arr.length()).map { arr.getJSONObject(it).getString("name") }
    }

    // region ClobAuth

    @Test
    fun `GIVEN owner and timestamp WHEN buildClobAuth THEN schema matches ClobAuth spec`() {
        // Act
        val json = JSONObject(
            PolymarketTypedDataBuilder.buildClobAuth(address = "0xOwner", timestamp = "1700000000"),
        )

        // Assert
        assertThat(json.getString("primaryType")).isEqualTo("ClobAuth")

        val domain = json.getJSONObject("domain")
        assertThat(domain.getString("name")).isEqualTo("ClobAuthDomain")
        assertThat(domain.getString("version")).isEqualTo("1")
        assertThat(domain.getLong("chainId")).isEqualTo(137L)
        assertThat(domain.has("verifyingContract")).isFalse()

        assertThat(fieldNames(json, "EIP712Domain")).containsExactly("name", "version", "chainId").inOrder()
        assertThat(fieldNames(json, "ClobAuth"))
            .containsExactly("address", "timestamp", "nonce", "message").inOrder()

        val message = json.getJSONObject("message")
        assertThat(message.getString("address")).isEqualTo("0xOwner")
        assertThat(message.getString("timestamp")).isEqualTo("1700000000")
        assertThat(message.getString("nonce")).isEqualTo("0")
        assertThat(message.getString("message"))
            .isEqualTo("This message attests that I control the given wallet")
    }

    @Test
    fun `GIVEN ClobAuth WHEN buildClobAuth THEN domain has no verifyingContract key`() {
        // Act
        val json = JSONObject(
            PolymarketTypedDataBuilder.buildClobAuth(address = "0xOwner", timestamp = "1700000000"),
        )

        // Assert
        assertThat(json.getJSONObject("domain").has("verifyingContract")).isFalse()
    }

    // endregion

    // region Batch

    @Test
    fun `GIVEN dw nonce deadline calls WHEN buildApprovalsBatch THEN schema matches DepositWallet spec`() {
        // Arrange
        val dw = "0xDepositWallet"
        val calls = PolymarketApprovalCalls.build()

        // Act
        val json = JSONObject(
            PolymarketTypedDataBuilder.buildApprovalsBatch(
                depositWallet = dw, nonce = "0", deadline = "1700000600", calls = calls,
            ),
        )

        // Assert
        assertThat(json.getString("primaryType")).isEqualTo("Batch")

        val domain = json.getJSONObject("domain")
        assertThat(domain.getString("name")).isEqualTo("DepositWallet")
        assertThat(domain.getString("version")).isEqualTo("1")
        assertThat(domain.getLong("chainId")).isEqualTo(137L)
        assertThat(domain.getString("verifyingContract")).isEqualTo(dw)

        assertThat(fieldNames(json, "EIP712Domain"))
            .containsExactly("name", "version", "chainId", "verifyingContract").inOrder()
        assertThat(fieldNames(json, "Batch"))
            .containsExactly("wallet", "nonce", "deadline", "calls").inOrder()
        assertThat(fieldNames(json, "Call")).containsExactly("target", "value", "data").inOrder()

        val message = json.getJSONObject("message")
        assertThat(message.getString("wallet")).isEqualTo(dw)
        assertThat(message.getString("nonce")).isEqualTo("0")
        assertThat(message.getString("deadline")).isEqualTo("1700000600")

        val messageCalls = message.getJSONArray("calls")
        assertThat(messageCalls.length()).isEqualTo(6)
        val first = messageCalls.getJSONObject(0)
        assertThat(first.getString("target")).isEqualTo(calls[0].target)
        assertThat(first.getString("value")).isEqualTo("0")
        assertThat(first.getString("data")).isEqualTo(calls[0].data)
        val last = messageCalls.getJSONObject(5)
        assertThat(last.getString("data")).isEqualTo(calls[5].data)
    }

    // endregion
}