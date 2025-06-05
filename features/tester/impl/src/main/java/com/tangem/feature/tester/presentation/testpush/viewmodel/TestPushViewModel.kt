package com.tangem.feature.tester.presentation.testpush.viewmodel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.core.ui.R
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.GetMarketsTokenListFlowUseCase
import com.tangem.domain.markets.TokenListBatchingContext
import com.tangem.domain.markets.TokenMarketListConfig
import com.tangem.domain.markets.TokenMarketUpdateRequest
import com.tangem.feature.tester.presentation.navigation.InnerTesterRouter
import com.tangem.feature.tester.presentation.testpush.entity.TestPushClickIntents
import com.tangem.feature.tester.presentation.testpush.entity.TestPushMenuConfigUM
import com.tangem.feature.tester.presentation.testpush.entity.TestPushUM
import com.tangem.feature.tester.presentation.testpush.viewmodel.transformers.*
import com.tangem.feature.tester.presentation.testpush.viewmodel.transformers.markettokens.TestPushMarketTokenBottomSheetTransformer
import com.tangem.feature.tester.presentation.testpush.viewmodel.transformers.markettokens.TestPushMarketTokenClickBottomSheetTransformer
import com.tangem.feature.tester.presentation.testpush.viewmodel.transformers.markettokens.TestPushMarketTokenSearchBottomSheetTransformer
import com.tangem.pagination.BatchAction
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.notifications.PushNotificationsTokenProvider
import com.tangem.utils.transformer.update
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class TestPushViewModel @Inject constructor(
    private val pushNotificationsTokenProvider: PushNotificationsTokenProvider,
    @ApplicationContext private val applicationContext: Context,
    private val dispatchers: CoroutineDispatcherProvider,
    getMarketsTokenListFlowUseCase: GetMarketsTokenListFlowUseCase,
) : ViewModel(), TestPushClickIntents {

    private val _uiState = MutableStateFlow(
        TestPushUM(
            fcmToken = "",
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
        val value = uiState.value

        val channelId = "Test Tangem Channel"

        val intent = Intent(applicationContext, Class.forName("com.tangem.tap.MainActivity")).apply {
            value.data.forEach { (key, value) ->
                putExtra(key.text, value.text)
            }
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            /* context = */ applicationContext,
            /* requestCode = */ 1,
            /* intent = */ intent,
            /* flags = */ PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notificationBuilder =
            NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(com.tangem.feature.tester.impl.R.drawable.ic_tangem_24)
                .setContentTitle(value.title.text)
                .setContentText(value.message.text)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

        val notificationManager =
            applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelId,
                ContextCompat.getString(
                    applicationContext,
                    R.string.common_tangem_wallet,
                ),
                NotificationManager.IMPORTANCE_HIGH,
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }

        // Generating unique notification id
        val uniqueId = (System.currentTimeMillis() % Integer.MAX_VALUE).toInt()

        notificationManager.notify(
            /* id = */ uniqueId,
            /* notification = */ notificationBuilder.build(),
        )
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
}