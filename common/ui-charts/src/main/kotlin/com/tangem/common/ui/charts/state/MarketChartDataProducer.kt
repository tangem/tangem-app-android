package com.tangem.common.ui.charts.state

import androidx.compose.runtime.Stable
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import com.tangem.common.ui.charts.state.converter.PointValuesConverter
import com.tangem.common.ui.charts.state.converter.PriceAndTimePointValuesConverter
import com.tangem.common.ui.charts.state.formatter.FormatterWrapWithCache
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * This class represents a transaction for updating the state and look of a Market Chart.
 *
 * @property chartLook The updated look of the Market Chart.
 * @property chartData The updated state of the Market Chart.
 */
class Transaction(
    private val currentData: MarketChartData,
    private val currentLook: MarketChartLook,
) {
    var chartLook: MarketChartLook? = null
    var chartData: MarketChartData.NoData? = null

    fun updateLook(block: (prev: MarketChartLook) -> MarketChartLook) {
        val newLook = block(currentLook)

        chartLook = newLook.copy(
            xAxisFormatter = FormatterWrapWithCache(newLook.xAxisFormatter),
            yAxisFormatter = FormatterWrapWithCache(newLook.yAxisFormatter),
        )
    }

    fun updateState(block: (prev: MarketChartData) -> MarketChartData.NoData) {
        chartData = block(currentData)
    }
}

/**
 * This class represents a transaction for updating the state and look of a Market Chart.
 * It extends the Transaction class and allows update state by data.
 *
 * @property chartData The updated state of the Market Chart.
 * @property chartLook The updated look of the Market Chart.
 */
class TransactionSuspend(
    private val currentData: MarketChartData,
    private val currentLook: MarketChartLook,
) {
    internal var nonSuspendTransaction: Transaction? = null
    var chartData: MarketChartData? = null
    var chartLook: MarketChartLook?
        get() = nonSuspendTransaction?.chartLook
        set(value) {
            if (nonSuspendTransaction == null) {
                nonSuspendTransaction = Transaction(currentData, currentLook)
            }
            nonSuspendTransaction?.chartLook = value
        }

    fun updateLook(block: (prev: MarketChartLook) -> MarketChartLook) {
        val newLook = block(currentLook)

        chartLook = newLook.copy(
            xAxisFormatter = FormatterWrapWithCache(newLook.xAxisFormatter),
            yAxisFormatter = FormatterWrapWithCache(newLook.yAxisFormatter),
        )
    }

    internal fun updateState(block: (prev: MarketChartData) -> MarketChartData) {
        chartData = block(currentData)
    }

    internal fun updateData(block: (prev: MarketChartData.Data) -> MarketChartData.Data) {
        chartData = when (val currentState = currentData) {
            is MarketChartData.Data -> block(currentState)
            else -> currentState
        }
    }
}

@Stable
class MarketChartDataProducer private constructor(
    initialData: MarketChartData,
    initialLook: MarketChartLook,
    val pointsValuesConverter: PointValuesConverter = PriceAndTimePointValuesConverter(needToFormatAxis = true),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    internal val dataState = MutableStateFlow(initialData)
    internal val lookState = MutableStateFlow(initialLook)
    internal val entries = MutableStateFlow<List<LineCartesianLayerModel.Entry>>(emptyList())
    internal val modelProducer = CartesianChartModelProducer(dispatcher = dispatcher)
    internal val rawData = MutableStateFlow<MarketChartRawData?>(null)
    private val mutex = Mutex()

    /**
     * This function runs a suspending transaction block to update the state and look of the Market Chart.
     */
    suspend fun runTransactionSuspend(block: TransactionSuspend.() -> Unit) = withContext(dispatcher) {
        mutex.withLock {
            handleTransactionSuspend(transaction = TransactionSuspend(dataState.value, lookState.value).apply(block))
        }
    }

    /**
     * This function runs a non-suspending transaction block to update the state and look of the Market Chart.
     */
    fun runTransaction(block: Transaction.() -> Unit) =
        handleTransaction(transaction = Transaction(dataState.value, lookState.value).apply(block))

    private suspend fun handleTransactionSuspend(transaction: TransactionSuspend) {
        val nonSuspendTransaction = transaction.nonSuspendTransaction
        val chartData = transaction.chartData
        val oldData = dataState.value

        if (chartData is MarketChartData.Data && (oldData !is MarketChartData.Data || oldData != chartData)) {
            (lookState.value.xAxisFormatter as? FormatterWrapWithCache)?.clearCache()
            (lookState.value.yAxisFormatter as? FormatterWrapWithCache)?.clearCache()

            val rawData = pointsValuesConverter.convert(chartData)

            val entriesLocal =
                rawData.x.mapIndexed { index, fl -> LineCartesianLayerModel.Entry(fl, rawData.y[index]) }

            currentCoroutineContext().ensureActive()

            runCatching {
                modelProducer.runTransaction {
                    add(LineCartesianLayerModel.Partial(series = listOf(entriesLocal)))
                }
            }

            entries.value = entriesLocal
            dataState.value = chartData
            this.rawData.value = rawData

            delay(timeMillis = 200)
        } else if (chartData != null) {
            dataState.value = chartData
        }

        nonSuspendTransaction?.let { handleTransaction(it) }
    }

    private fun handleTransaction(transaction: Transaction) {
        transaction.chartData?.let {
            dataState.value = it
        }
        transaction.chartLook?.let {
            lookState.value = it
        }
    }

    companion object {
        private val initialData: MarketChartData = MarketChartData.NoData.Empty
        private val initialLook: MarketChartLook = MarketChartLook()

        /**
         * This function builds a MarketChartDataProducer with the given parameters.
         * It runs a suspending transaction block to initialize the data and look of the Market Chart.
         *
         * @param dispatcher The dispatcher to be used for data updates.
         * @param block The transaction block to be run.
         * @return A MarketChartDataProducer.
         */
        suspend fun buildSuspend(
            pointsValuesConverter: PointValuesConverter = PriceAndTimePointValuesConverter(needToFormatAxis = true),
            dispatcher: CoroutineDispatcher = Dispatchers.Default,
            block: TransactionSuspend.() -> Unit,
        ): MarketChartDataProducer {
            val transaction = TransactionSuspend(initialData, initialLook).apply(block)

            return MarketChartDataProducer(
                initialData = initialData,
                initialLook = initialLook,
                dispatcher = dispatcher,
                pointsValuesConverter = pointsValuesConverter,
            ).apply {
                handleTransactionSuspend(transaction)
            }
        }

        /**
         * This function builds a MarketChartDataProducer with the given parameters.
         * It runs a non-suspending transaction block to initialize the data and look of the Market Chart.
         *
         * @param dispatcher The dispatcher to be used for data updates.
         * @param block The transaction block to be run.
         * @return A MarketChartDataProducer.
         */
        fun build(
            pointsValuesConverter: PointValuesConverter = PriceAndTimePointValuesConverter(needToFormatAxis = true),
            dispatcher: CoroutineDispatcher = Dispatchers.Default,
            block: Transaction.() -> Unit,
        ): MarketChartDataProducer {
            val transaction = Transaction(initialData, initialLook).apply(block)

            return MarketChartDataProducer(
                initialData = transaction.chartData ?: initialData,
                initialLook = transaction.chartLook ?: initialLook,
                dispatcher = dispatcher,
                pointsValuesConverter = pointsValuesConverter,
            )
        }
    }
}
