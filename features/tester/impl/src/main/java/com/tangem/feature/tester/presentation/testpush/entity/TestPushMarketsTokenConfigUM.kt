package com.tangem.feature.tester.presentation.testpush.entity

import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.domain.markets.TokenMarket
import kotlinx.collections.immutable.ImmutableList

internal data class TestPushMarketsTokenConfigUM(
    val itemList: ImmutableList<TokenMarket>,
    val onItemClick: (TokenMarket) -> Unit,
    val searchValue: TextFieldValue,
    val onSearchValueEdit: (TextFieldValue) -> Unit,
) : TangemBottomSheetConfigContent