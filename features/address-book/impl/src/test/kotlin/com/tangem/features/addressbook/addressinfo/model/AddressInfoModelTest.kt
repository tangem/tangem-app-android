package com.tangem.features.addressbook.addressinfo.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.features.addressbook.addressinfo.DefaultAddressInfoComponent
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class AddressInfoModelTest {

    private val clipboardManager: ClipboardManager = mockk(relaxed = true)
    private val messageSender: UiMessageSender = mockk(relaxed = true)

    private var model: AddressInfoModel? = null

    @AfterEach
    fun tearDown() {
        model?.onDestroy()
        model = null
    }

    @Test
    fun `WHEN created THEN state reflects params`() = runTest {
        // Act
        val model = createModel(testScope = this, address = "0xABC", networkCount = 3)

        // Assert
        assertThat(model.state.value.address).isEqualTo("0xABC")
        assertThat(model.state.value.networkCount).isEqualTo(3)
    }

    @Test
    fun `WHEN onCopy THEN address copied AND snackbar shown AND sheet dismissed`() = runTest {
        // Arrange
        var dismissed = false
        val model = createModel(testScope = this, address = "0xABC", onDismiss = { dismissed = true })

        // Act
        model.state.value.onCopy()

        // Assert
        verify { clipboardManager.setText(text = "0xABC", isSensitive = false) }
        verify { messageSender.send(any<SnackbarMessage>()) }
        assertThat(dismissed).isTrue()
    }

    @Test
    fun `WHEN onEditAddress or onDeleteAddress THEN delegated to the editor callbacks`() = runTest {
        // Arrange
        var edited = false
        var deleted = false
        val model = createModel(
            testScope = this,
            onEditAddress = { edited = true },
            onDeleteAddress = { deleted = true },
        )

        // Act
        model.state.value.onEditAddress()
        model.state.value.onDeleteAddress()

        // Assert
        assertThat(edited).isTrue()
        assertThat(deleted).isTrue()
    }

    @Suppress("LongParameterList")
    private fun createModel(
        testScope: TestScope,
        address: String = "0xABC",
        networkCount: Int = 1,
        onEditAddress: () -> Unit = {},
        onDeleteAddress: () -> Unit = {},
        onDismiss: () -> Unit = {},
    ): AddressInfoModel {
        val params = DefaultAddressInfoComponent.Params(
            address = address,
            networkCount = networkCount,
            onEditAddress = onEditAddress,
            onDeleteAddress = onDeleteAddress,
            onDismiss = onDismiss,
        )
        return AddressInfoModel(
            paramsContainer = MutableParamsContainer(value = params),
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            clipboardManager = clipboardManager,
            messageSender = messageSender,
        ).also { model = it }
    }

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }
}