package com.tangem.features.markets.details.impl.model.converters

import androidx.compose.runtime.Stable
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.features.markets.details.impl.ui.state.InfoBottomSheetContent
import com.tangem.features.markets.details.impl.ui.state.MarketsTokenDetailsUM
import com.tangem.features.markets.impl.R
import com.tangem.utils.converter.Converter

@Stable
internal class DescriptionConverter(
    private val onReadModeClicked: (InfoBottomSheetContent) -> Unit,
    private val onGeneratedAINotificationClick: () -> Unit,
) : Converter<TokenMarketInfo, MarketsTokenDetailsUM.Description?> {

    override fun convert(value: TokenMarketInfo): MarketsTokenDetailsUM.Description? {
        return value.shortDescription?.let { desc ->
            MarketsTokenDetailsUM.Description(
                shortDescription = stringReference(desc),
                fullDescription = value.fullDescription?.let { fullDescription ->
                    stringReference(fullDescription)
                },
                onReadMoreClick = {
                    onReadModeClicked(
                        InfoBottomSheetContent(
                            title = resourceReference(
                                R.string.markets_token_details_about_token_title,
                                wrappedList(
                                    value.name,
                                ),
                            ),
                            body = stringReference(value.fullDescription ?: ""),
                            generatedAINotificationUM = InfoBottomSheetContent.GeneratedAINotificationUM(
                                onClick = onGeneratedAINotificationClick,
                            ),
                        ),
                    )
                },
            )
        }
    }
}