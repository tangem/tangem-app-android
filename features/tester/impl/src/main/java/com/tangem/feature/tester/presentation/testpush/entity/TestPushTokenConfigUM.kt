package com.tangem.feature.tester.presentation.testpush.entity

import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.models.network.Network
import kotlinx.collections.immutable.ImmutableList

internal data class TestPushTokenConfigUM(
    val itemList: ImmutableList<ManagedCryptoCurrency>,
    val onItemClick: (ManagedCryptoCurrency, Network) -> Unit,
    val searchValue: TextFieldValue,
    val onSearchValueEdit: (TextFieldValue) -> Unit,
) : TangemBottomSheetConfigContent