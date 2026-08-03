package com.tangem.features.tangempay.orderCard.impl.ui

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.UnableToLoadData
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.orderCard.impl.ui.components.PhoneVisualTransformation
import com.tangem.features.tangempay.orderCard.impl.ui.components.TextInput
import com.tangem.features.tangempay.orderCard.impl.ui.components.TextInputState
import com.tangem.features.tangempay.orderCard.impl.ui.state.OrderFieldError
import com.tangem.features.tangempay.orderCard.impl.ui.state.TangemPayOrderCardDataScreenUM
import com.tangem.features.tangempay.orderCard.impl.ui.state.TangemPayOrderCardDataScreenUM.Error
import com.tangem.features.tangempay.orderCard.impl.ui.state.TangemPayOrderCardDataScreenUM.FieldUM
import com.tangem.features.tangempay.orderCard.impl.ui.state.TangemPayOrderCardDataScreenUM.Form
import com.tangem.features.tangempay.orderCard.impl.ui.state.TangemPayOrderCardDataScreenUM.Loading

@Composable
internal fun TangemPayOrderCardDataScreen(state: TangemPayOrderCardDataScreenUM, modifier: Modifier = Modifier) {
    TangemTopBarScaffold(
        modifier = modifier,
        topBar = {
            TangemTopNavigation(
                title = resourceReference(R.string.tangempay_order_type_title),
                subtitle = resourceReference(R.string.tangempay_order_data_subtitle),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                onBack = state.onBackClick,
                onClose = state.onCloseClick,
            )
        },
    ) { contentPadding ->
        when (state) {
            is Loading -> CenteredContent(contentPadding = contentPadding) { TangemLoader() }
            is Error -> CenteredContent(contentPadding = contentPadding) {
                UnableToLoadData(onRetryClick = state.onRetry)
            }
            is Form -> OrderDataForm(state = state, contentPadding = contentPadding)
        }
    }
}

@Composable
private fun CenteredContent(contentPadding: PaddingValues, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun OrderDataForm(state: Form, contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current
    val submit = state.onOrderClick
    val onOrderClick: () -> Unit = remember(focusManager, submit) {
        {
            focusManager.clearFocus()
            submit()
        }
    }
    Column(modifier = modifier.fillMaxSize()) {
        OrderDataFields(
            state = state,
            contentPadding = contentPadding,
            onImeDone = focusManager::clearFocus,
            modifier = Modifier.weight(1f),
        )
        TangemButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding()
                .imePadding(),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            text = resourceReference(R.string.tangempay_order_data_order_button),
            isEnabled = state.isOrderEnabled,
            onClick = onOrderClick,
        )
    }
}

@Composable
private fun OrderDataFields(
    state: Form,
    contentPadding: PaddingValues,
    onImeDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = contentPadding.calculateTopPadding())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        SpacerH(4.dp)
        OrderDataField(label = R.string.tangempay_order_data_name_on_card, field = state.embossName)
        SpacerH(4.dp)
        OrderDataDisabledField(label = R.string.tangempay_order_data_country, value = state.country)
        OrderDataDisabledField(label = R.string.tangempay_order_data_email, value = state.email)
        OrderDataField(label = R.string.tangempay_order_data_first_name, field = state.firstName)
        OrderDataField(label = R.string.tangempay_order_data_last_name, field = state.lastName)
        OrderDataField(label = R.string.tangempay_order_data_region, field = state.region)
        OrderDataField(label = R.string.tangempay_order_data_city, field = state.city)
        OrderDataField(label = R.string.tangempay_order_data_address_line1, field = state.addressLine1)
        OrderDataField(label = R.string.tangempay_order_data_address_line2, field = state.addressLine2)
        OrderDataField(label = R.string.tangempay_order_data_postal_code, field = state.postalCode)
        OrderDataPhoneField(field = state.phone, mask = state.phoneMask, onImeDone = onImeDone)
        SpacerH(4.dp)
    }
}

@Composable
private fun OrderDataField(@StringRes label: Int, field: FieldUM, modifier: Modifier = Modifier) {
    TextInput(
        label = resourceReference(label),
        value = field.value,
        onValueChange = field.onValueChange,
        modifier = modifier.fillMaxWidth(),
        state = field.error.toInputState(),
        isRequired = field.isRequired,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
        onFocusChange = field.onFocusChange,
    )
}

@Composable
private fun OrderDataDisabledField(@StringRes label: Int, value: String, modifier: Modifier = Modifier) {
    TextInput(
        label = resourceReference(label),
        value = value,
        onValueChange = {},
        modifier = modifier.fillMaxWidth(),
        state = TextInputState.Disabled,
    )
}

@Composable
private fun OrderDataPhoneField(field: FieldUM, mask: String, onImeDone: () -> Unit, modifier: Modifier = Modifier) {
    val enteredColor = TangemTheme.colors3.text.primary
    val hintColor = TangemTheme.colors3.text.tertiary
    val transformation = remember(mask, enteredColor, hintColor) {
        PhoneVisualTransformation(mask = mask, enteredColor = enteredColor, hintColor = hintColor)
    }
    TextInput(
        label = resourceReference(R.string.tangempay_order_data_phone),
        value = field.value,
        onValueChange = field.onValueChange,
        modifier = modifier.fillMaxWidth(),
        state = field.error.toInputState(),
        isRequired = field.isRequired,
        visualTransformation = transformation,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onImeDone() }),
        onFocusChange = field.onFocusChange,
    )
}

private fun OrderFieldError?.toInputState(): TextInputState = when (this) {
    null -> TextInputState.Default
    OrderFieldError.Required -> TextInputState.Error(resourceReference(R.string.tangempay_order_data_field_required))
    OrderFieldError.Invalid -> TextInputState.Error(resourceReference(R.string.tangempay_order_data_field_invalid))
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "Light")
@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Dark",
)
@Composable
private fun TangemPayOrderCardDataScreenPreview(
    @PreviewParameter(OrderDataPreviewProvider::class) state: TangemPayOrderCardDataScreenUM,
) {
    TangemThemePreviewRedesign {
        TangemPayOrderCardDataScreen(state = state)
    }
}

private class OrderDataPreviewProvider : CollectionPreviewParameterProvider<TangemPayOrderCardDataScreenUM>(
    collection = listOf(
        previewForm(),
        previewForm(prefilled = true),
        previewForm(prefilled = true, withErrors = true),
        Loading(onBackClick = {}, onCloseClick = {}),
        Error(onBackClick = {}, onCloseClick = {}, onRetry = {}),
    ),
)

private fun previewForm(prefilled: Boolean = false, withErrors: Boolean = false): Form {
    val error = OrderFieldError.Invalid.takeIf { withErrors }
    return Form(
        onBackClick = {},
        onCloseClick = {},
        country = "US",
        email = "j.silverhand@gmail.com",
        phoneMask = "+1 (###) ###-####",
        embossName = previewField(if (prefilled) "JOHNNY SILVERHAND" else "", error),
        firstName = previewField(if (prefilled) "Johnny" else "", error),
        lastName = previewField(if (prefilled) "Silverhand" else "", error),
        region = previewField(if (prefilled) "California" else "", error),
        city = previewField(if (prefilled) "Night City" else "", error),
        addressLine1 = previewField(if (prefilled) "Crescent st. 24" else "", error),
        addressLine2 = previewField(if (prefilled) "Apt. 56" else "", null, isRequired = false),
        postalCode = previewField(if (prefilled) "0000" else "", error),
        phone = previewField(if (prefilled) "4155550123" else "", error),
        isOrderEnabled = prefilled && !withErrors,
        onOrderClick = {},
    )
}

private fun previewField(value: String, error: OrderFieldError?, isRequired: Boolean = true) = FieldUM(
    value = value,
    error = error,
    isRequired = isRequired,
    onValueChange = {},
    onFocusChange = {},
)