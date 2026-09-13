package com.tangem.features.txhistory.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.express.models.ExpressExchangeStatus
import com.tangem.domain.express.models.ExpressOnrampStatus
import com.tangem.domain.models.network.SdkAmount
import com.tangem.domain.models.network.TxInfo.TransactionStatus
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.domain.txhistory.model.TxHistoryInfo
import com.tangem.features.txhistory.converter.TxDetailsConverterTestBase
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class TxHistoryAnalyticsMappersTest : TxDetailsConverterTestBase() {

    internal data class TypeModel(val tx: TxHistoryInfo, val expected: String)
    internal data class StatusModel(val tx: TxHistoryInfo, val expected: String)
    internal data class TitleModel(val type: String, val status: String, val expected: String)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ToAnalyticsType {

        @ParameterizedTest
        @ProvideTestModels
        fun toAnalyticsType(model: TypeModel) {
            // Act
            val actual = model.tx.toAnalyticsType()

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            TypeModel(onChain(TransactionType.Transfer, isOutgoing = true), expected = "Send"),
            TypeModel(onChain(TransactionType.Transfer, isOutgoing = false), expected = "Receive"),
            TypeModel(onChain(TransactionType.Approve(amount = SdkAmount(currencySymbol = "USDT", value = null, decimals = 6), address = "0xspender")), expected = "Approve"),
            TypeModel(onChain(TransactionType.Swap), expected = "Swap"),
            TypeModel(onChain(TransactionType.Staking.Stake), expected = "Staking"),
            TypeModel(onChain(TransactionType.Staking.Unstake), expected = "Staking"),
            TypeModel(onChain(TransactionType.YieldSupply.Topup), expected = "Yield Supply"),
            TypeModel(onChain(TransactionType.GaslessFee), expected = "Gasless Fee"),
            TypeModel(onChain(TransactionType.Operation(name = "Mint")), expected = "Mint"),
            TypeModel(onChain(TransactionType.UnknownOperation), expected = "Unknown"),
            TypeModel(expressSwap(status = ExpressExchangeStatus.Finished), expected = "Swap"),
            TypeModel(expressOnramp(status = ExpressOnrampStatus.Finished), expected = "Onramp"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ToAnalyticsStatus {

        @ParameterizedTest
        @ProvideTestModels
        fun toAnalyticsStatus(model: StatusModel) {
            // Act
            val actual = model.tx.toAnalyticsStatus()

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            StatusModel(onChain(TransactionType.Transfer, status = TransactionStatus.Confirmed), expected = "Confirmed"),
            StatusModel(onChain(TransactionType.Transfer, status = TransactionStatus.Unconfirmed), expected = "Pending"),
            StatusModel(onChain(TransactionType.Transfer, status = TransactionStatus.Failed), expected = "Failed"),
            StatusModel(expressSwap(status = ExpressExchangeStatus.Finished), expected = "Confirmed"),
            StatusModel(expressSwap(status = ExpressExchangeStatus.Waiting), expected = "Pending"),
            StatusModel(expressSwap(status = ExpressExchangeStatus.Failed), expected = "Failed"),
            StatusModel(expressSwap(status = ExpressExchangeStatus.Expired), expected = "Failed"),
            StatusModel(expressOnramp(status = ExpressOnrampStatus.Finished), expected = "Confirmed"),
            StatusModel(expressOnramp(status = ExpressOnrampStatus.WaitingForPayment), expected = "Pending"),
            StatusModel(expressOnramp(status = ExpressOnrampStatus.Failed), expected = "Failed"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AnalyticsTitle {

        @ParameterizedTest
        @ProvideTestModels
        fun analyticsTitle(model: TitleModel) {
            // Act
            val actual = analyticsTitle(type = model.type, status = model.status)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            TitleModel(type = "Send", status = "Confirmed", expected = "Send"),
            TitleModel(type = "Send", status = "Failed", expected = "Send Failed"),
            TitleModel(type = "Send", status = "Pending", expected = "Send Pending"),
            TitleModel(type = "Swap", status = "Unknown", expected = "Swap"),
        )
    }
}