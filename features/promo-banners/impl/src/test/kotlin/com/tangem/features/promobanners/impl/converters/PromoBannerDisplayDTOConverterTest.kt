package com.tangem.features.promobanners.impl.converters

import com.tangem.datasource.api.tangemTech.models.promobanners.PromoBannerDisplayDTO
import com.tangem.features.promobanners.impl.model.PromoBannerPriority
import org.junit.jupiter.api.Test
import com.google.common.truth.Truth.assertThat

class PromoBannerDisplayDTOConverterTest {

    private val converter = PromoBannerDisplayDTOConverter()

    @Test
    fun `should convert DTO to domain model`() {
        val dto = PromoBannerDisplayDTO(
            id = 123,
            placeholder = "MAIN",
            priority = "HIGH",
            title = "Test Banner",
            subtitle = "Test subtitle",
            iconUrl = "https://example.com/icon.png",
            deeplink = "tangem://wallet",
            buttonEnabled = true,
            buttonText = "Click me",
            dismissable = true,
        )

        val result = converter.convert(dto)

        assertThat(result.id).isEqualTo(123)
        assertThat(result.placeholder).isEqualTo("MAIN")
        assertThat(result.priority).isEqualTo(PromoBannerPriority.HIGH)
        assertThat(result.title).isEqualTo("Test Banner")
        assertThat(result.subtitle).isEqualTo("Test subtitle")
        assertThat(result.iconUrl).isEqualTo("https://example.com/icon.png")
        assertThat(result.deeplink).isEqualTo("tangem://wallet")
        assertThat(result.isButtonEnabled).isTrue()
        assertThat(result.buttonText).isEqualTo("Click me")
        assertThat(result.isDismissable).isTrue()
    }

    @Test
    fun `should pass placeholder as is`() {
        val dto = createDTO(placeholder = "UNKNOWN")
        val result = converter.convert(dto)
        assertThat(result.placeholder).isEqualTo("UNKNOWN")
    }

    @Test
    fun `should map unknown priority to LOW`() {
        val dto = createDTO(priority = "UNKNOWN")
        val result = converter.convert(dto)
        assertThat(result.priority).isEqualTo(PromoBannerPriority.LOW)
    }

    @Test
    fun `should handle SHTORKA placeholder`() {
        val dto = createDTO(placeholder = "SHTORKA")
        val result = converter.convert(dto)
        assertThat(result.placeholder).isEqualTo("SHTORKA")
    }

    private fun createDTO(
        placeholder: String = "MAIN",
        priority: String = "MEDIUM",
    ) = PromoBannerDisplayDTO(
        id = 1,
        placeholder = placeholder,
        priority = priority,
        title = "Title",
        subtitle = "Subtitle",
        iconUrl = null,
        deeplink = null,
        buttonEnabled = false,
        buttonText = null,
        dismissable = false,
    )
}