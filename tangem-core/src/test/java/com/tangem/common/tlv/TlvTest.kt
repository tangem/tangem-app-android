package com.tangem.common.tlv

import com.google.common.truth.Truth.assertThat
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import org.junit.Test


class TlvTest {

    @Test
    fun `TLVs to bytes, only PIN`() {
        val tlvs = listOf(
                Tlv(TlvTag.Pin, "000000".calculateSha256())
        )
        val expected = byteArrayOf(16, 32, -111, -76, -47, 66, -126, 63, 125, 32, -59, -16, -115,
                -10, -111, 34, -34, 67, -13, 95, 5, 122, -104, -115, -106, 25, -10, -45, 19, -124,
                -123, -55, -94, 3)

        assertThat(tlvs.toBytes())
                .isEqualTo(expected)
    }

    @Test
    fun `TLVs to bytes, check wallet`() {
        val tlvs = listOf(
                Tlv(TlvTag.Pin, "000000".calculateSha256()),
                Tlv(TlvTag.CardId, "cb22000000027374".hexToBytes()),
                Tlv(TlvTag.Challenge, byteArrayOf(-82, -78, -31, 34, 66, -19, -86, -1, 26, 8, 100, -126, -74, 20, -28, 83))
        )

        val expected = byteArrayOf(16, 32, -111, -76, -47, 66, -126, 63, 125, 32, -59, -16, -115, -10,
                -111, 34, -34, 67, -13, 95, 5, 122, -104, -115, -106, 25, -10, -45, 19, -124, -123,
                -55, -94, 3, 1, 8, -53, 34, 0, 0, 0, 2, 115, 116, 22, 16, -82, -78, -31, 34, 66, -19,
                -86, -1, 26, 8, 100, -126, -74, 20, -28, 83)

        assertThat(tlvs.toBytes())
                .isEqualTo(expected)
    }

    @Test
    fun `Bytes to Tlvs, only PIN`() {
        val bytes = byteArrayOf(16, 32, -111, -76, -47, 66, -126, 63, 125, 32, -59, -16, -115,
                -10, -111, 34, -34, 67, -13, 95, 5, 122, -104, -115, -106, 25, -10, -45, 19, -124,
                -123, -55, -94, 3)

        val tlvs = Tlv.tlvListFromBytes(bytes)

        assertThat(tlvs)
                .isNotNull()
        assertThat(tlvs)
                .isNotEmpty()

        val pin = tlvs!!.find { it.tag == TlvTag.Pin  }?.value
        val pinExpected = "000000".calculateSha256()

        assertThat(pin)
                .isEqualTo(pinExpected)
    }

    @Test
    fun `Bytes to TLVs, check wallet TLVs`() {
        val bytes = byteArrayOf(16, 32, -111, -76, -47, 66, -126, 63, 125, 32, -59, -16, -115, -10,
                -111, 34, -34, 67, -13, 95, 5, 122, -104, -115, -106, 25, -10, -45, 19, -124, -123,
                -55, -94, 3, 1, 8, -53, 34, 0, 0, 0, 2, 115, 116, 22, 16, -82, -78, -31, 34, 66, -19,
                -86, -1, 26, 8, 100, -126, -74, 20, -28, 83)

        val tlvs = Tlv.tlvListFromBytes(bytes)

        assertThat(tlvs)
                .isNotNull()
        assertThat(tlvs)
                .isNotEmpty()

        val pin = tlvs!!.find { it.tag == TlvTag.Pin  }?.value
        val pinExpected = "000000".calculateSha256()
        assertThat(pin)
                .isEqualTo(pinExpected)

        val cardId = tlvs.find { it.tag == TlvTag.CardId }?.value
        val cardIdExpected = "cb22000000027374".hexToBytes()
        assertThat(cardId)
                .isEqualTo(cardIdExpected)

        val challenge = tlvs.find { it.tag == TlvTag.Challenge }?.value
        val challengeExpected = byteArrayOf(-82, -78, -31, 34, 66, -19, -86, -1, 26, 8, 100, -126, -74, 20, -28, 83)
        assertThat(challenge)
                .isEqualTo(challengeExpected)
    }

    @Test
    fun `Bytes to TLVs, wrong values`() {
        val bytes = byteArrayOf(0)
        val tlvs = Tlv.tlvListFromBytes(bytes)
       assertThat(tlvs)
               .isNull()

        val bytes1 = byteArrayOf(0, 0, 0, 0, 0, 0, 0)
        val tlvs1 = Tlv.tlvListFromBytes(bytes1)
        assertThat(tlvs1)
                .isNull()
    }
}