package com.tangem.data.pay.util

import arrow.core.getOrElse
import com.tangem.data.pay.converter.TangemPayTariffPlanConverter
import com.tangem.datasource.api.pay.models.response.CryptoBalance
import com.tangem.datasource.api.pay.models.response.CustomerMeResponse
import com.tangem.datasource.api.pay.models.response.FiatBalance
import com.tangem.domain.models.account.CardDisplayName
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.pay.TangemPayCardLimit
import com.tangem.domain.models.pay.TangemPayCardLimitPeriod
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.CustomerInfo.CardInfo
import com.tangem.domain.pay.model.CustomerInfo.ProductInstance
import com.tangem.domain.pay.model.CustomerInfo.ProductInstance.SpecificationDataType
import com.tangem.domain.pay.model.CustomerInfo.ProductInstance.Status
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.orZero
import org.joda.time.DateTime

internal object CustomerInfoConverter : Converter<CustomerMeResponse.Result, CustomerInfo> {
    override fun convert(value: CustomerMeResponse.Result): CustomerInfo {
        val kycStatus = KycStatus.fromString(status = value.kyc?.status)
        val fiatBalance = value.balance?.fiat
        val cryptoBalance = value.balance?.crypto

        val productInstances = value.productInstances.map { it.toDomain() }
        val cards = if (value.paymentAccount == null || value.balance == null) {
            emptyList()
        } else {
            value.cards.mapIndexed { index, cardWire ->
                // Legacy single-card has no card_id on the card object → join to the single product instance.
                val cardId = cardWire.cardId ?: value.productInstances.getOrNull(index)?.cardId.orEmpty()
                buildCardInfo(cardId = cardId, card = cardWire)
            }
        }

        return CustomerInfo(
            customerId = value.id,
            productInstances = productInstances,
            cards = cards,
            kycStatus = kycStatus,
            state = CustomerInfo.State.fromString(value.state),
            fiatBalance = fiatBalance?.toDomain(),
            cryptoBalance = cryptoBalance?.toDomain(),
            availableForWithdrawal = value.balance?.availableForWithdrawal?.amount.orZero(),
            tariffPlan = value.customerTariffPlan?.toDomain(),
        )
    }

    private fun CustomerMeResponse.CustomerTariffPlan.toDomain(): TangemPayCustomerTariffPlan? {
        val plan = tariffPlan?.toDomain() ?: return null
        return TangemPayCustomerTariffPlan(
            status = TangemPayCustomerTariffPlan.Status.fromString(status),
            plan = plan,
            nextBillingAt = nextBillingAt.toDateTimeOrNull(),
            pendingPlan = pendingTariffPlan?.toDomain(),
            pendingTransitionAt = pendingTransitionAt.toDateTimeOrNull(),
        )
    }

    private fun CustomerMeResponse.TariffPlan.toDomain(): TangemPayTariffPlan? =
        TangemPayTariffPlanConverter.convert(this)

    private fun CustomerMeResponse.ProductInstance.toDomain(): ProductInstance {
        val status = status.toDomain()
        val cardFrozenState = when (status) {
            Status.ACTIVE -> TangemPayCardFrozenState.Unfrozen
            else -> TangemPayCardFrozenState.Frozen
        }
        val name = displayName?.ifEmpty { null }
        return ProductInstance(
            id = id,
            cardId = cardId,
            frozenState = cardFrozenState,
            status = status,
            displayName = if (name != null) CardDisplayName(name).getOrElse { null } else null,
            actualCardLimit = actualCardLimit?.parseCardLimit(),
            adminCardLimit = adminCardLimit?.parseCardLimit(),
            specificationDataType = specificationDataType.toDomain(),
        )
    }

    private fun buildCardInfo(cardId: String, card: CustomerMeResponse.Card): CardInfo {
        return CardInfo(
            cardId = cardId,
            cardStatus = TangemPayCard.Status.fromString(card.cardStatus),
            lastFourDigits = card.cardNumberEnd,
            isPinSet = card.isPinSet == true,
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

    private fun CustomerMeResponse.ProductInstance.SpecificationDataType.toDomain(): SpecificationDataType =
        when (this) {
            CustomerMeResponse.ProductInstance.SpecificationDataType.ACCOUNT -> SpecificationDataType.ACCOUNT
            CustomerMeResponse.ProductInstance.SpecificationDataType.CARD -> SpecificationDataType.CARD
        }

    private fun String?.toDateTimeOrNull(): DateTime? = this?.let { runCatching { DateTime.parse(it) }.getOrNull() }
}