package com.tangem.feature.tester.presentation.testpush.viewmodel

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.GetMarketsTokenListFlowUseCase
import com.tangem.domain.markets.TokenListBatchingContext
import com.tangem.domain.markets.TokenMarketListConfig
import com.tangem.domain.markets.TokenMarketUpdateRequest
import com.tangem.feature.tester.presentation.navigation.InnerTesterRouter
import com.tangem.feature.tester.presentation.testpush.entity.TestPushClickIntents
import com.tangem.feature.tester.presentation.testpush.entity.TestPushMenuConfigUM
import com.tangem.feature.tester.presentation.testpush.entity.TestPushUM
import com.tangem.feature.tester.presentation.testpush.utils.TestPushPayloadParser
import com.tangem.feature.tester.presentation.testpush.viewmodel.transformers.*
import com.tangem.feature.tester.presentation.testpush.viewmodel.transformers.markettokens.TestPushMarketTokenBottomSheetTransformer
import com.tangem.feature.tester.presentation.testpush.viewmodel.transformers.markettokens.TestPushMarketTokenClickBottomSheetTransformer
import com.tangem.feature.tester.presentation.testpush.viewmodel.transformers.markettokens.TestPushMarketTokenSearchBottomSheetTransformer
import com.tangem.pagination.BatchAction
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.notifications.PushNotificationsTokenProvider
import com.tangem.features.tester.api.TestPushEmitter
import com.tangem.utils.transformer.update
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class TestPushViewModel @Inject constructor(
    private val pushNotificationsTokenProvider: PushNotificationsTokenProvider,
    private val testPushEmitter: TestPushEmitter,
    private val dispatchers: CoroutineDispatcherProvider,
    getMarketsTokenListFlowUseCase: GetMarketsTokenListFlowUseCase,
) : ViewModel(), TestPushClickIntents {

    private val _uiState = MutableStateFlow(
        TestPushUM(
            fcmToken = "",
            payload = TextFieldValue(""),
            payloadError = null,
            title = TextFieldValue(""),
            message = TextFieldValue(""),
            data = emptyList(),
            bottomSheetConfig = null,
        ),
    )
    val uiState: StateFlow<TestPushUM>
        get() = _uiState.asStateFlow()

    private var router: InnerTesterRouter? = null

    private val marketsJobHolder = JobHolder()
    private val marketsFlow = MutableSharedFlow<BatchAction<Int, TokenMarketListConfig, TokenMarketUpdateRequest>>()
    private val marketsLoader = getMarketsTokenListFlowUseCase(
        batchingContext = TokenListBatchingContext(
            actionsFlow = marketsFlow,
            coroutineScope = viewModelScope,
        ),
        batchFlowType = GetMarketsTokenListFlowUseCase.BatchFlowType.Main,
    )

    init {
        viewModelScope.launch {
            val fcmToken = pushNotificationsTokenProvider.getToken()
            _uiState.update(TestPushChangeFCMTokenTransformer(fcmToken))
        }
    }

    fun setupNavigation(router: InnerTesterRouter) {
        this.router = router
    }

    override fun onPayloadChange(value: TextFieldValue) {
        _uiState.update(TestPushChangePayloadTransformer(value))
    }

    override fun onTitleChange(value: TextFieldValue) {
        _uiState.update(TestPushChangeTitleTransformer(value))
    }

    override fun onMessageChange(value: TextFieldValue) {
        _uiState.update(TestPushChangeMessageTransformer(value))
    }

    override fun onDataKeyChange(index: Int, value: TextFieldValue) {
        _uiState.update(TestPushAddKeyDataTransformer(index, value))
    }

    override fun onDataValueChange(index: Int, value: TextFieldValue) {
        _uiState.update(TestPushAddValueDataTransformer(index, value))
    }

    override fun onDataAdd() {
        _uiState.update(TestPushAddDataTransformer)
    }

    override fun onDataRemove(index: Int) {
        _uiState.update(TestPushRemoveDataTransformer(index))
    }

    override fun onDeeplinkParamMenu() {
        _uiState.update(
            TestPushMenuTransformer(
                dismissBottomSheet = ::dismissBottomSheet,
                onDeepLinkParamClick = ::onDeepLinkParamClick,
            ),
        )
    }

    override fun onBackClick() {
        router?.back()
    }

    override fun onSendPush() {
        val state = uiState.value
        val rawPayload = state.payload.text.trim()
        if (rawPayload.isEmpty()) {
            testPushEmitter.emit(state.toFormPayload().withDisplayFallback(state))
        } else {
            TestPushPayloadParser.parse(rawPayload)
                .onLeft { _uiState.update(TestPushPayloadErrorTransformer(it)) }
                .onRight { testPushEmitter.emit(it.withDisplayFallback(state)) }
        }
    }

    private fun Map<String, String>.withDisplayFallback(state: TestPushUM): Map<String, String> {
        val isDisplayable = keys.any { it.startsWith(TestPushPayloadParser.NOTIFICATION_KEY_PREFIX) } ||
            !get(TITLE_KEY).isNullOrBlank() ||
            !get(BODY_KEY).isNullOrBlank()

        if (isDisplayable) return this

        return buildMap {
            putAll(this@withDisplayFallback)
            put(TITLE_KEY, state.title.text.ifBlank { FALLBACK_TITLE })
            state.message.text.takeIf(String::isNotBlank)?.let { put(BODY_KEY, it) }
        }
    }

    private fun TestPushUM.toFormPayload(): Map<String, String> = buildMap {
        title.text.takeIf(String::isNotBlank)?.let { put(TITLE_KEY, it) }
        message.text.takeIf(String::isNotBlank)?.let { put(BODY_KEY, it) }
        data.forEach { (key, value) ->
            if (key.text.isNotBlank()) put(key.text, value.text)
        }
    }

    private fun dismissBottomSheet() {
        marketsJobHolder.cancel()
        _uiState.update(TestPushDismissBottomSheetTransformer)
    }

    private fun onDeepLinkParamClick(item: TestPushMenuConfigUM.PushMenu) {
        when (item) {
            TestPushMenuConfigUM.PushMenu.MarketTokenDetails -> onMarketsTokenDetailsSheet()
        }
    }

    private fun subscribeMarketTokensList() {
        marketsLoader.state.onEach { item ->
            _uiState.update(
                TestPushMarketTokenBottomSheetTransformer(
                    item = item.data.flatMap { it.data }.toPersistentList(),
                    onDismissBottomSheet = ::dismissBottomSheet,
                    onSearchEdit = {
                        _uiState.update(
                            TestPushMarketTokenSearchBottomSheetTransformer(
                                it,
                            ),
                        )
                        onMarketsBatchReload(it.text)
                    },
                    onItemClick = {
                        _uiState.update(
                            TestPushMarketTokenClickBottomSheetTransformer(
                                it,
                            ),
                        )
                        marketsJobHolder.cancel()
                        dismissBottomSheet()
                    },
                ),
            )
        }
            .flowOn(dispatchers.default)
            .launchIn(viewModelScope)
            .saveIn(marketsJobHolder)
    }

    private fun onMarketsTokenDetailsSheet() {
        _uiState.update(
            TestPushMarketTokenBottomSheetTransformer(
                item = persistentListOf(),
                onDismissBottomSheet = ::dismissBottomSheet,
                onSearchEdit = {},
                onItemClick = {},
            ),
        )
        subscribeMarketTokensList()
        onMarketsBatchReload(searchValue = "")
    }

    private fun onMarketsBatchReload(searchValue: String) {
        viewModelScope.launch {
            marketsFlow.emit(
                BatchAction.Reload(
                    requestParams = TokenMarketListConfig(
                        fiatPriceCurrency = AppCurrency.Default.code,
                        searchText = searchValue,
                        priceChangeInterval = TokenMarketListConfig.Interval.H24,
                        order = TokenMarketListConfig.Order.ByRating,
                    ),
                ),
            )
        }
    }

    private companion object {
        const val TITLE_KEY = "title"
        const val BODY_KEY = "body"
        const val FALLBACK_TITLE = "Test push"
    }
}