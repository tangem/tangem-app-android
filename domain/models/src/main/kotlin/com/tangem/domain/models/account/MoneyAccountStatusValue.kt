package com.tangem.domain.models.account

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
sealed class MoneyAccountStatusValue {
    abstract val source: StatusSource

    val totalFiatBalance: TotalFiatBalance
        get() = when (this) {
            is Empty,
            is NotCreated,
            is UnderReview,
            is Provisioning,
            is CountryNotSupported,
            is Error,
            -> TotalFiatBalance.Loaded(amount = SerializedBigDecimal.ZERO, source = source)
            is Loading -> TotalFiatBalance.Loading
            is Active -> {
                val rate = fiatRate ?: return TotalFiatBalance.Failed
                TotalFiatBalance.Loaded(amount = fiatBalance.availableBalance.multiply(rate), source = source)
            }
        }

    fun copySealed(source: StatusSource): MoneyAccountStatusValue {
        return when (this) {
            is UnderReview -> copy(source = source)
            is Provisioning -> copy(source = source)
            is Active -> copy(source = source)
            is Loading,
            is Empty,
            is NotCreated,
            is CountryNotSupported,
            is Error,
            -> this
        }
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
    data class UnderReview(
        override val source: StatusSource,
        val kycStatus: KycStatus,
        val customerId: String,
    ) : MoneyAccountStatusValue()

    /** Provisioned on the backend (e.g. via Rain) after KYC approval and terms acceptance. */
    @Serializable
    data class Provisioning(override val source: StatusSource) : MoneyAccountStatusValue()

    @Serializable
    data object CountryNotSupported : MoneyAccountStatusValue() {
        override val source: StatusSource = StatusSource.ACTUAL
    }

    /**
     * @property fiatRate Exchange rate of [cryptoCurrency] to the app's selected fiat currency,
     *                    or `null` if the quote is not yet available. When `null`,
     *                    [totalFiatBalance] resolves to [TotalFiatBalance.Failed].
     */
    @Serializable
    data class Active(
        override val source: StatusSource,
        val customerId: String,
        val currencyCode: String,
        val depositAddress: String?,
        val fiatBalance: FiatBalance,
        val cryptoBalance: CryptoBalance,
        val availableForWithdrawal: SerializedBigDecimal,
        val cryptoCurrency: CryptoCurrency.Token,
        val fiatRate: SerializedBigDecimal?,
    ) : MoneyAccountStatusValue() {
        val cryptoCurrencyStatus: CryptoCurrencyStatus = CryptoCurrencyStatus(
            currency = cryptoCurrency,
            value = buildCryptoCurrencyStatusValue(
                amount = availableForWithdrawal,
                fiatAmount = fiatBalance.availableBalance,
                fiatRate = fiatRate,
                depositAddress = cryptoBalance.depositAddress,
            ),
        )
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

    @Serializable
    data class FiatBalance(val availableBalance: SerializedBigDecimal, val currency: String)

    @Serializable
    data class CryptoBalance(
        val id: String,
        val chainId: Long,
        val depositAddress: String,
        val tokenContractAddress: String,
        val balance: SerializedBigDecimal,
    )
}

private fun buildCryptoCurrencyStatusValue(
    amount: SerializedBigDecimal,
    fiatAmount: SerializedBigDecimal,
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
            fiatAmount = fiatAmount,
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