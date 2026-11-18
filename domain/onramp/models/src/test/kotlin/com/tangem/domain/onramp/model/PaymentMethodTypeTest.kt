package com.tangem.domain.onramp.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.onramp.model.PaymentMethodType.PaymentSpeed
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class PaymentMethodTypeTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetType {

        @ParameterizedTest
        @ProvideTestModels
        fun getType(model: GetTypeCase) {
            // Act
            val actual = PaymentMethodType.getType(model.id)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            GetTypeCase(id = "google-pay", expected = PaymentMethodType.GOOGLE_PAY),
            GetTypeCase(id = "card", expected = PaymentMethodType.CARD),
            GetTypeCase(id = "invoice-revolut-pay", expected = PaymentMethodType.REVOLUT_PAY),
            GetTypeCase(id = "sepa", expected = PaymentMethodType.SEPA),
            GetTypeCase(id = "moonpay-balance", expected = PaymentMethodType.MOONPAY_BALANCE),
            GetTypeCase(id = "unknown", expected = PaymentMethodType.OTHER),
            GetTypeCase(id = "", expected = PaymentMethodType.OTHER),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetProcessingSpeed {

        @ParameterizedTest
        @ProvideTestModels
        fun getProcessingSpeed(model: SpeedCase) {
            // Act
            val actual = model.type.getProcessingSpeed()

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            SpeedCase(type = PaymentMethodType.GOOGLE_PAY, expected = PaymentSpeed.Instant),
            SpeedCase(type = PaymentMethodType.REVOLUT_PAY, expected = PaymentSpeed.Instant),
            SpeedCase(type = PaymentMethodType.MOONPAY_BALANCE, expected = PaymentSpeed.Instant),
            SpeedCase(type = PaymentMethodType.CARD, expected = PaymentSpeed.FewMin),
            SpeedCase(type = PaymentMethodType.SEPA, expected = PaymentSpeed.FewDays),
            SpeedCase(type = PaymentMethodType.OTHER, expected = PaymentSpeed.PlentyDays),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsInstant {

        @ParameterizedTest
        @ProvideTestModels
        fun isInstant(model: IsInstantCase) {
            // Act
            val actual = model.type.isInstant()

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            IsInstantCase(type = PaymentMethodType.GOOGLE_PAY, expected = true),
            IsInstantCase(type = PaymentMethodType.REVOLUT_PAY, expected = true),
            IsInstantCase(type = PaymentMethodType.MOONPAY_BALANCE, expected = true),
            IsInstantCase(type = PaymentMethodType.CARD, expected = false),
            IsInstantCase(type = PaymentMethodType.SEPA, expected = false),
            IsInstantCase(type = PaymentMethodType.OTHER, expected = false),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetPriorityForMethod {

        @ParameterizedTest
        @ProvideTestModels
        fun getPriorityForMethod(model: PriorityCase) {
            // Act
            val actual = model.type.getPriorityForMethod(isGooglePayEnabled = model.isGooglePayEnabled)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            PriorityCase(type = PaymentMethodType.GOOGLE_PAY, isGooglePayEnabled = true, expected = 0),
            PriorityCase(type = PaymentMethodType.CARD, isGooglePayEnabled = true, expected = 1),
            PriorityCase(type = PaymentMethodType.REVOLUT_PAY, isGooglePayEnabled = true, expected = 2),
            PriorityCase(type = PaymentMethodType.SEPA, isGooglePayEnabled = true, expected = 3),
            PriorityCase(type = PaymentMethodType.MOONPAY_BALANCE, isGooglePayEnabled = true, expected = 4),
            PriorityCase(type = PaymentMethodType.OTHER, isGooglePayEnabled = true, expected = 4),
            PriorityCase(type = PaymentMethodType.REVOLUT_PAY, isGooglePayEnabled = false, expected = 1),
            PriorityCase(type = PaymentMethodType.CARD, isGooglePayEnabled = false, expected = 2),
            PriorityCase(type = PaymentMethodType.SEPA, isGooglePayEnabled = false, expected = 2),
            PriorityCase(type = PaymentMethodType.OTHER, isGooglePayEnabled = false, expected = 3),
            PriorityCase(type = PaymentMethodType.MOONPAY_BALANCE, isGooglePayEnabled = false, expected = 4),
            PriorityCase(type = PaymentMethodType.GOOGLE_PAY, isGooglePayEnabled = false, expected = 4),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetPriorityBySpeed {

        @ParameterizedTest
        @ProvideTestModels
        fun getPriorityBySpeed(model: PriorityCase) {
            // Act
            val actual = model.type.getPriorityBySpeed(isGooglePayEnabled = model.isGooglePayEnabled)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            PriorityCase(type = PaymentMethodType.GOOGLE_PAY, isGooglePayEnabled = true, expected = 0),
            PriorityCase(type = PaymentMethodType.REVOLUT_PAY, isGooglePayEnabled = true, expected = 1),
            PriorityCase(type = PaymentMethodType.CARD, isGooglePayEnabled = true, expected = 2),
            PriorityCase(type = PaymentMethodType.SEPA, isGooglePayEnabled = true, expected = 3),
            PriorityCase(type = PaymentMethodType.MOONPAY_BALANCE, isGooglePayEnabled = true, expected = 4),
            PriorityCase(type = PaymentMethodType.OTHER, isGooglePayEnabled = true, expected = 4),
            PriorityCase(type = PaymentMethodType.REVOLUT_PAY, isGooglePayEnabled = false, expected = 0),
            PriorityCase(type = PaymentMethodType.CARD, isGooglePayEnabled = false, expected = 1),
            PriorityCase(type = PaymentMethodType.SEPA, isGooglePayEnabled = false, expected = 2),
            PriorityCase(type = PaymentMethodType.MOONPAY_BALANCE, isGooglePayEnabled = false, expected = 3),
            PriorityCase(type = PaymentMethodType.OTHER, isGooglePayEnabled = false, expected = 3),
            PriorityCase(type = PaymentMethodType.GOOGLE_PAY, isGooglePayEnabled = false, expected = 4),
        )
    }

    data class GetTypeCase(val id: String, val expected: PaymentMethodType)

    data class SpeedCase(val type: PaymentMethodType, val expected: PaymentSpeed)

    data class IsInstantCase(val type: PaymentMethodType, val expected: Boolean)

    data class PriorityCase(val type: PaymentMethodType, val isGooglePayEnabled: Boolean, val expected: Int)
}