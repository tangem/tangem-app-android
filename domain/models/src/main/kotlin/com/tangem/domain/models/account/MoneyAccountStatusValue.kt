package com.tangem.domain.models.account

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class MoneyAccountStatusValue {

    abstract val source: StatusSource

    val totalFiatBalance: TotalFiatBalance
        get() {
            if (this is Loading) return TotalFiatBalance.Loading

            val balance = (this as? Account)?.balance
                ?: return TotalFiatBalance.Loaded(amount = SerializedBigDecimal.ZERO, source = source)
            val fiatRate = balance.fiatRate ?: return TotalFiatBalance.Failed

            return TotalFiatBalance.Loaded(
                amount = balance.cryptoAmount.multiply(fiatRate),
                source = source,
            )
        }

    @Serializable
    data object Empty : MoneyAccountStatusValue() {
        override val source: StatusSource = StatusSource.ACTUAL
    }

    @Serializable
    data object Loading : MoneyAccountStatusValue() {
        override val source: StatusSource = StatusSource.ACTUAL
    }

    @Serializable
    data object NotCreated : MoneyAccountStatusValue() {
        override val source: StatusSource = StatusSource.ACTUAL
    }

    @Serializable
    sealed class Onboarding : MoneyAccountStatusValue() {

        abstract val customerId: String

        @Serializable
        data class Issuing(
            @SerialName("source") override val source: StatusSource,
            @SerialName("customer_id") override val customerId: String,
            @SerialName("step") val step: IssuingStep,
        ) : Onboarding()

        @Serializable
        data class KycRequired(
            @SerialName("source") override val source: StatusSource,
            @SerialName("customer_id") override val customerId: String,
        ) : Onboarding()

        @Serializable
        data class UnderReview(
            @SerialName("source") override val source: StatusSource,
            @SerialName("customer_id") override val customerId: String,
            @SerialName("kyc_status") val kycStatus: KycStatus,
        ) : Onboarding()

        @Serializable
        data class VerificationDeclined(
            @SerialName("source") override val source: StatusSource,
            @SerialName("customer_id") override val customerId: String,
        ) : Onboarding()

        @Serializable
        data class CountryNotSupported(
            @SerialName("source") override val source: StatusSource,
            @SerialName("customer_id") override val customerId: String,
            @SerialName("countryCode") val countryCode: String?,
        ) : Onboarding()

        @Serializable
        data class Activating(
            @SerialName("source") override val source: StatusSource,
            @SerialName("customer_id") override val customerId: String,
        ) : Onboarding()
    }

    @Serializable
    sealed class Account : MoneyAccountStatusValue() {

        abstract val customerId: String
        abstract val cryptoCurrency: CryptoCurrency.Token
        abstract val balance: Balance?

        val cryptoCurrencyStatus: CryptoCurrencyStatus?
            get() = balance?.let { accountBalance ->
                CryptoCurrencyStatus(
                    currency = cryptoCurrency,
                    value = depositCryptoCurrencyStatusValue(
                        amount = accountBalance.cryptoAmount,
                        fiatRate = accountBalance.fiatRate,
                        depositAddress = accountBalance.depositAddress,
                        source = source,
                    ),
                )
            }

        @Serializable
        data class Active(
            @SerialName("source") override val source: StatusSource,
            @SerialName("crypto_currency") override val cryptoCurrency: CryptoCurrency.Token,
            @SerialName("balance") override val balance: Balance?,
            @SerialName("customer_id") override val customerId: String,
        ) : Account()

        @Serializable
        data class Deactivated(
            @SerialName("source") override val source: StatusSource,
            @SerialName("crypto_currency") override val cryptoCurrency: CryptoCurrency.Token,
            @SerialName("balance") override val balance: Balance?,
            @SerialName("customer_id") override val customerId: String,
        ) : Account()
    }

    @Serializable
    sealed class Error : MoneyAccountStatusValue() {

        @Serializable
        data object ExposedDevice : Error() {
            override val source: StatusSource = StatusSource.ACTUAL
        }

        @Serializable
        data object Unavailable : Error() {
            override val source: StatusSource = StatusSource.ACTUAL
        }

        @Serializable
        data object NotSynced : Error() {
            override val source: StatusSource = StatusSource.ACTUAL
        }
    }

    enum class IssuingStep {
        STARTING,
        INITIALIZATION,
        ELIGIBILITY_CHECK,
        RAIN_APPROVAL,
        UNKNOWN,
    }

    @Serializable
    data class Balance(
        @SerialName("crypto_amount") val cryptoAmount: SerializedBigDecimal,
        @SerialName("deposit_address") val depositAddress: String,
        @SerialName("currency_code") val currencyCode: String,
        @SerialName("fiat_rate") val fiatRate: SerializedBigDecimal?,
    )
}