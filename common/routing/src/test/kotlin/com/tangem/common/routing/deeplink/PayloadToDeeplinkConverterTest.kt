package com.tangem.common.routing.deeplink

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.deeplink.DeeplinkConst.CUSTOMER_WALLET_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.DEEPLINK_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.DERIVATION_PATH_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.NETWORK_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TOKEN_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TRANSACTION_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TYPE_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.WALLET_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.WEBLINK_KEY
import org.junit.jupiter.api.Test

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
            "tangem://token?network_id=ethereum&token_id=0x123&type=token&user_wallet_id=wallet123" +
                "&derivation_path=m%270%270%270",
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

    @Test
    fun `GIVEN tangem pay card_ready push payload WHEN convert THEN should return pay-app-main deeplink`() {
        // GIVEN
        val payload = mapOf(
            TYPE_KEY to "card_ready",
            CUSTOMER_WALLET_ID_KEY to "wallet123",
        )

        // WHEN
        val result = PayloadToDeeplinkConverter.convert(payload)

        // THEN
        assertThat(result).isEqualTo(
            "tangem://pay-app-main?type=card_ready&customer_wallet_id=wallet123",
        )
    }

    @Test
    fun `GIVEN tangem pay transaction_spend push payload WHEN convert THEN should return pay-app-main deeplink with transaction_id`() {
        // GIVEN
        val payload = mapOf(
            TYPE_KEY to "transaction_spend",
            CUSTOMER_WALLET_ID_KEY to "wallet123",
            TRANSACTION_ID_KEY to "test456",
        )

        // WHEN
        val result = PayloadToDeeplinkConverter.convert(payload)

        // THEN
        assertThat(result).isEqualTo(
            "tangem://pay-app-main?type=transaction_spend&customer_wallet_id=wallet123&transaction_id=test456",
        )
    }

    @Test
    fun `GIVEN tangem pay top_up push payload WHEN convert THEN should return pay-app-main deeplink`() {
        // GIVEN
        val payload = mapOf(
            TYPE_KEY to "declined_top_up",
            CUSTOMER_WALLET_ID_KEY to "wallet123",
            TRANSACTION_ID_KEY to "test456",
        )

        // WHEN
        val result = PayloadToDeeplinkConverter.convert(payload)

        // THEN
        assertThat(result).isEqualTo(
            "tangem://pay-app-main?type=declined_top_up&customer_wallet_id=wallet123&transaction_id=test456",
        )
    }

    @Test
    fun `GIVEN tangem pay declined_reason push payload WHEN convert THEN should return pay-app-main deeplink`() {
        // GIVEN
        val payload = mapOf(
            TYPE_KEY to "declined_reason9",
            CUSTOMER_WALLET_ID_KEY to "wallet123",
            TRANSACTION_ID_KEY to "test456",
        )

        // WHEN
        val result = PayloadToDeeplinkConverter.convert(payload)

        // THEN
        assertThat(result).isEqualTo(
            "tangem://pay-app-main?type=declined_reason9&customer_wallet_id=wallet123&transaction_id=test456",
        )
    }

    @Test
    fun `GIVEN tangem pay refund push payload WHEN convert THEN should return pay-app-main deeplink`() {
        // GIVEN
        val payload = mapOf(
            TYPE_KEY to "transaction_spend_refund",
            CUSTOMER_WALLET_ID_KEY to "wallet123",
            TRANSACTION_ID_KEY to "test456",
        )

        // WHEN
        val result = PayloadToDeeplinkConverter.convert(payload)

        // THEN
        assertThat(result).isEqualTo(
            "tangem://pay-app-main?type=transaction_spend_refund&customer_wallet_id=wallet123&transaction_id=test456",
        )
    }

    @Test
    fun `GIVEN tangem pay threshold top-up push payload WHEN convert THEN should return pay-app-main deeplink`() {
        // GIVEN
        val payload = mapOf(
            TYPE_KEY to "threshold1_top_up",
            CUSTOMER_WALLET_ID_KEY to "wallet123",
        )

        // WHEN
        val result = PayloadToDeeplinkConverter.convert(payload)

        // THEN
        assertThat(result).isEqualTo(
            "tangem://pay-app-main?type=threshold1_top_up&customer_wallet_id=wallet123",
        )
    }

    @Test
    fun `GIVEN tangem pay collateral push payload WHEN convert THEN should return pay-app-main deeplink`() {
        // GIVEN
        val payload = mapOf(
            TYPE_KEY to "collateral_deposit",
            CUSTOMER_WALLET_ID_KEY to "wallet123",
        )

        // WHEN
        val result = PayloadToDeeplinkConverter.convert(payload)

        // THEN
        assertThat(result).isEqualTo(
            "tangem://pay-app-main?type=collateral_deposit&customer_wallet_id=wallet123",
        )
    }

    @Test
    fun `GIVEN tangem pay push payload with missing customer_wallet_id WHEN convert THEN should return null`() {
        // GIVEN
        val payload = mapOf(
            TYPE_KEY to "card_ready",
        )

        // WHEN
        val result = PayloadToDeeplinkConverter.convert(payload)

        // THEN
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN payload with link key holding a deeplink WHEN convert THEN should return link value`() {
        // GIVEN
        val payload = mapOf(WEBLINK_KEY to "tangem://markets")

        // WHEN
        val result = PayloadToDeeplinkConverter.convert(payload)

        // THEN
        assertThat(result).isEqualTo("tangem://markets")
    }

    @Test
    fun `GIVEN payload with link key holding a web url WHEN convert THEN should return link value`() {
        // GIVEN
        val payload = mapOf(WEBLINK_KEY to "https://tangem.com/pricing/")

        // WHEN
        val result = PayloadToDeeplinkConverter.convert(payload)

        // THEN
        assertThat(result).isEqualTo("https://tangem.com/pricing/")
    }

    @Test
    fun `GIVEN payload with both deeplink and link keys WHEN convert THEN deeplink should win`() {
        // GIVEN
        val payload = mapOf(
            DEEPLINK_KEY to "tangem://main",
            WEBLINK_KEY to "https://tangem.com/pricing/",
        )

        // WHEN
        val result = PayloadToDeeplinkConverter.convert(payload)

        // THEN
        assertThat(result).isEqualTo("tangem://main")
    }

    @Test
    fun `GIVEN payload with token keys and link key WHEN convert THEN token deeplink should win`() {
        // GIVEN
        val payload = mapOf(
            TYPE_KEY to "token",
            NETWORK_ID_KEY to "ethereum",
            TOKEN_ID_KEY to "0x123",
            WALLET_ID_KEY to "wallet123",
            WEBLINK_KEY to "https://tangem.com/pricing/",
        )

        // WHEN
        val result = PayloadToDeeplinkConverter.convert(payload)

        // THEN
        assertThat(result).isEqualTo(
            "tangem://token?network_id=ethereum&token_id=0x123&type=token&user_wallet_id=wallet123",
        )
    }

    @Test
    fun `GIVEN payload with tangem pay keys and link key WHEN convert THEN pay-app-main deeplink should win`() {
        // GIVEN
        val payload = mapOf(
            TYPE_KEY to "card_ready",
            CUSTOMER_WALLET_ID_KEY to "wallet123",
            WEBLINK_KEY to "https://tangem.com/pricing/",
        )

        // WHEN
        val result = PayloadToDeeplinkConverter.convert(payload)

        // THEN
        assertThat(result).isEqualTo(
            "tangem://pay-app-main?type=card_ready&customer_wallet_id=wallet123" +
                "&link=https%3A%2F%2Ftangem.com%2Fpricing%2F",
        )
    }
}