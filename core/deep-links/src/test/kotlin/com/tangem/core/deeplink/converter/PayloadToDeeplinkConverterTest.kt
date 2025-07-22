package com.tangem.core.deeplink.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.deeplink.DEEPLINK_KEY
import com.tangem.core.deeplink.DeeplinkConst.DERIVATION_PATH_KEY
import com.tangem.core.deeplink.DeeplinkConst.NETWORK_ID_KEY
import com.tangem.core.deeplink.DeeplinkConst.TOKEN_ID_KEY
import com.tangem.core.deeplink.DeeplinkConst.TYPE_KEY
import com.tangem.core.deeplink.DeeplinkConst.WALLET_ID_KEY
import org.junit.Test

internal class PayloadToDeeplinkConverterTest {

    @Test
    fun `GIVEN payload with deeplink key WHEN convert THEN should return deeplink value`() {
        // GIVEN
        val payload = mapOf(
            DEEPLINK_KEY to "tangem://token-details?networkId=ethereum&tokenId=0x123&type=token&user_wallet_id=wallet123" +
                "&derivation_path=m'0'0'0",
        )

        // WHEN
        val result = PayloadToDeeplinkConverter.convert(payload)

        // THEN
        assertThat(result).isEqualTo(
            "tangem://token-details?networkId=ethereum&tokenId=0x123&type=token&user_wallet_id=wallet123&derivation_path=m'0'0'0",
        )
    }

    @Test
    fun `GIVEN valid push notification payload with all vital values WHEN convert THEN should return correct deeplink`() {
        // GIVEN
        val payload = mapOf(
            TYPE_KEY to "token",
            NETWORK_ID_KEY to "ethereum",
            TOKEN_ID_KEY to "0x123",
            WALLET_ID_KEY to "wallet123",
            DERIVATION_PATH_KEY to "m'0'0'0",
        )

        // WHEN
        val result = PayloadToDeeplinkConverter.convert(payload)

        // THEN
        assertThat(result).isEqualTo(
            "tangem://token?network_id=ethereum&token_id=0x123&type=token&user_wallet_id=wallet123&derivation_path=m'0'0'0",
        )
    }

    @Test
    fun `GIVEN push notification payload without derivationPath WHEN convert THEN should return correct deeplink without derivation_path`() {
        // GIVEN
        val payload = mapOf(
            TYPE_KEY to "token",
            NETWORK_ID_KEY to "ethereum",
            TOKEN_ID_KEY to "0x123",
            WALLET_ID_KEY to "wallet123",
        )

        // WHEN
        val result = PayloadToDeeplinkConverter.convert(payload)

        // THEN
        assertThat(result).isEqualTo(
            "tangem://token?network_id=ethereum&token_id=0x123&type=token&user_wallet_id=wallet123",
        )
    }

    @Test
    fun `GIVEN push notification payload with missing type WHEN convert THEN should return null`() {
        // GIVEN
        val payload = mapOf(
            NETWORK_ID_KEY to "ethereum",
            TOKEN_ID_KEY to "0x123",
            WALLET_ID_KEY to "wallet123",
            DERIVATION_PATH_KEY to "m'0'0'0",
        )

        // WHEN
        val result = PayloadToDeeplinkConverter.convert(payload)

        // THEN
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN push notification payload with missing networkId WHEN convert THEN should return null`() {
        // GIVEN
        val payload = mapOf(
            TYPE_KEY to "token",
            TOKEN_ID_KEY to "0x123",
            WALLET_ID_KEY to "wallet123",
            DERIVATION_PATH_KEY to "m'0'0'0",
        )

        // WHEN
        val result = PayloadToDeeplinkConverter.convert(payload)

        // THEN
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN push notification payload with missing tokenId WHEN convert THEN should return null`() {
        // GIVEN
        val payload = mapOf(
            TYPE_KEY to "token",
            NETWORK_ID_KEY to "ethereum",
            WALLET_ID_KEY to "wallet123",
            DERIVATION_PATH_KEY to "m'0'0'0",
        )

        // WHEN
        val result = PayloadToDeeplinkConverter.convert(payload)

        // THEN
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN push notification payload with missing walletId WHEN convert THEN should return null`() {
        // GIVEN
        val payload = mapOf(
            TYPE_KEY to "token",
            NETWORK_ID_KEY to "ethereum",
            TOKEN_ID_KEY to "0x123",
        )

        // WHEN
        val result = PayloadToDeeplinkConverter.convert(payload)

        // THEN
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN empty payload WHEN convert THEN should return null`() {
        // GIVEN
        val payload = emptyMap<String, String>()

        // WHEN
        val result = PayloadToDeeplinkConverter.convert(payload)

        // THEN
        assertThat(result).isNull()
    }
}