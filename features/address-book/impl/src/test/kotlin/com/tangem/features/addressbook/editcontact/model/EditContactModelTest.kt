package com.tangem.features.addressbook.editcontact.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.addressbook.model.ContactId
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.addressbook.common.AddressBookResultHolder
import com.tangem.features.addressbook.editcontact.DefaultEditContactComponent
import com.tangem.features.addressbook.editcontact.state.EditContactStateController
import com.tangem.features.addressbook.editcontact.ui.state.EditContactUM
import com.tangem.features.addressbook.editcontact.ui.state.ValidatedAddress
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class EditContactModelTest {

    private val resultHolder = AddressBookResultHolder()

    private var model: EditContactModel? = null

    @AfterEach
    fun tearDown() {
        // Cancels modelScope, stopping the confirmed-addresses collector.
        model?.onDestroy()
        model = null
    }

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
        val params = createParams(contactId = ContactId(value = "contact-id"))

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
    fun `GIVEN confirmed address set on holder WHEN collected THEN address appended to state`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        val validatedAddress = ValidatedAddress(address = "0xABC", networkIds = persistentListOf("ethereum"))

        // Act
        resultHolder.setConfirmedAddress(validatedAddress)
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.addresses).containsExactly(validatedAddress)
        // The value must be consumed so it is not re-applied on resubscription.
        assertThat(resultHolder.confirmedAddress.value).isNull()
    }

    @Test
    fun `GIVEN same address confirmed twice WHEN collected THEN added only once`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        val validatedAddress = ValidatedAddress(address = "0xABC", networkIds = persistentListOf("ethereum"))

        // Act
        resultHolder.setConfirmedAddress(validatedAddress)
        advanceUntilIdle()
        resultHolder.setConfirmedAddress(validatedAddress)
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.addresses).containsExactly(validatedAddress)
    }

    @Test
    fun `GIVEN predefined address WHEN model created THEN address attached`() = runTest {
        // Arrange
        val predefined = ValidatedAddress(address = "0xABC", networkIds = persistentListOf("ethereum"))

        // Act
        val model = createModel(testScope = this, params = createParams(predefinedAddress = predefined))
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.addresses).containsExactly(predefined)
    }

    private fun createParams(
        contactId: ContactId? = null,
        predefinedAddress: ValidatedAddress? = null,
    ): DefaultEditContactComponent.Params = DefaultEditContactComponent.Params(
        contactId = contactId,
        predefinedAddress = predefinedAddress,
        onBackClick = {},
        onAddAddressClick = {},
    )

    private fun createModel(
        testScope: TestScope,
        params: DefaultEditContactComponent.Params = createParams(),
        paramsContainer: ParamsContainer = MutableParamsContainer(value = params),
    ): EditContactModel {
        return EditContactModel(
            paramsContainer = paramsContainer,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            stateController = EditContactStateController(),
            resultHolder = resultHolder,
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