package com.tangem.features.collectibles.impl.discovery.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.collectibles.impl.createMachine
import com.tangem.core.ui.extensions.wrappedList
import org.junit.jupiter.api.*
import java.math.BigDecimal
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CollectiblesDiscoveryPackConverterTest {

    private val clickedPackIds = mutableListOf<String>()

    private val converter = CollectiblesDiscoveryPackConverter(onPackClick = { clickedPackIds += it })

    private lateinit var defaultLocale: Locale

    // The price is rendered with NumberFormat.getCurrencyInstance(Locale.getDefault()).
    @BeforeAll
    fun pinLocale() {
        defaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @AfterAll
    fun restoreLocale() {
        Locale.setDefault(defaultLocale)
    }

    // The class is PER_CLASS, so the recorded clicks would leak between tests.
    @BeforeEach
    fun resetRecordedClicks() {
        clickedPackIds.clear()
    }

    @Test
    fun `GIVEN a short name WHEN convert THEN it is preferred over the full name`() {
        // Act
        val actual = converter.convert(createMachine(name = "Pokemon Legendary", shortName = "Pokemon"))

        // Assert
        assertThat(actual.title).isEqualTo(stringReference("Pokemon"))
    }

    @Test
    fun `GIVEN an empty short name WHEN convert THEN the full name is shown`() {
        // Act
        val actual = converter.convert(createMachine(name = "Pokemon Legendary", shortName = ""))

        // Assert
        assertThat(actual.title).isEqualTo(stringReference("Pokemon Legendary"))
    }

    @Test
    fun `GIVEN a blank short name WHEN convert THEN the full name is shown`() {
        // Act
        val actual = converter.convert(createMachine(name = "Pokemon Legendary", shortName = "   "))

        // Assert
        assertThat(actual.title).isEqualTo(stringReference("Pokemon Legendary"))
    }

    @Test
    fun `GIVEN no image WHEN convert THEN the url is empty rather than null`() {
        // Act
        val actual = converter.convert(createMachine(imageUrl = null))

        // Assert
        assertThat(actual.imageUrl).isEmpty()
    }

    @Test
    fun `GIVEN a buyback offer WHEN convert THEN its percent is badged`() {
        // Act
        val actual = converter.convert(createMachine(instantBuybackPercent = 80))

        // Assert
        assertThat(actual.badge)
            .isEqualTo(TextReference.Res(R.string.collectibles_main_pack_buyback_badge, wrappedList("80%")))
    }

    @Test
    fun `GIVEN no buyback offer WHEN convert THEN there is no badge`() {
        // Act
        val actual = converter.convert(createMachine(instantBuybackPercent = 0))

        // Assert
        assertThat(actual.badge).isNull()
    }

    @Test
    fun `GIVEN a negative buyback percent WHEN convert THEN there is no badge`() {
        // Act
        val actual = converter.convert(createMachine(instantBuybackPercent = -5))

        // Assert
        assertThat(actual.badge).isNull()
    }

    @Test
    fun `GIVEN a whole price WHEN convert THEN its decimals are dropped`() {
        // Act
        val actual = converter.convert(createMachine(price = BigDecimal("10.00")))

        // Assert
        assertThat(actual.price).isEqualTo(stringReference("$10"))
    }

    @Test
    fun `GIVEN a fractional price WHEN convert THEN its decimals are kept`() {
        // Act
        val actual = converter.convert(createMachine(price = BigDecimal("10.50")))

        // Assert
        assertThat(actual.price).isEqualTo(stringReference("$10.50"))
    }

    @Test
    fun `GIVEN a pack WHEN it is clicked THEN its external id is reported`() {
        // Arrange
        val pack = converter.convert(createMachine(id = "machine_42", externalId = "pokemon_50"))

        // Act
        pack.onClick()

        // Assert
        assertThat(clickedPackIds).containsExactly("pokemon_50")
    }
}