package com.tangem.data.pay.util

import arrow.core.getOrElse
import com.tangem.datasource.api.pay.models.response.CryptoBalance
import com.tangem.datasource.api.pay.models.response.CustomerMeResponse
import com.tangem.datasource.api.pay.models.response.FiatBalance
import com.tangem.domain.models.account.CardDisplayName
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.pay.TangemPayCardLimit
import com.tangem.domain.models.pay.TangemPayCardLimitPeriod
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.CustomerInfo.CardInfo
import com.tangem.domain.pay.model.CustomerInfo.ProductInstance
import com.tangem.domain.pay.model.CustomerInfo.ProductInstance.Status
import com.tangem.domain.visa.model.TangemPayCardFrozenState
import com.tangem.utils.converter.Converter

internal object CustomerInfoConverter : Converter<CustomerMeResponse.Result, CustomerInfo> {
    @Suppress("ComplexCondition")
    override fun convert(value: CustomerMeResponse.Result): CustomerInfo {
        val kycStatus = KycStatus.fromString(status = value.kyc?.status)
        val card = value.card
        val fiatBalance = value.balance?.fiat
        val cryptoBalance = value.balance?.crypto
        val paymentAccount = value.paymentAccount
        val cardInfo = if (paymentAccount != null && card != null && fiatBalance != null && cryptoBalance != null) {
            CardInfo(
                lastFourDigits = card.cardNumberEnd,
                balance = fiatBalance.availableBalance,
                currencyCode = fiatBalance.currency,
                depositAddress = value.depositAddress,
                isPinSet = value.card?.isPinSet == true,
                fiatBalance = fiatBalance.toDomain(),
                cryptoBalance = cryptoBalance.toDomain(),
            )
        } else {
            null
        }
        val productInstance = value.productInstance?.let { instance ->
            val status = instance.status.toDomain()
            val cardFrozenState = when (status) {
                Status.ACTIVE -> TangemPayCardFrozenState.Unfrozen
                else -> TangemPayCardFrozenState.Frozen
            }
            val displayName = instance.displayName?.ifEmpty { null }

            ProductInstance(
                id = instance.id,
                cardId = instance.cardId,
                frozenState = cardFrozenState,
                status = status,
                displayName = if (displayName != null) CardDisplayName(displayName).getOrElse { null } else null,
                actualCardLimit = instance.actualCardLimit?.parseCardLimit(),
                adminCardLimit = instance.adminCardLimit?.parseCardLimit(),
            )
        }
        return CustomerInfo(
            customerId = value.id,
            productInstance = productInstance,
            kycStatus = kycStatus,
            cardInfo = cardInfo,
            state = CustomerInfo.State.fromString(value.state),
            fiatBalance = fiatBalance?.toDomain(),
            cryptoBalance = cryptoBalance?.toDomain(),
        )
    }

    private fun CustomerMeResponse.CardLimit.parseCardLimit(): TangemPayCardLimit {
        return TangemPayCardLimit(
            amount = amount,
            period = TangemPayCardLimitPeriod.fromString(periodType),
        )
    }

    private fun FiatBalance.toDomain() = PaymentAccountStatusValue.FiatBalance(
        availableBalance = availableBalance,
        currency = currency,
    )

    private fun CryptoBalance.toDomain() = PaymentAccountStatusValue.CryptoBalance(
        id = id,
        chainId = chainId.toLong(),
        depositAddress = depositAddress.orEmpty(),
        tokenContractAddress = tokenContractAddress,
        balance = balance,
    )

    private fun CustomerMeResponse.ProductInstance.Status.toDomain(): Status = when (this) {
        CustomerMeResponse.ProductInstance.Status.NEW -> Status.NEW
        CustomerMeResponse.ProductInstance.Status.READY_FOR_MANUFACTURING -> Status.READY_FOR_MANUFACTURING
        CustomerMeResponse.ProductInstance.Status.MANUFACTURING -> Status.MANUFACTURING
        CustomerMeResponse.ProductInstance.Status.SENT_TO_DELIVERY -> Status.SENT_TO_DELIVERY
        CustomerMeResponse.ProductInstance.Status.DELIVERED -> Status.DELIVERED
        CustomerMeResponse.ProductInstance.Status.ACTIVATING -> Status.ACTIVATING
        CustomerMeResponse.ProductInstance.Status.ACTIVE -> Status.ACTIVE
        CustomerMeResponse.ProductInstance.Status.BLOCKED -> Status.BLOCKED
        CustomerMeResponse.ProductInstance.Status.DEACTIVATING -> Status.DEACTIVATING
        CustomerMeResponse.ProductInstance.Status.DEACTIVATED -> Status.DEACTIVATED
        CustomerMeResponse.ProductInstance.Status.CANCELED -> Status.CANCELED
        CustomerMeResponse.ProductInstance.Status.UNKNOWN -> Status.UNKNOWN
    }
}