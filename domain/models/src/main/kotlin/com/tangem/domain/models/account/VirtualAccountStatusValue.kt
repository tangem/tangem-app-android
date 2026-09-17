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
sealed class VirtualAccountStatusValue {

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

    fun copySealed(source: StatusSource): VirtualAccountStatusValue {
        return when (this) {
            is Issuance.Issuing -> copy(source = source)
            is Issuance.UnderReview -> copy(source = source)
            is Issuance.VerificationDeclined -> copy(source = source)
            is Issuance.Activating -> copy(source = source)
            is Issued.Active -> copy(source = source)
            is Issued.Deactivated -> copy(source = source)
            is Loading,
            is Empty,
            is NotCreated,
            is Issuance.CountryNotSupported,
            is Error,
            -> this
        }
    }

    fun copySealed(balance: Balance?): VirtualAccountStatusValue {
        return when (this) {
            is Issuance.Issuing -> copy(balance = balance)
            is Issuance.UnderReview -> copy(balance = balance)
            is Issuance.VerificationDeclined -> copy(balance = balance)
            is Issuance.Activating -> copy(balance = balance)
            is Issued.Active -> copy(balance = balance)
            is Issued.Deactivated -> copy(balance = balance)
            is Loading,
            is Empty,
            is NotCreated,
            is Issuance.CountryNotSupported,
            is Error,
            -> this
        }
    }

    @Serializable
    data object Empty : VirtualAccountStatusValue() {
        override val source: StatusSource = StatusSource.ACTUAL
        override val balance: Balance? = null
    }

    @Serializable
    data object Loading : VirtualAccountStatusValue() {
        override val source: StatusSource = StatusSource.ACTUAL
        override val balance: Balance? = null
    }

    @Serializable
    data object NotCreated : VirtualAccountStatusValue() {
        override val source: StatusSource = StatusSource.ACTUAL
        override val balance: Balance? = null
    }

    @Serializable
    sealed class Issuance : VirtualAccountStatusValue() {

        abstract val customerId: String?

        @Serializable
        data class Issuing(
            @SerialName("source") override val source: StatusSource,
            @SerialName("balance") override val balance: Balance?,
            @SerialName("customer_id") override val customerId: String?,
            @SerialName("step") val step: IssuingStep,
        ) : Issuance()

        @Serializable
        data class UnderReview(
            @SerialName("source") override val source: StatusSource,
            @SerialName("balance") override val balance: Balance?,
            @SerialName("customer_id") override val customerId: String,
            @SerialName("kyc_status") val kycStatus: KycStatus,
        ) : Issuance()

        @Serializable
        data class VerificationDeclined(
            @SerialName("source") override val source: StatusSource,
            @SerialName("balance") override val balance: Balance?,
            @SerialName("customer_id") override val customerId: String,
        ) : Issuance()

        @Serializable
        data object CountryNotSupported : Issuance() {
            override val source: StatusSource = StatusSource.ACTUAL
            override val balance: Balance? = null
            override val customerId: String? = null
        }

        @Serializable
        data class Activating(
            @SerialName("source") override val source: StatusSource,
            @SerialName("balance") override val balance: Balance?,
            @SerialName("customer_id") override val customerId: String?,
        ) : Issuance()
    }

    @Serializable
    sealed class Issued : VirtualAccountStatusValue() {

        abstract val customerId: String
        abstract val productInstanceId: String

        @Serializable
        data class Active(
            @SerialName("source") override val source: StatusSource,
            @SerialName("balance") override val balance: Balance?,
            @SerialName("customer_id") override val customerId: String,
            @SerialName("product_instance_id") override val productInstanceId: String,
        ) : Issued()

        @Serializable
        data class Deactivated(
            @SerialName("source") override val source: StatusSource,
            @SerialName("balance") override val balance: Balance?,
            @SerialName("customer_id") override val customerId: String,
            @SerialName("product_instance_id") override val productInstanceId: String,
        ) : Issued()
    }

    @Serializable
    sealed class Error : VirtualAccountStatusValue() {

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