package com.tangem.features.swap.v2.impl.chooseprovider.ui.preview

import com.tangem.core.ui.components.audits.AuditLabelUM
import com.tangem.core.ui.components.provider.entity.ProviderChooseUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.features.swap.v2.impl.R
import com.tangem.features.swap.v2.impl.chooseprovider.entity.SwapChooseProviderBottomSheetContent
import com.tangem.features.swap.v2.impl.chooseprovider.entity.SwapProviderListItem
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

internal object SwapChooseProviderContentPreview {

    private val provider1 = ExpressProvider(
        providerId = "changenow",
        rateTypes = listOf(ExpressRateType.Float),
        name = "ChangeNow",
        type = ExpressProviderType.CEX,
        imageLarge = "",
        termsOfUse = "",
        privacyPolicy = "",
        isRecommended = true,
        slippage = BigDecimal.ZERO,
    )
    private val provider2 = provider1.copy(
        providerId = "changelly",
        name = "Changelly",
    )

    private val quote1 = SwapQuoteUM.Content(
        provider = provider1,
        quoteAmount = "123".toBigDecimal(),
        quoteAmountValue = stringReference("123"),
        diffPercent = SwapQuoteUM.Content.DifferencePercent.Best,
    )

    private val quote2 = SwapQuoteUM.Content(
        provider = provider2,
        quoteAmount = "13.12".toBigDecimal(),
        quoteAmountValue = stringReference("13.12"),
        diffPercent = SwapQuoteUM.Content.DifferencePercent.Empty,
    )

    val state = SwapChooseProviderBottomSheetContent(
        providerList = persistentListOf(
            SwapProviderListItem(
                providerUM = ProviderChooseUM(
                    title = stringReference(provider1.name),
                    subtitle = stringReference(provider1.type.typeName),
                    infoText = stringReference("1800 POL"),
                    iconUrl = "",
                    isSelected = true,
                    extraUM = ProviderChooseUM.ExtraUM.Empty,
                    labelUM = ProviderChooseUM.LabelUM.Info(
                        AuditLabelUM(
                            text = resourceReference(R.string.express_provider_best_rate),
                            type = AuditLabelUM.Type.Info,
                        ),
                    ),
                ),
                quote = quote1,
            ),
            SwapProviderListItem(
                providerUM = ProviderChooseUM(
                    title = stringReference(provider2.name),
                    subtitle = stringReference(provider2.type.typeName),
                    infoText = stringReference("1800 POL"),
                    iconUrl = "",
                    isSelected = false,
                    extraUM = ProviderChooseUM.ExtraUM.Action(
                        text = resourceReference(R.string.express_provider_permission_needed),
                    ),
                    labelUM = ProviderChooseUM.LabelUM.Info(
                        AuditLabelUM(
                            text = resourceReference(R.string.express_provider_best_rate),
                            type = AuditLabelUM.Type.Info,
                        ),
                    ),
                ),
                quote = quote2,
            ),
        ),
        selectedProvider = provider1,
    )
}