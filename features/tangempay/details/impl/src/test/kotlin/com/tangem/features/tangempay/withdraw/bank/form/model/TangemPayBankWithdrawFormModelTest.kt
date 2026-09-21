package com.tangem.features.tangempay.withdraw.bank.form.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.pay.TangemPayCountry
import com.tangem.domain.models.pay.TangemPayRegion
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.form.TangemPayBankWithdrawFormComponent
import com.tangem.features.tangempay.withdraw.bank.form.state.TangemPayBankWithdrawFormUM.FieldUM
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawHeaderUM
import com.tangem.features.tangempay.withdraw.bank.model.NewRecipientDraft
import com.tangem.features.tangempay.withdraw.bank.navigation.BankWithdrawEditTarget
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TangemPayBankWithdrawFormModelTest {

    private val router: Router = mockk(relaxed = true)
    private val onBack: (BankWithdrawFormResult) -> Unit = mockk(relaxed = true)
    private val onContinue: (BankWithdrawFormResult) -> Unit = mockk(relaxed = true)
    private val onOpenSelector: (CountrySelectorConfig) -> Unit = mockk(relaxed = true)
    private val dispatchers = TestingCoroutineDispatcherProvider()
    private val draft = MutableStateFlow(BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal))

    private val fields = persistentListOf(
        BankWithdrawFormField.AccountNumber,
        BankWithdrawFormField.RoutingNumber,
        BankWithdrawFormField.FirstName,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(router, onBack, onContinue, onOpenSelector)
        draft.value = BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal)
    }

    @Test
    fun `GIVEN empty draft WHEN model created THEN empty fields header 4 of 5 and continue disabled`() {
        // Act
        val model = createModel()

        // Assert
        val state = model.uiState.value
        assertThat(state.header).isEqualTo(
            BankWithdrawHeaderUM(
                title = resourceReference(R.string.tangempay_withdraw_hub_to_personal_account),
                subtitle = resourceReference(R.string.tangempay_withdraw_bank_step_counter, wrappedList(4, 5)),
            ),
        )
        assertThat(state.fields.map { it.field }).containsExactlyElementsIn(fields).inOrder()
        assertThat(state.fields.map { it.value }).containsExactly("", "", "")
        assertThat(state.fields.map { it.error }).containsExactly(null, null, null)
        assertThat(state.isContinueEnabled).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN draft with saved values WHEN model created THEN fields prefilled`() {
        // Arrange
        draft.value = BankWithdrawDraft(
            entry = BankWithdrawEntry.NewPersonal,
            newRecipient = NewRecipientDraft(fields = mapOf(BankWithdrawFormField.FirstName to "Basit")),
        )

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value.fields.map { it.value }).containsExactly("", "", "Basit").inOrder()
        model.onDestroy()
    }

    @Test
    fun `GIVEN all fields filled WHEN values change THEN continue enabled`() {
        // Arrange
        val model = createModel()

        // Act
        fill(model, BankWithdrawFormField.AccountNumber, "4830019271")
        fill(model, BankWithdrawFormField.RoutingNumber, "021000021")
        fill(model, BankWithdrawFormField.FirstName, "Basit")

        // Assert
        assertThat(model.uiState.value.isContinueEnabled).isTrue()
        model.onDestroy()
    }

    @Test
    fun `GIVEN invalid value WHEN field loses focus THEN format error shown`() {
        // Arrange
        val model = createModel()
        fill(model, BankWithdrawFormField.RoutingNumber, "123")

        // Act
        textField(model, BankWithdrawFormField.RoutingNumber).onFocusChange(false)

        // Assert
        assertThat(textField(model, BankWithdrawFormField.RoutingNumber).error)
            .isEqualTo(resourceReference(R.string.tangempay_withdraw_bank_field_error_routing_number))
        model.onDestroy()
    }

    @Test
    fun `GIVEN empty value WHEN field loses focus THEN no error`() {
        // Arrange
        val model = createModel()

        // Act
        textField(model, BankWithdrawFormField.AccountNumber).onFocusChange(false)

        // Assert
        assertThat(textField(model, BankWithdrawFormField.AccountNumber).error).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN non-latin city WHEN field blurred THEN latin error shown`() {
        // Arrange
        val model = createModel(formFields = cityFields)
        fill(model, BankWithdrawFormField.City, "Москва")

        // Act
        textField(model, BankWithdrawFormField.City).onFocusChange(false)

        // Assert
        assertThat(textField(model, BankWithdrawFormField.City).error)
            .isEqualTo(resourceReference(R.string.tangempay_order_data_field_latin_only))
        model.onDestroy()
    }

    @Test
    fun `GIVEN field with error WHEN value changes THEN error cleared`() {
        // Arrange
        val model = createModel()
        fill(model, BankWithdrawFormField.AccountNumber, "12")
        textField(model, BankWithdrawFormField.AccountNumber).onFocusChange(false)

        // Act
        fill(model, BankWithdrawFormField.AccountNumber, "123")

        // Assert
        assertThat(textField(model, BankWithdrawFormField.AccountNumber).error).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN blank and invalid fields WHEN continue clicked THEN errors shown and nothing reported`() {
        // Arrange
        val model = createModel()
        fill(model, BankWithdrawFormField.AccountNumber, "4830019271")
        fill(model, BankWithdrawFormField.RoutingNumber, "12")

        // Act
        model.uiState.value.onContinueClick()

        // Assert
        assertThat(model.uiState.value.fields.map { it.error }).containsExactly(
            null,
            resourceReference(R.string.tangempay_withdraw_bank_field_error_routing_number),
            resourceReference(R.string.tangempay_order_data_field_required),
        ).inOrder()
        verify(exactly = 0) { onContinue(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN valid fields WHEN continue clicked THEN trimmed values reported`() {
        // Arrange
        val model = createModel()
        fill(model, BankWithdrawFormField.AccountNumber, "4830019271")
        fill(model, BankWithdrawFormField.RoutingNumber, "021000021")
        fill(model, BankWithdrawFormField.FirstName, " Basit ")

        // Act
        model.uiState.value.onContinueClick()

        // Assert
        verify(exactly = 1) {
            onContinue(
                BankWithdrawFormResult(
                    values = mapOf(
                        BankWithdrawFormField.AccountNumber to "4830019271",
                        BankWithdrawFormField.RoutingNumber to "021000021",
                        BankWithdrawFormField.FirstName to "Basit",
                    ),
                    displayNames = mapOf(
                        BankWithdrawFormField.AccountNumber to "",
                        BankWithdrawFormField.RoutingNumber to "",
                        BankWithdrawFormField.FirstName to "",
                    ),
                ),
            )
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN fields WHEN model created THEN ime action next except last done`() {
        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value.fields.filterIsInstance<FieldUM.Text>().map { it.keyboardOptions.imeAction.toString() })
            .containsExactly("Next", "Next", "Done").inOrder()
        model.onDestroy()
    }

    @Test
    fun `GIVEN typed values WHEN back clicked THEN values reported and router pops`() {
        // Arrange
        val model = createModel()
        fill(model, BankWithdrawFormField.FirstName, " Basit ")

        // Act
        model.uiState.value.onBackClick()

        // Assert
        verify(exactly = 1) {
            onBack(
                BankWithdrawFormResult(
                    values = mapOf(
                        BankWithdrawFormField.AccountNumber to "",
                        BankWithdrawFormField.RoutingNumber to "",
                        BankWithdrawFormField.FirstName to "Basit",
                    ),
                    displayNames = mapOf(
                        BankWithdrawFormField.AccountNumber to "",
                        BankWithdrawFormField.RoutingNumber to "",
                        BankWithdrawFormField.FirstName to "",
                    ),
                ),
            )
        }
        verify(exactly = 1) { router.pop() }
        model.onDestroy()
    }

    @Test
    fun `GIVEN edit step WHEN back clicked THEN nothing reported and router pops`() {
        // Arrange
        val model = createModel(step = TangemPayBankWithdrawInnerRoute.Edit(BankWithdrawEditTarget.RecipientDetails))
        fill(model, BankWithdrawFormField.FirstName, "Basit")

        // Act
        model.uiState.value.onBackClick()

        // Assert
        verify(exactly = 0) { onBack(any()) }
        verify(exactly = 1) { router.pop() }
        model.onDestroy()
    }

    @Test
    fun `GIVEN untouched field WHEN draft changes after construction THEN new draft value shown`() {
        // Arrange
        val model = createModel()

        // Act
        setDraftFirstName("Night City")

        // Assert
        assertThat(textField(model, BankWithdrawFormField.FirstName).value).isEqualTo("Night City")
        model.onDestroy()
    }

    @Test
    fun `GIVEN user typed a value WHEN draft changes afterwards THEN typed value still shown`() {
        // Arrange
        val model = createModel()
        fill(model, BankWithdrawFormField.FirstName, "Basit")

        // Act
        setDraftFirstName("Night City")

        // Assert
        assertThat(textField(model, BankWithdrawFormField.FirstName).value).isEqualTo("Basit")
        model.onDestroy()
    }

    @Test
    fun `GIVEN continue committed WHEN draft changes afterwards THEN field follows the new draft value`() {
        // Arrange
        val model = createModel()
        fill(model, BankWithdrawFormField.AccountNumber, "4830019271")
        fill(model, BankWithdrawFormField.RoutingNumber, "021000021")
        fill(model, BankWithdrawFormField.FirstName, "Basit")
        model.uiState.value.onContinueClick()

        // Act
        setDraftFirstName("Night City")

        // Assert
        assertThat(textField(model, BankWithdrawFormField.FirstName).value).isEqualTo("Night City")
        model.onDestroy()
    }

    @Test
    fun `GIVEN postal code error under US WHEN country changes THEN error cleared`() {
        // Arrange
        val model = createModel(formFields = usAddressFields)
        model.onCountrySelected(field = BankWithdrawFormField.Country, country = usa, region = california)
        fill(model, BankWithdrawFormField.PostalCode, "12")
        textField(model, BankWithdrawFormField.PostalCode).onFocusChange(false)
        assertThat(textField(model, BankWithdrawFormField.PostalCode).error).isNotNull()

        // Act
        model.onCountrySelected(field = BankWithdrawFormField.Country, country = greatBritain, region = null)

        // Assert
        assertThat(textField(model, BankWithdrawFormField.PostalCode).error).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN picked residency WHEN only region changed THEN country kept`() {
        // GIVEN
        val model = createModel(formFields = addressFields)
        model.onCountrySelected(field = BankWithdrawFormField.Country, country = usa, region = california)

        // WHEN
        model.onRegionSelected(field = BankWithdrawFormField.Region, region = texas)

        // THEN
        assertThat(selectorField(model, BankWithdrawFormField.Country).value).isEqualTo("United States")
        assertThat(selectorField(model, BankWithdrawFormField.Region).value).isEqualTo("Texas")
        model.onDestroy()
    }

    @Test
    fun `GIVEN no country picked WHEN model created THEN region is a free-text field`() {
        // GIVEN + WHEN
        val model = createModel(formFields = addressFields)

        // THEN
        assertThat(model.uiState.value.fields.map { it.field }).containsExactlyElementsIn(addressFields).inOrder()
        assertThat(anyField(model, BankWithdrawFormField.Region)).isInstanceOf(FieldUM.Text::class.java)
        assertThat(anyField(model, BankWithdrawFormField.Region).placeholder)
            .isEqualTo(resourceReference(R.string.tangempay_withdraw_bank_placeholder_region_name))
        model.onDestroy()
    }

    @Test
    fun `GIVEN US picked WHEN country selected THEN region is a selector`() {
        // GIVEN
        val model = createModel(formFields = addressFields)

        // WHEN
        model.onCountrySelected(field = BankWithdrawFormField.Country, country = usa, region = california)

        // THEN
        assertThat(anyField(model, BankWithdrawFormField.Region)).isInstanceOf(FieldUM.Selector::class.java)
        assertThat(selectorField(model, BankWithdrawFormField.Region).value).isEqualTo("California")
        assertThat(anyField(model, BankWithdrawFormField.Region).placeholder)
            .isEqualTo(resourceReference(R.string.tangempay_withdraw_bank_placeholder_not_selected))
        model.onDestroy()
    }

    @Test
    fun `GIVEN country without regions WHEN country selected THEN region is free text and continue disabled`() {
        // GIVEN
        val model = createModel(formFields = addressFields)

        // WHEN
        model.onCountrySelected(field = BankWithdrawFormField.Country, country = monaco, region = null)

        // THEN
        assertThat(anyField(model, BankWithdrawFormField.Region)).isInstanceOf(FieldUM.Text::class.java)
        assertThat(model.uiState.value.isContinueEnabled).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN empty postal code WHEN model created THEN postal code still shown`() {
        // GIVEN + WHEN
        val model = createModel(formFields = BankWithdrawFormFields.bankDetails)

        // THEN
        assertThat(model.uiState.value.fields.map { it.field })
            .containsExactlyElementsIn(BankWithdrawFormFields.bankDetails)
            .inOrder()
        model.onDestroy()
    }

    @Test
    fun `GIVEN free-text region typed WHEN continue clicked THEN region reported`() {
        // GIVEN
        val model = createModel(formFields = addressFields)
        model.onCountrySelected(field = BankWithdrawFormField.Country, country = monaco, region = null)
        fill(model, BankWithdrawFormField.Region, " Bavaria ")

        // WHEN
        model.uiState.value.onContinueClick()

        // THEN
        verify(exactly = 1) {
            onContinue(
                BankWithdrawFormResult(
                    values = mapOf(
                        BankWithdrawFormField.Country to "MC",
                        BankWithdrawFormField.Region to "Bavaria",
                    ),
                    displayNames = mapOf(
                        BankWithdrawFormField.Country to "Monaco",
                        BankWithdrawFormField.Region to "",
                    ),
                ),
            )
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN free-text region typed WHEN US picked THEN region cleared`() {
        // GIVEN
        val model = createModel(formFields = addressFields)
        model.onCountrySelected(field = BankWithdrawFormField.Country, country = monaco, region = null)
        fill(model, BankWithdrawFormField.Region, "Bavaria")

        // WHEN
        model.onCountrySelected(field = BankWithdrawFormField.Country, country = usa, region = california)

        // THEN
        assertThat(selectorField(model, BankWithdrawFormField.Region).value).isEqualTo("California")
        model.onDestroy()
    }

    @Test
    fun `GIVEN country picked WHEN region selector clicked THEN picker opened for that country`() {
        // GIVEN
        val model = createModel(formFields = addressFields)
        model.onCountrySelected(field = BankWithdrawFormField.Country, country = usa, region = california)

        // WHEN
        selectorField(model, BankWithdrawFormField.Region).onClick()

        // THEN
        verify(exactly = 1) {
            onOpenSelector(CountrySelectorConfig.Region(field = BankWithdrawFormField.Region, countryCode = "US"))
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN address form WHEN country selector clicked THEN country picker opened`() {
        // GIVEN
        val model = createModel(formFields = addressFields)

        // WHEN
        selectorField(model, BankWithdrawFormField.Country).onClick()

        // THEN
        verify(exactly = 1) { onOpenSelector(CountrySelectorConfig.Country(field = BankWithdrawFormField.Country)) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN bank details WHEN bank region selector clicked THEN us region picker opened`() {
        // GIVEN
        val model = createModel(formFields = bankAddressFields)

        // WHEN
        selectorField(model, BankWithdrawFormField.BankRegion).onClick()

        // THEN
        verify(exactly = 1) {
            onOpenSelector(
                CountrySelectorConfig.Region(field = BankWithdrawFormField.BankRegion, countryCode = "US"),
            )
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN US picked WHEN country selected THEN country field shows the country name`() {
        // Arrange
        val model = createModel(formFields = addressFields)

        // Act
        model.onCountrySelected(field = BankWithdrawFormField.Country, country = usa, region = california)

        // Assert
        assertThat(selectorField(model, BankWithdrawFormField.Country).value).isEqualTo("United States")
        model.onDestroy()
    }

    @Test
    fun `GIVEN region picked WHEN region selected THEN region field shows the region name`() {
        // Arrange
        val model = createModel(formFields = addressFields)
        model.onCountrySelected(field = BankWithdrawFormField.Country, country = usa, region = null)

        // Act
        model.onRegionSelected(field = BankWithdrawFormField.Region, region = texas)

        // Assert
        assertThat(selectorField(model, BankWithdrawFormField.Region).value).isEqualTo("Texas")
        model.onDestroy()
    }

    @Test
    fun `GIVEN bank region picked WHEN region selected THEN bank region field shows the state name`() {
        // Arrange
        val model = createModel(formFields = bankAddressFields)

        // Act
        model.onRegionSelected(field = BankWithdrawFormField.BankRegion, region = newYork)

        // Assert
        assertThat(selectorField(model, BankWithdrawFormField.BankRegion).value).isEqualTo("New York")
        model.onDestroy()
    }

    @Test
    fun `GIVEN draft code without a name WHEN model created THEN the code is shown`() {
        // Arrange
        draft.value = BankWithdrawDraft(
            entry = BankWithdrawEntry.NewPersonal,
            newRecipient = NewRecipientDraft(fields = mapOf(BankWithdrawFormField.Country to "US")),
        )

        // Act
        val model = createModel(formFields = addressFields)

        // Assert
        assertThat(selectorField(model, BankWithdrawFormField.Country).value).isEqualTo("US")
        model.onDestroy()
    }

    @Test
    fun `GIVEN draft with names WHEN model recreated THEN names still shown`() {
        // Arrange
        setDraftWithPickedCalifornia()

        // Act
        val model = createModel(formFields = addressFields)

        // Assert
        assertThat(selectorField(model, BankWithdrawFormField.Country).value).isEqualTo("United States")
        assertThat(selectorField(model, BankWithdrawFormField.Region).value).isEqualTo("California")
        model.onDestroy()
    }

    @Test
    fun `GIVEN picked country WHEN continue clicked THEN codes and names reported`() {
        // Arrange
        val model = createModel(formFields = usAddressFields)
        model.onCountrySelected(field = BankWithdrawFormField.Country, country = usa, region = california)
        fill(model, BankWithdrawFormField.PostalCode, "90001")

        // Act
        model.uiState.value.onContinueClick()

        // Assert
        verify(exactly = 1) {
            onContinue(
                BankWithdrawFormResult(
                    values = mapOf(
                        BankWithdrawFormField.Country to "US",
                        BankWithdrawFormField.Region to "CA",
                        BankWithdrawFormField.PostalCode to "90001",
                    ),
                    displayNames = mapOf(
                        BankWithdrawFormField.Country to "United States",
                        BankWithdrawFormField.Region to "California",
                        BankWithdrawFormField.PostalCode to "",
                    ),
                ),
            )
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN iso subdivision code WHEN region selected THEN bare code stored`() {
        // Arrange
        val model = createModel(formFields = addressFields)
        model.onCountrySelected(field = BankWithdrawFormField.Country, country = usa, region = null)

        // Act
        model.onRegionSelected(field = BankWithdrawFormField.Region, region = californiaIso)
        model.uiState.value.onContinueClick()

        // Assert
        verify(exactly = 1) {
            onContinue(
                BankWithdrawFormResult(
                    values = mapOf(
                        BankWithdrawFormField.Country to "US",
                        BankWithdrawFormField.Region to "CA",
                    ),
                    displayNames = mapOf(
                        BankWithdrawFormField.Country to "United States",
                        BankWithdrawFormField.Region to "California",
                    ),
                ),
            )
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN lowercase prefixed region code WHEN region selected THEN bare code stored`() {
        // Arrange
        val model = createModel(formFields = addressFields)
        model.onCountrySelected(field = BankWithdrawFormField.Country, country = usa, region = null)

        // Act
        model.onRegionSelected(field = BankWithdrawFormField.Region, region = californiaLowercaseIso)
        model.uiState.value.onContinueClick()

        // Assert
        verify(exactly = 1) {
            onContinue(
                BankWithdrawFormResult(
                    values = mapOf(
                        BankWithdrawFormField.Country to "US",
                        BankWithdrawFormField.Region to "CA",
                    ),
                    displayNames = mapOf(
                        BankWithdrawFormField.Country to "United States",
                        BankWithdrawFormField.Region to "California",
                    ),
                ),
            )
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN bare region code WHEN region selected THEN code stored unchanged`() {
        // Arrange
        val model = createModel(formFields = addressFields)
        model.onCountrySelected(field = BankWithdrawFormField.Country, country = usa, region = null)

        // Act
        model.onRegionSelected(field = BankWithdrawFormField.Region, region = texas)
        model.uiState.value.onContinueClick()

        // Assert
        verify(exactly = 1) {
            onContinue(
                BankWithdrawFormResult(
                    values = mapOf(
                        BankWithdrawFormField.Country to "US",
                        BankWithdrawFormField.Region to "TX",
                    ),
                    displayNames = mapOf(
                        BankWithdrawFormField.Country to "United States",
                        BankWithdrawFormField.Region to "Texas",
                    ),
                ),
            )
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN us bank region WHEN region selected THEN us prefix stripped`() {
        // Arrange
        val model = createModel(formFields = bankAddressFields)

        // Act
        model.onRegionSelected(field = BankWithdrawFormField.BankRegion, region = newYorkIso)
        model.uiState.value.onContinueClick()

        // Assert
        verify(exactly = 1) {
            onContinue(
                BankWithdrawFormResult(
                    values = mapOf(BankWithdrawFormField.BankRegion to "NY"),
                    displayNames = mapOf(BankWithdrawFormField.BankRegion to "New York"),
                ),
            )
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN country with a region WHEN country selected THEN region code stripped too`() {
        // Arrange
        val model = createModel(formFields = addressFields)

        // Act
        model.onCountrySelected(field = BankWithdrawFormField.Country, country = usa, region = californiaIso)
        model.uiState.value.onContinueClick()

        // Assert
        verify(exactly = 1) {
            onContinue(
                BankWithdrawFormResult(
                    values = mapOf(
                        BankWithdrawFormField.Country to "US",
                        BankWithdrawFormField.Region to "CA",
                    ),
                    displayNames = mapOf(
                        BankWithdrawFormField.Country to "United States",
                        BankWithdrawFormField.Region to "California",
                    ),
                ),
            )
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN free-text region typed WHEN continue clicked THEN region name is blank`() {
        // Arrange
        setDraftWithPickedCalifornia()
        val model = createModel(formFields = addressFields)
        model.onCountrySelected(field = BankWithdrawFormField.Country, country = monaco, region = null)
        fill(model, BankWithdrawFormField.Region, "Bavaria")

        // Act
        model.uiState.value.onContinueClick()

        // Assert
        verify(exactly = 1) {
            onContinue(
                BankWithdrawFormResult(
                    values = mapOf(
                        BankWithdrawFormField.Country to "MC",
                        BankWithdrawFormField.Region to "Bavaria",
                    ),
                    displayNames = mapOf(
                        BankWithdrawFormField.Country to "Monaco",
                        BankWithdrawFormField.Region to "",
                    ),
                ),
            )
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN picked US region WHEN country changed to a country without regions THEN region name cleared`() {
        // Arrange
        val model = createModel(formFields = addressFields)
        model.onCountrySelected(field = BankWithdrawFormField.Country, country = usa, region = california)

        // Act
        model.onCountrySelected(field = BankWithdrawFormField.Country, country = monaco, region = null)
        model.uiState.value.onBackClick()

        // Assert
        verify(exactly = 1) {
            onBack(
                BankWithdrawFormResult(
                    values = mapOf(
                        BankWithdrawFormField.Country to "MC",
                        BankWithdrawFormField.Region to "",
                    ),
                    displayNames = mapOf(
                        BankWithdrawFormField.Country to "Monaco",
                        BankWithdrawFormField.Region to "",
                    ),
                ),
            )
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN picked US region WHEN region typed by hand THEN the old name is gone`() {
        // Arrange
        setDraftWithPickedCalifornia()
        val model = createModel(formFields = addressFields)
        model.onCountrySelected(field = BankWithdrawFormField.Country, country = monaco, region = null)

        // Act
        fill(model, BankWithdrawFormField.Region, "Bavaria")

        // Assert
        assertThat(anyField(model, BankWithdrawFormField.Region).value).isEqualTo("Bavaria")
        model.onDestroy()
    }

    @Test
    fun `GIVEN stale region name in draft WHEN region typed THEN the name is cleared`() {
        // Arrange
        draft.value = BankWithdrawDraft(
            entry = BankWithdrawEntry.NewPersonal,
            newRecipient = NewRecipientDraft(
                fields = mapOf(
                    BankWithdrawFormField.Country to "MC",
                    BankWithdrawFormField.Region to "CA",
                ),
                displayNames = mapOf(BankWithdrawFormField.Region to "California"),
            ),
        )
        val model = createModel(formFields = addressFields)

        // Act
        fill(model, BankWithdrawFormField.Region, "Bavaria")
        val typedRegion = textField(model, BankWithdrawFormField.Region).value
        model.uiState.value.onContinueClick()

        // Assert
        assertThat(typedRegion).isEqualTo("Bavaria")
        verify(exactly = 1) {
            onContinue(
                BankWithdrawFormResult(
                    values = mapOf(
                        BankWithdrawFormField.Country to "MC",
                        BankWithdrawFormField.Region to "Bavaria",
                    ),
                    displayNames = mapOf(
                        BankWithdrawFormField.Country to "",
                        BankWithdrawFormField.Region to "",
                    ),
                ),
            )
        }
        model.onDestroy()
    }

    private fun fill(model: TangemPayBankWithdrawFormModel, field: BankWithdrawFormField, value: String) {
        textField(model, field).onValueChange(value)
    }

    private fun anyField(model: TangemPayBankWithdrawFormModel, field: BankWithdrawFormField) =
        model.uiState.value.fields.first { it.field == field }

    private fun textField(model: TangemPayBankWithdrawFormModel, field: BankWithdrawFormField) =
        model.uiState.value.fields.filterIsInstance<FieldUM.Text>().first { it.field == field }

    private fun selectorField(model: TangemPayBankWithdrawFormModel, field: BankWithdrawFormField) =
        model.uiState.value.fields.filterIsInstance<FieldUM.Selector>().first { it.field == field }

    private fun setDraftWithPickedCalifornia() {
        draft.value = BankWithdrawDraft(
            entry = BankWithdrawEntry.NewPersonal,
            newRecipient = NewRecipientDraft(
                fields = mapOf(
                    BankWithdrawFormField.Country to "US",
                    BankWithdrawFormField.Region to "CA",
                ),
                displayNames = mapOf(
                    BankWithdrawFormField.Country to "United States",
                    BankWithdrawFormField.Region to "California",
                ),
            ),
        )
    }

    private fun setDraftFirstName(value: String) {
        val draftFields = mapOf(BankWithdrawFormField.FirstName to value)
        draft.value = draft.value.copy(newRecipient = draft.value.newRecipient.copy(fields = draftFields))
    }

    private fun createModel(
        formFields: ImmutableList<BankWithdrawFormField> = fields,
        step: TangemPayBankWithdrawInnerRoute = TangemPayBankWithdrawInnerRoute.RecipientDetails,
    ): TangemPayBankWithdrawFormModel = TangemPayBankWithdrawFormModel(
        paramsContainer = MutableParamsContainer(
            TangemPayBankWithdrawFormModel.Params(
                form = TangemPayBankWithdrawFormComponent.Params(
                    draft = draft,
                    step = step,
                    fields = formFields,
                    onBack = onBack,
                    onContinue = onContinue,
                ),
                onOpenSelector = onOpenSelector,
            ),
        ),
        dispatchers = dispatchers,
        router = router,
    )

    private companion object {
        val addressFields = persistentListOf(BankWithdrawFormField.Country, BankWithdrawFormField.Region)
        val usAddressFields = persistentListOf(
            BankWithdrawFormField.Country,
            BankWithdrawFormField.Region,
            BankWithdrawFormField.PostalCode,
        )
        val bankAddressFields = persistentListOf(BankWithdrawFormField.BankRegion)
        val cityFields = persistentListOf(BankWithdrawFormField.City)
        val usa = TangemPayCountry("US", "United States", null, isSuggested = true, hasRegions = true)
        val greatBritain = TangemPayCountry("GB", "United Kingdom", null, isSuggested = false, hasRegions = true)
        val monaco = TangemPayCountry("MC", "Monaco", null, isSuggested = false, hasRegions = false)
        val california = TangemPayRegion(code = "CA", name = "California", isSuggested = true)
        val texas = TangemPayRegion(code = "TX", name = "Texas", isSuggested = false)
        val newYork = TangemPayRegion(code = "NY", name = "New York", isSuggested = false)
        val californiaIso = TangemPayRegion(code = "US-CA", name = "California", isSuggested = true)
        val californiaLowercaseIso = TangemPayRegion(code = "us-CA", name = "California", isSuggested = true)
        val newYorkIso = TangemPayRegion(code = "US-NY", name = "New York", isSuggested = false)
    }
}