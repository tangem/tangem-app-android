package com.tangem.features.swap.v2.impl.sendviaswap.confirm.model

import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import java.math.BigDecimal

internal fun expressProvider(
    type: ExpressProviderType = ExpressProviderType.CEX,
    providerId: String = "p1",
    name: String = "Test Provider",
): ExpressProvider = ExpressProvider(
    providerId = providerId,
    name = name,
    type = type,
    imageLarge = "",
    termsOfUse = null,
    privacyPolicy = null,
    slippage = null,
)

internal fun contentQuote(
    fromAmount: BigDecimal,
    toAmount: BigDecimal,
    provider: ExpressProvider = expressProvider(),
    quoteId: String = "quote-id",
): SwapQuoteUM.Content = SwapQuoteUM.Content(
    provider = provider,
    toAmount = toAmount,
    fromAmount = fromAmount,
    toAmountValue = TextReference.EMPTY,
    fromAmountValue = TextReference.EMPTY,
    diffPercent = SwapQuoteUM.Content.DifferencePercent.Empty,
    isSingleProvider = true,
    rate = TextReference.EMPTY,
    quoteId = quoteId,
)

/** Shared parameterized-test case for the DEX/DEX_BRIDGE/ONRAMP rejection scenarios. */
internal data class NonCexProviderTypeCase(val providerType: ExpressProviderType) {
    override fun toString(): String = providerType.name
}

internal val nonCexProviderTypeCases: List<NonCexProviderTypeCase> = listOf(
    ExpressProviderType.DEX,
    ExpressProviderType.DEX_BRIDGE,
    ExpressProviderType.ONRAMP,
).map(::NonCexProviderTypeCase)