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
    private val wethFactory: YieldSupplyWrappedNativeFactory,
) : AaveAssetResolver {

    override fun resolve(currency: CryptoCurrency): LendingAsset? {
        // A joint account is a Safe contract and owns no Yield Module
        if (currency !is CryptoCurrency.Coin || currency.isJoint) return null
        val weth = wethFactory.create(currency.network) ?: return null

        return LendingAsset(currency = currency, wrappedToken = weth)
    }
}

/** The reserve in the module is held in WETH, so an Aave asset always has it */
internal val LendingAsset.weth: CryptoCurrency.Token
    get() = checkNotNull(wrappedToken) { "Aave asset without WETH: ${currency.id}" }