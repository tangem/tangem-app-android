package com.tangem.core.ui.components.bottomsheets.chooseaddress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.tangem.core.ui.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.res.TangemTheme

@Composable
fun ChooseAddressBottomSheet(config: TangemBottomSheetConfig) {
    if (config.content is ChooseAddressBottomSheetConfig && config.isShow) {
        TangemBottomSheet(config) { content ->
            ChooseAddressBottomSheetContent(
                content = content as ChooseAddressBottomSheetConfig,
            )
        }
    }
}

@Composable
private fun ChooseAddressBottomSheetContent(content: ChooseAddressBottomSheetConfig) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = TangemTheme.dimens.spacing24,
                top = TangemTheme.dimens.spacing24,
                end = TangemTheme.dimens.spacing24,
                bottom = TangemTheme.dimens.spacing16,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing24),
    ) {
        Text(
            text = stringResource(id = R.string.token_details_choose_address),
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
            style = TangemTheme.typography.body2,
        )
        content.addressModels.forEach { addressModel ->
            SecondaryButton(
                text = addressModel.type.name,
                onClick = { content.onClick(addressModel) },
            )
        }
    }
}