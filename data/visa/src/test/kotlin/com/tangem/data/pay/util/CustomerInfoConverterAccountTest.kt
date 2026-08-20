package com.tangem.data.pay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.spend.datasource.pay.models.response.BalanceResponse
import com.tangem.spend.datasource.pay.models.response.CryptoBalance
import com.tangem.spend.datasource.pay.models.response.CustomerMeResponse
import com.tangem.spend.datasource.pay.models.response.FiatBalance
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
 * Covers what proves an account exists: `payment_account`, the product instances and the `cards[]` payload
 * survive the mapping even when the response carries no balances.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CustomerInfoConverterAccountTest {

    @Test
    fun `GIVEN payment account WHEN convert THEN payment account is mapped`() {
        // Arrange
        val result = result(
            paymentAccount = CustomerMeResponse.PaymentAccount(
                id = "pa_1",
                address = "0xaccount",
                customerWalletAddress = "0xwallet",
            ),
        )

        // Act
        val info = CustomerInfoConverter.convert(result)

        // Assert
        assertThat(info.paymentAccount).isEqualTo(
            CustomerInfo.PaymentAccount(
                id = "pa_1",
                address = "0xaccount",
                customerWalletAddress = "0xwallet",
            ),
        )
    }

    @Test
    fun `GIVEN no payment account WHEN convert THEN payment account is null`() {
        // Act
        val info = CustomerInfoConverter.convert(result(paymentAccount = null))

        // Assert
        assertThat(info.paymentAccount).isNull()
    }

    @Test
    fun `GIVEN cards without balances WHEN convert THEN cards are kept`() {
        // Arrange
        val result = result(
            paymentAccount = null,
            balance = BalanceResponse(fiat = null, crypto = null, availableForWithdrawal = null),
            cards = listOf(card(id = "card_1")),
        )

        // Act
        val info = CustomerInfoConverter.convert(result)

        // Assert — an empty balance no longer erases the card payload
        assertThat(info.cards.map { it.cardId }).containsExactly("card_1")
        assertThat(info.fiatBalance).isNull()
        assertThat(info.cryptoBalance).isNull()
    }

    @Test
    fun `GIVEN active card instance and no balances WHEN convert THEN customer is enrolled`() {
        // Arrange
        val result = result(
            paymentAccount = null,
            productInstances = listOf(productInstance(status = CustomerMeResponse.ProductInstance.Status.ACTIVE)),
        )

        // Act
        val info = CustomerInfoConverter.convert(result)

        // Assert
        assertThat(info.isEnrolled).isTrue()
        assertThat(info.activeCardProductInstances).hasSize(1)
    }

    @Test
    fun `GIVEN neither account nor instances nor balances WHEN convert THEN customer is not enrolled`() {
        // Act
        val info = CustomerInfoConverter.convert(result(paymentAccount = null))

        // Assert
        assertThat(info.isEnrolled).isFalse()
    }

    @Test
    fun `GIVEN balances only WHEN convert THEN customer is enrolled`() {
        // Arrange
        val result = result(
            paymentAccount = null,
            balance = BalanceResponse(
                fiat = FiatBalance(
                    currency = "USD",
                    availableBalance = BigDecimal.TEN,
                    creditLimit = BigDecimal.ZERO,
                    pendingCharges = BigDecimal.ZERO,
                    postedCharges = BigDecimal.ZERO,
                    balanceDue = BigDecimal.ZERO,
                ),
                crypto = CryptoBalance(
                    id = "usdc",
                    chainId = 137,
                    depositAddress = "0xdeposit",
                    tokenContractAddress = "0xcontract",
                    balance = BigDecimal.TEN,
                ),
                availableForWithdrawal = null,
            ),
        )

        // Act
        val info = CustomerInfoConverter.convert(result)

        // Assert
        assertThat(info.isEnrolled).isTrue()
    }

    private fun result(
        paymentAccount: CustomerMeResponse.PaymentAccount?,
        balance: BalanceResponse? = BalanceResponse(fiat = null, crypto = null, availableForWithdrawal = null),
        productInstances: List<CustomerMeResponse.ProductInstance> = emptyList(),
        cards: List<CustomerMeResponse.Card> = emptyList(),
    ) = CustomerMeResponse.Result(
        id = "c1",
        state = "ACTIVE",
        createdAt = "2026-01-01T00:00:00Z",
        paymentAccount = paymentAccount,
        kyc = null,
        depositAddress = null,
        balance = balance,
        productInstances = productInstances,
        cards = cards,
        customerTariffPlan = null,
    )

    private fun productInstance(status: CustomerMeResponse.ProductInstance.Status) =
        CustomerMeResponse.ProductInstance(
            id = "pi_1",
            cid = null,
            cardId = "card_1",
            cardWalletAddress = null,
            status = status,
            updatedAt = "2026-01-01T00:00:00Z",
            paymentAccountId = "pa_1",
            displayName = null,
            actualCardLimit = null,
            adminCardLimit = null,
            specificationDataType = CustomerMeResponse.ProductInstance.SpecificationDataType.CARD,
        )

    private fun card(id: String) = CustomerMeResponse.Card(
        id = id,
        token = "token",
        expirationMonth = "01",
        expirationYear = "30",
        embossName = "name",
        cardType = "VIRTUAL",
        cardStatus = "ACTIVE",
        cardNumberEnd = "1234",
        isPinSet = true,
        images = null,
    )
}