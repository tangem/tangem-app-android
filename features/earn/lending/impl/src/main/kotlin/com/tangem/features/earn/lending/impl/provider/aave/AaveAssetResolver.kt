package com.tangem.features.earn.lending.impl.provider.aave

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.isJoint
import com.tangem.domain.yield.supply.YieldSupplyWrappedNativeFactory
import com.tangem.features.earn.lending.impl.provider.LendingAsset
import javax.inject.Inject

internal interface AaveAssetResolver {

    /** @return null when Lending on Aave is not offered for [currency] */
    fun resolve(currency: CryptoCurrency): LendingAsset?
}

internal class DefaultAaveAssetResolver @Inject constructor(
    private val wrappedNativeFactory: YieldSupplyWrappedNativeFactory,
) : AaveAssetResolver {

    override fun resolve(currency: CryptoCurrency): LendingAsset? {
        // A joint account is a Safe contract and owns no Yield Module
        if (currency !is CryptoCurrency.Coin || currency.isJoint) return null
        val wrappedToken = wrappedNativeFactory.create(currency.network) ?: return null

        return LendingAsset(currency = currency, wrappedToken = wrappedToken)
    }
}

/** The Aave reserve the deposit goes to: the wrapped token for a coin, the token itself otherwise */
internal val LendingAsset.reserveToken: CryptoCurrency.Token
    get() = checkNotNull(
        wrappedToken ?: currency as? CryptoCurrency.Token,
    ) { "Aave coin without a wrapper: ${currency.id}" }