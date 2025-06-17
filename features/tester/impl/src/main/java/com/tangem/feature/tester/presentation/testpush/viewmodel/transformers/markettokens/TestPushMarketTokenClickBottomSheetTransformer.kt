package com.tangem.feature.tester.presentation.testpush.viewmodel.transformers.markettokens

import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.common.routing.DeepLinkRoute
import com.tangem.core.deeplink.DEEPLINK_KEY
import com.tangem.domain.markets.TokenMarket
import com.tangem.feature.tester.presentation.testpush.entity.TestPushUM
import com.tangem.utils.transformer.Transformer

internal class TestPushMarketTokenClickBottomSheetTransformer(
    private val tokenMarket: TokenMarket,
) : Transformer<TestPushUM> {
    override fun transform(prevState: TestPushUM): TestPushUM {
        return prevState.copy(
            data = prevState.data.plus(
                TextFieldValue(DEEPLINK_KEY) to TextFieldValue(
                    buildString {
                        append("tangem://")
                        append(DeepLinkRoute.MarketTokenDetail.host)
                        append("?token_id=")
                        append(tokenMarket.id)
                        append("&type=promo")
                    },
                ),
            ),
        )
    }
}