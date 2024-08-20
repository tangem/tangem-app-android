package com.tangem.features.markets.details.impl.model.converters

import androidx.compose.runtime.Stable
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.markets.details.impl.ui.state.InfoBottomSheetContent
import com.tangem.features.markets.details.impl.ui.state.SecurityScoreUM
import com.tangem.features.markets.impl.R
import com.tangem.utils.converter.Converter

// TODO implement when backend is ready
@Stable
internal class SecurityScoreConverter(
    private val onInfoClick: (InfoBottomSheetContent) -> Unit,
) : Converter<Unit, SecurityScoreUM> {

    override fun convert(value: Unit): SecurityScoreUM {
        return with(value) {
            SecurityScoreUM(
                score = 4.7f,
                description = "Based on 3 ratings",
                onInfoClick = {
                    onInfoClick(
                        InfoBottomSheetContent(
                            title = resourceReference(R.string.markets_token_details_security_score),
                            body = stringReference("markets_token_details_security_score_description"),
                            // FIXME
                            // resourceReference(R.string.markets_token_details_security_score_description)
                        ),
                    )
                },
            )
        }
    }
}