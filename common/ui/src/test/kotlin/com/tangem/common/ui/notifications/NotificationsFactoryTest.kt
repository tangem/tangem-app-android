package com.tangem.common.ui.notifications

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.ui.notifications.NotificationsFactory.addFeeUnreachableNotification
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

internal class NotificationsFactoryTest {

    private val currencyFactory = MockCryptoCurrencyFactory()
    private val usdtStatus = CryptoCurrencyStatus(
        currency = currencyFactory.createToken(blockchain = Blockchain.Tron, contractAddress = "TUsdt"),
        value = CryptoCurrencyStatus.Loading,
    )
    private val trxStatus = CryptoCurrencyStatus(
        currency = currencyFactory.createCoin(blockchain = Blockchain.Tron),
        value = CryptoCurrencyStatus.Loading,
    )
    private val activationRefusal = GetFeeError.GaslessError.TronActivationNotEnoughFunds(
        minimumAmount = BigDecimal("5"),
        tokenSymbol = "USDT",
    )
    private val onReload: () -> Unit = {}
    private val expectedNotification = NotificationUM.Error.TronActivationNotEnoughFunds(
        minimumAmount = "5",
        tokenSymbol = "USDT",
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AddFeeUnreachableNotification {

        @Test
        fun `GIVEN Tron activation minimum not met WHEN send notifications built THEN insufficient balance error`() {
            // Act
            val actual = buildList {
                addFeeUnreachableNotification(
                    tokenStatus = usdtStatus,
                    coinStatus = trxStatus,
                    feeError = activationRefusal,
                    dustValue = null,
                    onReload = {},
                    onClick = {},
                )
            }

            // Assert
            assertThat(actual).containsExactly(expectedNotification)
        }

        @Test
        fun `GIVEN Tron activation minimum not met WHEN short notifications built THEN insufficient balance error`() {
            // Act
            val actual = buildList {
                addFeeUnreachableNotification(feeError = activationRefusal, tokenName = "Tron", onReload = {})
            }

            // Assert
            assertThat(actual).containsExactly(expectedNotification)
        }

        @Test
        fun `GIVEN per-wallet activation limit WHEN send notifications built THEN no notification`() {
            // Act
            val actual = buildSendNotifications(feeError = rateLimited(isPerWallet = true))

            // Assert
            assertThat(actual).isEmpty()
        }

        @Test
        fun `GIVEN service-wide activation limit WHEN send notifications built THEN fee unreachable with reload`() {
            // Act
            val actual = buildSendNotifications(feeError = rateLimited(isPerWallet = false))

            // Assert
            assertThat(actual).containsExactly(NotificationUM.Warning.NetworkFeeUnreachable(onReload))
        }

        @Test
        fun `GIVEN per-wallet activation limit WHEN short notifications built THEN no notification`() {
            // Act
            val actual = buildList {
                addFeeUnreachableNotification(feeError = rateLimited(isPerWallet = true), tokenName = "Tron", onReload)
            }

            // Assert
            assertThat(actual).isEmpty()
        }

        @Test
        fun `GIVEN service-wide activation limit WHEN short notifications built THEN fee unreachable with reload`() {
            // Act
            val actual = buildList {
                addFeeUnreachableNotification(feeError = rateLimited(isPerWallet = false), tokenName = "Tron", onReload)
            }

            // Assert
            assertThat(actual).containsExactly(NotificationUM.Warning.NetworkFeeUnreachable(onReload))
        }
    }

    private fun rateLimited(isPerWallet: Boolean) = GetFeeError.GaslessError.TronActivationRateLimited(isPerWallet)

    private fun buildSendNotifications(feeError: GetFeeError) = buildList {
        addFeeUnreachableNotification(
            tokenStatus = usdtStatus,
            coinStatus = trxStatus,
            feeError = feeError,
            dustValue = null,
            onReload = onReload,
            onClick = {},
        )
    }
}