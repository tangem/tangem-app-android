package com.tangem.features.earn.yield.impl

import com.tangem.common.routing.AppRoute
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.yieldSupplyKey
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.yield.supply.yieldSupply
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.features.earn.product.api.product.EarnRateEntry
import com.tangem.lib.crypto.BlockchainUtils
import java.math.BigDecimal
import javax.inject.Inject

internal class YieldModeRateMapper @Inject constructor() {

    fun entry(
        userWalletId: UserWalletId,
        status: CryptoCurrencyStatus,
        apyByKey: Map<String, BigDecimal>,
    ): EarnRateEntry? {
        val token = status.currency as? CryptoCurrency.Token ?: return null
        val isCaseInsensitive = BlockchainUtils.isCaseInsensitiveContractAddress(token.network.rawId)
        val apyPercent = apyByKey.entries
            .firstOrNull { (key, _) -> key.equals(token.yieldSupplyKey(), ignoreCase = isCaseInsensitive) }
            ?.value
            ?: return null

        return EarnRateEntry(
            rate = EarnProductRate(fraction = apyPercent.movePointLeft(PERCENT_POINT_SHIFT), type = EarnRateType.APY),
            isActive = status.value.yieldSupply?.isActive == true,
            source = status.value.sources.networkSource,
            route = AppRoute.YieldSupplyEntry(userWalletId, token, apyPercent.toPlainString()),
        )
    }

    private companion object {

        /** Shifting the point is exact, so every digit of the backend percent reaches the formatter. */
        const val PERCENT_POINT_SHIFT = 2
    }
}