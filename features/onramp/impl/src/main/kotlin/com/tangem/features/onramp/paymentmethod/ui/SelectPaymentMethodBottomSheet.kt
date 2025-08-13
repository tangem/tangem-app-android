package com.tangem.features.onramp.paymentmethod.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.selectedBorder
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.SelectPaymentMethodBottomSheetTestTags
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.paymentmethod.entity.PaymentMethodUM
import com.tangem.features.onramp.paymentmethod.entity.PaymentMethodsBottomSheetConfig
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun SelectPaymentMethodBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet<PaymentMethodsBottomSheetConfig>(
        config = config,
        addBottomInsets = true,
        containerColor = TangemTheme.colors.background.primary,
        titleText = resourceReference(R.string.onramp_pay_with),
        content = { contentConfig ->
            SelectPaymentMethodBottomSheetContent(
                modifier = Modifier
                    .padding(horizontal = TangemTheme.dimens.spacing16)
                    .padding(top = TangemTheme.dimens.spacing12, bottom = TangemTheme.dimens.spacing24)
                    .fillMaxWidth()
                    .wrapContentHeight(),
                selectedMethodId = contentConfig.selectedMethodId,
                methods = contentConfig.paymentMethods,
            )
        },
    )
}

@Composable
private fun SelectPaymentMethodBottomSheetContent(
    selectedMethodId: String,
    methods: ImmutableList<PaymentMethodUM>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.testTag(SelectPaymentMethodBottomSheetTestTags.LAZY_LIST),
    ) {
        items(
            items = methods,
            key = { item -> item.id },
            contentType = { item -> item::class.java },
            itemContent = { item ->
                val isSelected = remember(item, selectedMethodId) {
                    item.id == selectedMethodId
                }
                val itemModifier = Modifier
                    .fillMaxWidth()
                    .selectedBorder(isSelected = isSelected)
                    .clip(RoundedCornerShape(TangemTheme.dimens.radius16))
                    .clickable(onClick = item.onSelect)
                    .padding(
                        start = TangemTheme.dimens.spacing12,
                        top = TangemTheme.dimens.spacing10,
                        bottom = TangemTheme.dimens.spacing10,
                    )
                PaymentMethodItem(
                    modifier = itemModifier,
                    paymentMethod = item,
                    isSelected = isSelected,
                )
            },
        )
    }
}

@Composable
private fun PaymentMethodItem(paymentMethod: PaymentMethodUM, isSelected: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        PaymentMethodIcon(
            imageUrl = paymentMethod.imageUrl,
            modifier = Modifier.testTag(SelectPaymentMethodBottomSheetTestTags.PAYMENT_METHOD_ICON),
        )
        Text(
            text = paymentMethod.name,
            style = TangemTheme.typography.subtitle2,
            color = if (isSelected) TangemTheme.colors.text.primary1 else TangemTheme.colors.text.secondary,
        )
    }
}