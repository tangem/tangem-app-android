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
import com.tangem.domain.visa.model.TangemPayPushNotificationType
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

    @Test
    fun `GIVEN tangem pay card_ready push payload WHEN convert THEN should return pay-app-main deeplink`() {
        // GIVEN
        val payload = mapOf(
            TYPE_KEY to TangemPayPushNotificationType.CARD_READY.value,
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
            TYPE_KEY to TangemPayPushNotificationType.TRANSACTION_SPEND.value,
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
            TYPE_KEY to TangemPayPushNotificationType.DECLINED_TOP_UP.value,
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
    fun `GIVEN tangem pay collateral push payload WHEN convert THEN should return pay-app-main deeplink`() {
        // GIVEN
        val payload = mapOf(
            TYPE_KEY to TangemPayPushNotificationType.COLLATERAL_DEPOSIT.value,
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
            TYPE_KEY to TangemPayPushNotificationType.CARD_READY.value,
        )

        // WHEN
        val result = PayloadToDeeplinkConverter.convert(payload)

        // THEN
        assertThat(result).isNull()
    }
}