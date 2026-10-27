package com.tangem.tap.features.details.ui.appcurrency

import android.content.res.Configuration
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.event.EventEffect
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.AppCurrencySelectorScreenTestTags
import com.tangem.tap.features.details.ui.appcurrency.AppCurrencySelectorState.Currency
import com.tangem.wallet.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppCurrencySelectorScreen(state: AppCurrencySelectorState, modifier: Modifier = Modifier) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val listState = rememberLazyListState()

    Scaffold(
        modifier = modifier
            .imePadding()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = TangemTheme.colors3.bg.primary,
        topBar = {
            TopBar(
                modifier = Modifier.fillMaxWidth(),
                scrollBehavior = scrollBehavior,
                state = state,
            )
        },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.navigationBars),
        content = { paddingValues ->
            val contentModifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()

            when (state) {
                is AppCurrencySelectorState.Loading -> LoadingList(
                    modifier = contentModifier,
                )
                is AppCurrencySelectorState.Content -> CurrenciesList(
                    modifier = contentModifier,
                    listState = listState,
                    currencies = state.items,
                    selectedId = state.selectedId,
                    onCurrencyClick = state.onCurrencyClick,
                )
            }
        },
    )

    if (state is AppCurrencySelectorState.Content) {
        EventEffect(event = state.scrollToSelected) {
            listState.scrollToItem(it)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    state: AppCurrencySelectorState,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarColors,
        navigationIcon = {
            IconButton(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(32.dp),
                onClick = state.onBackClick,
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(id = R.drawable.ic_back_24),
                    contentDescription = null,
                )
            }
        },
        title = {
            when (state) {
                is AppCurrencySelectorState.Search -> SearchBar(
                    modifier = Modifier.fillMaxWidth(),
                    onInputChange = state.onSearchInputChange,
                )
                is AppCurrencySelectorState.Loading,
                is AppCurrencySelectorState.Default,
                -> Text(
                    text = stringResourceSafe(id = R.string.details_row_title_currency),
                    style = TangemTheme.typography.subtitle1,
                )
            }
        },
        actions = {
            when (state) {
                is AppCurrencySelectorState.Content -> {
                    IconButton(
                        modifier = Modifier
                            .size(32.dp)
                            .testTag(AppCurrencySelectorScreenTestTags.TOP_BAR_ACTION_BUTTON),
                        onClick = state.onTopBarActionClick,
                    ) {
                        val iconResId = when (state) {
                            is AppCurrencySelectorState.Default -> R.drawable.ic_search_24
                            is AppCurrencySelectorState.Search -> R.drawable.ic_close_24
                        }
                        val iconTint = when (state) {
                            is AppCurrencySelectorState.Default -> TangemTheme.colors3.icon.primary
                            is AppCurrencySelectorState.Search -> TangemTheme.colors3.icon.secondary
                        }

                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(id = iconResId),
                            tint = iconTint,
                            contentDescription = null,
                        )
                    }
                }
                is AppCurrencySelectorState.Loading -> Unit
            }
            SpacerW(width = 8.dp)
        },
    )
}

@Composable
private fun SearchBar(onInputChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    var input by remember { mutableStateOf(value = "") }

    TextField(
        modifier = modifier
            .focusRequester(focusRequester)
            .testTag(AppCurrencySelectorScreenTestTags.SEARCH_FIELD),
        value = input,
        onValueChange = { input = it },
        singleLine = true,
        textStyle = TangemTheme.typography.subtitle2,
        placeholder = {
            Text(text = stringResourceSafe(id = R.string.common_search))
        },
        colors = SearchBarColors,
    )

    LaunchedEffect(key1 = input) {
        onInputChange(input)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun LoadingList(modifier: Modifier = Modifier) {
    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }
    Column(modifier = modifier) {
        repeat(times = 10) {
            Row(
                modifier = Modifier
                    .padding(
                        start = 16.dp,
                        end = 24.dp,
                    )
                    .height(56.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    space = 16.dp,
                    alignment = Alignment.Start,
                ),
            ) {
                CircleShimmer(modifier = Modifier.size(24.dp))
                RectangleShimmer(
                    modifier = Modifier
                        .height(24.dp)
                        .fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(bottomBarHeight))
    }
}

@Composable
private fun CurrenciesList(
    listState: LazyListState,
    currencies: ImmutableList<Currency>,
    selectedId: String,
    onCurrencyClick: (Currency) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }
    LazyColumn(
        modifier = modifier.testTag(AppCurrencySelectorScreenTestTags.LAZY_LIST),
        state = listState,
        contentPadding = PaddingValues(bottom = bottomBarHeight),
    ) {
        items(
            items = currencies,
            key = Currency::id,
        ) { currency ->
            val onClick = remember(key1 = currency) {
                { onCurrencyClick(currency) }
            }

            CurrencyItem(
                modifier = Modifier.fillMaxWidth(),
                name = currency.name,
                isSelected = currency.id == selectedId,
                onClick = onClick,
            )
        }
    }
}

@Composable
private fun CurrencyItem(name: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .testTag(AppCurrencySelectorScreenTestTags.CURRENCY_ITEM)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .padding(
                start = 16.dp,
                end = 24.dp,
            )
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            space = 16.dp,
            alignment = Alignment.Start,
        ),
    ) {
        RadioButton(
            modifier = Modifier.size(24.dp),
            selected = isSelected,
            onClick = onClick,
            interactionSource = interactionSource,
            colors = RadioButtonColors,
        )
        Text(
            text = name,
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors3.text.primary,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private val TopAppBarColors: TopAppBarColors
    @Composable
    get() = TopAppBarDefaults.topAppBarColors(
        containerColor = TangemTheme.colors3.bg.primary,
        // Currently (08.09.23) it's not working when scrolling programmatically
        scrolledContainerColor = TangemTheme.colors3.bg.primary,
        navigationIconContentColor = TangemTheme.colors3.icon.primary,
        titleContentColor = TangemTheme.colors3.text.primary,
        actionIconContentColor = TangemTheme.colors3.icon.primary,
    )

private val SearchBarColors: TextFieldColors
    @Composable
    get() = TextFieldDefaults.colors(
        unfocusedContainerColor = TangemTheme.colors3.bg.primary,
        focusedContainerColor = TangemTheme.colors3.bg.primary,
        focusedTextColor = TangemTheme.colors3.text.primary,
        unfocusedTextColor = TangemTheme.colors3.text.secondary,
        focusedPlaceholderColor = TangemTheme.colors3.text.tertiary,
        unfocusedPlaceholderColor = TangemTheme.colors3.text.tertiary,
        focusedIndicatorColor = TangemTheme.colors3.bg.primary,
        unfocusedIndicatorColor = TangemTheme.colors3.bg.primary,
        cursorColor = TangemTheme.colors3.icon.primary,
    )

private val RadioButtonColors: RadioButtonColors
    @Composable
    get() = RadioButtonDefaults.colors(
        selectedColor = TangemTheme.colors3.icon.accent.blue,
        unselectedColor = TangemTheme.colors3.icon.secondary,
    )

// region Preview
@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Preview(showBackground = true, widthDp = 360, heightDp = 720, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppCurrencySelectorScreenPreview(
    @PreviewParameter(AppCurrencySelectorStateProvider::class) param: AppCurrencySelectorState,
) {
    TangemThemePreview {
        AppCurrencySelectorScreen(param)
    }
}

private class AppCurrencySelectorStateProvider : CollectionPreviewParameterProvider<AppCurrencySelectorState>(
    collection = buildList {
        val items = listOf(
            "US Dollar (USD) – $",
            "Inited Arab Emirates Dirham (AED) – DH",
            "Argentine Peso (ARS) – $",
            "Australian Dollar (AUD) – A$",
            "Bangladeshi Taka (BDT) – ৳",
            "Bahraini Dinar (BHD) – BD",
            "Bermudian Dollar (BMD) – $",
            "Brazil Real (BRL) – R$",
            "Canadian Dollar (CAD) – CA$",
            "Swiss Franc (CHF) – Fr",
            "Chilean Peso (CLP) – CLP$",
            "Chinese Yan (CNY)",
        )
            .mapIndexed { index, s -> Currency(index.toString(), s) }
            .toPersistentList()

        add(AppCurrencySelectorState.Loading(onBackClick = {}))

        add(
            AppCurrencySelectorState.Default(
                selectedId = "0",
                items = items,
                scrollToSelected = consumedEvent(),
                onCurrencyClick = {},
                onBackClick = {},
                onTopBarActionClick = {},
            ),
        )

        add(
            AppCurrencySelectorState.Search(
                selectedId = "0",
                items = items,
                scrollToSelected = consumedEvent(),
                onCurrencyClick = {},
                onBackClick = {},
                onSearchInputChange = {},
                onTopBarActionClick = {},
            ),
        )
    },
)
// endregion Preview