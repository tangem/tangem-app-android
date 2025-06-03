package com.tangem.feature.tester.presentation.testpush.viewmodel

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.feature.tester.presentation.navigation.InnerTesterRouter
import com.tangem.feature.tester.presentation.testpush.entity.TestPushClickIntents
import com.tangem.feature.tester.presentation.testpush.entity.TestPushUM
import com.tangem.utils.notifications.PushNotificationsTokenProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class TestPushViewModel @Inject constructor(
    private val pushNotificationsTokenProvider: PushNotificationsTokenProvider,
) : ViewModel(), TestPushClickIntents {

    val uiState: StateFlow<TestPushUM>
    field = MutableStateFlow(
        TestPushUM(
            fcmToken = "",
            title = TextFieldValue(""),
            message = TextFieldValue(""),
            data = listOf(TextFieldValue("") to TextFieldValue("")),
        ),
    )

    private var router: InnerTesterRouter? = null

    init {
        viewModelScope.launch {
            val fcmToken = pushNotificationsTokenProvider.getToken()
            uiState.update { it.copy(fcmToken = fcmToken) }
        }
    }

    fun setupNavigation(router: InnerTesterRouter) {
        this.router = router
    }

    override fun onTitleChange(value: TextFieldValue) {
        uiState.update { it.copy(title = value) }
    }

    override fun onMessageChange(value: TextFieldValue) {
        uiState.update { it.copy(message = value) }
    }

    override fun onDataKeyChange(index: Int, value: TextFieldValue) {
        val mutableData = uiState.value.data.toMutableList()
        val updated = mutableData[index].copy(first = value)
        mutableData.set(index = index, updated)

        uiState.update {
            it.copy(data = mutableData)
        }
    }

    override fun onDataValueChange(index: Int, value: TextFieldValue) {
        val mutableData = uiState.value.data.toMutableList()
        val updated = mutableData[index].copy(second = value)
        mutableData.set(index = index, updated)

        uiState.update {
            it.copy(data = mutableData)
        }
    }

    override fun onDataAdd() {
        uiState.update {
            it.copy(
                data = it.data.plus(TextFieldValue("") to TextFieldValue("")),
            )
        }
    }

    override fun onDataRemove(index: Int) {
        uiState.update {
            it.copy(
                data = it.data.toMutableList().apply {
                    removeAt(index = index)
                },
            )
        }
    }

    override fun onBackClick() {
        router?.back()
    }
}