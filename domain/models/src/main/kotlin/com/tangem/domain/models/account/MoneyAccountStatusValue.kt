package com.tangem.domain.models.account

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
sealed class MoneyAccountStatusValue {

    abstract val source: StatusSource

    abstract val balance: Balance?

    val totalFiatBalance: TotalFiatBalance
        get() {
            if (this is Loading) return TotalFiatBalance.Loading

            val balance = balance ?: return TotalFiatBalance.Loaded(amount = SerializedBigDecimal.ZERO, source = source)
            val fiatRate = balance.fiatRate ?: return TotalFiatBalance.Failed

            return TotalFiatBalance.Loaded(
                amount = balance.cryptoAmount.multiply(fiatRate),
                source = source,
            )
        }

    fun copySealed(source: StatusSource): MoneyAccountStatusValue {
        return when (this) {
            is Onboarding.Issuing -> copy(source = source)
            is Onboarding.UnderReview -> copy(source = source)
            is Onboarding.VerificationDeclined -> copy(source = source)
            is Onboarding.Activating -> copy(source = source)
            is Account.Active -> copy(source = source)
            is Account.Deactivated -> copy(source = source)
            is Loading,
            is Empty,
            is NotCreated,
            is Onboarding.CountryNotSupported,
            is Error,
            -> this
        }
    }

    @Serializable
    data object Empty : MoneyAccountStatusValue() {
        override val source: StatusSource = StatusSource.ACTUAL
        override val balance: Balance? = null
    }

    @Serializable
    data object Loading : MoneyAccountStatusValue() {
        override val source: StatusSource = StatusSource.ACTUAL
        override val balance: Balance? = null
    }

    @Serializable
    data object NotCreated : MoneyAccountStatusValue() {
        override val source: StatusSource = StatusSource.ACTUAL
        override val balance: Balance? = null
    }

    @Serializable
    sealed class Onboarding : MoneyAccountStatusValue() {

        abstract val customerId: String?

        @Serializable
        data class Issuing(
            @SerialName("source") override val source: StatusSource,
            @SerialName("balance") override val balance: Balance?,
            @SerialName("customer_id") override val customerId: String?,
            @SerialName("step") val step: IssuingStep,
        ) : Onboarding()

        @Serializable
        data class UnderReview(
            @SerialName("source") override val source: StatusSource,
            @SerialName("balance") override val balance: Balance?,
            @SerialName("customer_id") override val customerId: String,
            @SerialName("kyc_status") val kycStatus: KycStatus,
        ) : Onboarding()

        @Serializable
        data class VerificationDeclined(
            @SerialName("source") override val source: StatusSource,
            @SerialName("balance") override val balance: Balance?,
            @SerialName("customer_id") override val customerId: String,
        ) : Onboarding()

        @Serializable
        data object CountryNotSupported : Onboarding() {
            override val source: StatusSource = StatusSource.ACTUAL
            override val balance: Balance? = null
            override val customerId: String? = null
        }

        @Serializable
        data class Activating(
            @SerialName("source") override val source: StatusSource,
            @SerialName("balance") override val balance: Balance?,
            @SerialName("customer_id") override val customerId: String?,
        ) : Onboarding()
    }

    @Serializable
    sealed class Account : MoneyAccountStatusValue() {

        abstract val customerId: String
        abstract val productInstanceId: String

        @Serializable
        data class Active(
            @SerialName("source") override val source: StatusSource,
            @SerialName("balance") override val balance: Balance?,
            @SerialName("customer_id") override val customerId: String,
            @SerialName("product_instance_id") override val productInstanceId: String,
        ) : Account()

        @Serializable
        data class Deactivated(
            @SerialName("source") override val source: StatusSource,
            @SerialName("balance") override val balance: Balance?,
            @SerialName("customer_id") override val customerId: String,
            @SerialName("product_instance_id") override val productInstanceId: String,
        ) : Account()
    }

    @Serializable
    sealed class Error : MoneyAccountStatusValue() {

        @Serializable
        data object ExposedDevice : Error() {
            override val source: StatusSource = StatusSource.ACTUAL
            override val balance: Balance? = null
        }

        @Serializable
        data object Unavailable : Error() {
            override val source: StatusSource = StatusSource.ACTUAL
            override val balance: Balance? = null
        }

        @Serializable
        data object NotSynced : Error() {
            override val source: StatusSource = StatusSource.ACTUAL
            override val balance: Balance? = null
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
        @SerialName("crypto_currency") val cryptoCurrency: CryptoCurrency.Token,
        @SerialName("deposit_address") val depositAddress: String,
        @SerialName("currency_code") val currencyCode: String,
        @SerialName("fiat_rate") val fiatRate: SerializedBigDecimal?,
    ) {
        val cryptoCurrencyStatus: CryptoCurrencyStatus = CryptoCurrencyStatus(
            currency = cryptoCurrency,
            value = buildCryptoCurrencyStatusValue(
                amount = cryptoAmount,
                fiatRate = fiatRate,
                depositAddress = depositAddress,
            ),
        )
    }
}

private fun buildCryptoCurrencyStatusValue(
    amount: SerializedBigDecimal,
    fiatRate: SerializedBigDecimal?,
    depositAddress: String,
): CryptoCurrencyStatus.Value {
    val networkAddress = NetworkAddress.Single(
        defaultAddress = NetworkAddress.Address(
            type = NetworkAddress.Address.Type.Primary,
            value = depositAddress,
        ),
    )
    return if (fiatRate != null) {
        CryptoCurrencyStatus.Loaded(
            amount = amount,
            fiatAmount = amount.multiply(fiatRate),
            fiatRate = fiatRate,
            priceChange = BigDecimal.ZERO,
            networkAddress = networkAddress,
            sources = CryptoCurrencyStatus.Sources(),
            pendingTransactions = emptySet(),
            stakingBalance = null,
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
        )
    } else {
        CryptoCurrencyStatus.NoQuote(
            amount = amount,
            networkAddress = networkAddress,
            stakingBalance = null,
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            sources = CryptoCurrencyStatus.Sources(),
        )
    }
}