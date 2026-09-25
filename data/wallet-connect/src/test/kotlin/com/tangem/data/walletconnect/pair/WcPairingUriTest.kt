package com.tangem.data.walletconnect.pair

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class WcPairingUriTest {

    private val topic = "7f6e5d4c3b2a19081716151413121110ffeeddccbbaa99887766554433221100"

    @Test
    fun `GIVEN v2 pairing uri WHEN topicOf THEN pairing topic is returned lowercased`() {
        val uri = "wc:${topic.uppercase()}@2?relay-protocol=irn&symKey=abcd&expiryTimestamp=1700000000"

        assertThat(WcPairingUri.topicOf(uri)).isEqualTo(topic)
    }

    @Test
    fun `GIVEN surrounding whitespace WHEN topicOf THEN still parsed`() {
        assertThat(WcPairingUri.topicOf("  wc:$topic@2?relay-protocol=irn\n")).isEqualTo(topic)
    }

    @Test
    fun `GIVEN non wc uri WHEN topicOf THEN null`() {
        assertThat(WcPairingUri.topicOf("https://example.com")).isNull()
        assertThat(WcPairingUri.topicOf("")).isNull()
    }

    @Test
    fun `GIVEN malformed topic WHEN topicOf THEN null so no filter is applied`() {
        assertThat(WcPairingUri.topicOf("wc:not-a-topic@2?relay-protocol=irn")).isNull()
        assertThat(WcPairingUri.topicOf("wc:${topic.dropLast(1)}@2")).isNull()
    }
}
