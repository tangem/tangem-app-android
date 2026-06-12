package com.tangem.features.addressbook.editcontact.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.addressbook.model.ContactId
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.network.Network
import com.tangem.features.addressbook.editcontact.EditContactComponent
import com.tangem.features.addressbook.editcontact.contract.EditContactUM
import com.tangem.features.addressbook.editcontact.contract.ValidatedAddress
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class EditContactModelTest {

    @Test
    fun `WHEN model created THEN initial state is correct`() = runTest {
        val expectedColors = CryptoPortfolioIcon.Color.entries.toImmutableList()
        val expectedSelectedColor = expectedColors.first()

        val model = createModel(testScope = this)
        val state = model.state.value

        val expected = EditContactUM(
            title = resourceReference(R.string.address_book_new_contact),
            name = "",
            namePlaceholder = resourceReference(R.string.address_book_new_contact),
            portfolioIcon = AccountIconUM.CryptoPortfolio(
                value = CryptoPortfolioIcon.Icon.Letter,
                color = expectedSelectedColor,
            ),
            colors = EditContactUM.Colors(
                selected = expectedSelectedColor,
                list = expectedColors,
                onColorSelect = state.colors.onColorSelect,
            ),
            addresses = persistentListOf(),
            onNameChange = state.onNameChange,
            onCloseClick = state.onCloseClick,
            onAddAddressClick = state.onAddAddressClick,
        )
        assertThat(state).isEqualTo(expected)
    }

    @Test
    fun `GIVEN existing contactId WHEN model created THEN title is contact`() = runTest {
        // Arrange
        val params = EditContactComponent.Params(
            contactId = ContactId(value = "contact-id"),
            onBackClick = {},
            onAddAddressClick = {},
        )

        // Act
        val model = createModel(testScope = this, params = params)
        val state = model.state.value

        // Assert
        assertThat(state.title).isEqualTo(resourceReference(R.string.address_book_contact))
    }

    @Test
    fun `GIVEN initial state WHEN onNameChange THEN name updated`() = runTest {
        val model = createModel(testScope = this)
        val newName = "Satoshi"

        model.state.value.onNameChange(newName)

        assertThat(model.state.value.name).isEqualTo(newName)
    }

    @Test
    fun `GIVEN initial state WHEN onColorSelect THEN selected color and portfolio icon updated`() = runTest {
        val model = createModel(testScope = this)
        val newColor = CryptoPortfolioIcon.Color.entries.last()

        model.state.value.colors.onColorSelect(newColor)

        val state = model.state.value
        assertThat(state.colors.selected).isEqualTo(newColor)
        assertThat(state.portfolioIcon.color).isEqualTo(newColor)
    }

    @Test
    fun `GIVEN add address requested WHEN result delivered THEN address appended to state`() = runTest {
        // Arrange
        var capturedSink: ((ValidatedAddress) -> Unit)? = null
        val params = EditContactComponent.Params(
            contactId = null,
            onBackClick = {},
            onAddAddressClick = { onResult -> capturedSink = onResult },
        )
        val model = createModel(testScope = this, params = params)
        val validatedAddress = ValidatedAddress(address = "0xABC", network = mockk())

        // Act
        model.state.value.onAddAddressClick()
        capturedSink?.invoke(validatedAddress)

        // Assert
        assertThat(model.state.value.addresses).containsExactly(validatedAddress)
    }

    private fun createModel(
        testScope: TestScope,
        params: EditContactComponent.Params = EditContactComponent.Params(
            contactId = null,
            onBackClick = {},
            onAddAddressClick = {},
        ),
        paramsContainer: ParamsContainer = MutableParamsContainer(value = params),
    ): EditContactModel {
        return EditContactModel(
            paramsContainer = paramsContainer,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        )
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