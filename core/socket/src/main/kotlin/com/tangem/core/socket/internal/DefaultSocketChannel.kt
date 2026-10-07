package com.tangem.core.socket.internal

import com.tangem.core.socket.ClosePolicy
import com.tangem.core.socket.KeepAlive
import com.tangem.core.socket.SocketChannel
import com.tangem.core.socket.SocketClose
import com.tangem.core.socket.SocketCloseCode
import com.tangem.core.socket.SocketConnectContext
import com.tangem.core.socket.SocketEnvironment
import com.tangem.core.socket.SocketSession
import com.tangem.core.socket.SocketSpec
import com.tangem.core.socket.SocketState
import com.tangem.core.socket.transport.SocketConnection
import com.tangem.core.socket.transport.SocketRequest
import com.tangem.core.socket.transport.SocketTransport
import com.tangem.core.socket.transport.TransportEvent
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlin.time.Duration.Companion.seconds

internal class DefaultSocketChannel(
    private val spec: SocketSpec,
    private val transport: SocketTransport,
    private val environment: SocketEnvironment,
    private val json: Json,
    private val scope: CoroutineScope,
) : SocketChannel {

    override val state: StateFlow<SocketState>
        field = MutableStateFlow<SocketState>(SocketState.Idle)

    private val frames = MutableSharedFlow<JsonElement>(
        extraBufferCapacity = FRAME_BUFFER_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val decodedByType = ConcurrentHashMap<Pair<SerialDescriptor, Class<*>>, Flow<Any?>>()

    private val collectorCount = MutableStateFlow(0)

    private val sendLock = Any()

    private var connection: SocketConnection? = null

    private var isOnOpenRunning = false

    private val sendsQueuedDuringOnOpen = mutableListOf<String>()

    init {
        scope.launch {
            state.collect { state -> TangemLogger.i("Socket ${spec.url.substringBefore('?')}: $state") }
        }
        scope.launch {
            while (true) {
                runSuspendCatching { runGate() }.onFailure { error ->
                    TangemLogger.e("Socket connection loop failed, restarting", error)
                }
                state.value = SocketState.Idle
                delay(LOOP_RESTART_DELAY)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> receive(deserializer: DeserializationStrategy<T>): Flow<T> {
        val decoded = decodedByType.computeIfAbsent(deserializer.descriptor to deserializer.javaClass) {
            frames.mapNotNull { frame -> decodeOrNull(deserializer, frame) }
                .shareIn(scope, SharingStarted.WhileSubscribed(spec.idleTimeout.inWholeMilliseconds))
        } as Flow<T>
        return decoded
            .buffer(COLLECTOR_BUFFER_CAPACITY, BufferOverflow.DROP_OLDEST)
            .onStart { collectorCount.update { count -> count + 1 } }
            .onCompletion { collectorCount.update { count -> count - 1 } }
    }

    override fun send(text: String): Boolean = synchronized(sendLock) {
        if (isOnOpenRunning) {
            sendsQueuedDuringOnOpen += text
            return true
        }
        connection?.send(text) == true
    }

    override fun <T> send(serializer: SerializationStrategy<T>, value: T): Boolean =
        send(json.encodeToString(serializer, value))

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun runGate() {
        hasCollectors()
            .flatMapLatest { hasCollectors ->
                if (hasCollectors) {
                    combine(
                        flow = environment.isForeground(),
                        flow2 = environment.defaultNetwork(),
                        flow3 = spec.lifecycle.isActive(),
                        flow4 = reconnectGeneration(),
                        transform = ::gateOf,
                    )
                } else {
                    flowOf(Gate.Closed(SocketState.Idle))
                }
            }
            .distinctUntilChanged()
            .collectLatest { gate ->
                when (gate) {
                    is Gate.Open -> connectWithRetries()
                    is Gate.Closed -> state.value = gate.state
                }
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun hasCollectors(): Flow<Boolean> = collectorCount
        .map { count -> count > 0 }
        .distinctUntilChanged()
        .transformLatest { hasCollectors ->
            if (!hasCollectors) delay(spec.idleTimeout)
            emit(hasCollectors)
        }

    private fun reconnectGeneration(): Flow<Int> = (spec.reconnectOn ?: flowOf(Unit))
        .withIndex()
        .map { it.index }
        .onStart { emit(0) }
        .distinctUntilChanged()

    private fun gateOf(isForeground: Boolean, network: String?, isActive: Boolean, generation: Int): Gate = when {
        !isActive -> Gate.Closed(SocketState.Idle)
        !isForeground -> Gate.Closed(SocketState.Paused(SocketState.PauseReason.Background))
        network == null -> Gate.Closed(SocketState.Paused(SocketState.PauseReason.NoNetwork))
        else -> Gate.Open(generation = generation, network = network)
    }

    private suspend fun connectWithRetries(): Nothing {
        var attempt = 0
        var previousClose: SocketClose? = null
        while (true) {
            state.value = SocketState.Connecting(attempt)
            val close = connectOnce(SocketConnectContext(attempt, previousClose), onHealthy = { attempt = 0 })
            previousClose = close
            val retryDelay = when (val decision = spec.closePolicy.decide(close)) {
                ClosePolicy.Decision.Stop -> {
                    state.value = SocketState.Stopped(close.code, close.reason)
                    awaitCancellation()
                }
                is ClosePolicy.Decision.RetryAfter -> decision.delay
                ClosePolicy.Decision.Retry -> spec.backoff.delayFor(attempt).also { delay ->
                    require(!delay.isNegative()) { "Backoff returned a negative delay: $delay" }
                }
            }
            attempt++
            state.value = SocketState.WaitingToRetry(attempt, retryDelay)
            delay(retryDelay)
        }
    }

    private suspend fun connectOnce(context: SocketConnectContext, onHealthy: () -> Unit): SocketClose {
        val headers = spec.headers.runSuspendCatching { resolve(context) }.getOrElse { error ->
            return SocketClose(SocketCloseCode.ABNORMAL, "Headers failed: ${error.message}")
        }
        val opened = runCatching { (spec.transport ?: transport).open(SocketRequest(spec.url, headers)) }
            .getOrElse { error -> return SocketClose(SocketCloseCode.ABNORMAL, "Open failed: ${error.message}") }
        return try {
            coroutineScope {
                var keepAlive: Job? = null
                var healthyTimer: Job? = null
                var isHealthy = false
                val markHealthy = {
                    if (!isHealthy) {
                        isHealthy = true
                        onHealthy()
                    }
                }
                var closed: SocketClose? = null
                for (event in opened.events) {
                    when (event) {
                        TransportEvent.Opened -> {
                            synchronized(sendLock) { isOnOpenRunning = true }
                            val openError = runSuspendCatching { spec.onOpen(ConnectionSession(opened)) }
                                .exceptionOrNull()
                            if (openError != null) {
                                closed = SocketClose(SocketCloseCode.ABNORMAL, "onOpen failed: ${openError.message}")
                                break
                            }
                            synchronized(sendLock) {
                                connection = opened
                                isOnOpenRunning = false
                                sendsQueuedDuringOnOpen.forEach(opened::send)
                                sendsQueuedDuringOnOpen.clear()
                            }
                            state.value = SocketState.Connected
                            keepAlive = launchKeepAlive(opened)
                            healthyTimer = launch {
                                delay(HEALTHY_CONNECTION_AGE)
                                markHealthy()
                            }
                        }
                        is TransportEvent.Message -> {
                            markHealthy()
                            dispatch(opened, event.text)
                        }
                        is TransportEvent.Closed -> {
                            closed = event.close
                            break
                        }
                    }
                }
                keepAlive?.cancel()
                healthyTimer?.cancel()
                closed ?: SocketClose(SocketCloseCode.ABNORMAL, "Transport ended without a close")
            }
        } finally {
            synchronized(sendLock) {
                connection = null
                isOnOpenRunning = false
                sendsQueuedDuringOnOpen.clear()
            }
            opened.close(SocketCloseCode.NORMAL, "")
        }
    }

    private fun CoroutineScope.launchKeepAlive(connection: SocketConnection): Job? {
        val keepAlive = spec.keepAlive as? KeepAlive.ClientSends ?: return null
        return launch {
            while (true) {
                delay(keepAlive.interval)
                connection.send(keepAlive.text)
            }
        }
    }

    private fun dispatch(connection: SocketConnection, text: String) {
        val keepAlive = spec.keepAlive
        if (keepAlive is KeepAlive.ServerSends && text == keepAlive.text) {
            connection.send(keepAlive.reply)
            return
        }

        val frame = parseOrNull(text) ?: return
        if (spec.isSplitArrayFrames && frame is JsonArray) {
            frame.forEach(frames::tryEmit)
        } else {
            frames.tryEmit(frame)
        }
    }

    private fun parseOrNull(text: String): JsonElement? {
        val start = text.firstOrNull { !it.isWhitespace() }
        if (start != '{' && start != '[') return null

        return runCatching { json.parseToJsonElement(text) }.getOrNull()
    }

    private fun <T> decodeOrNull(deserializer: DeserializationStrategy<T>, frame: JsonElement): T? =
        runCatching { json.decodeFromJsonElement(deserializer, frame) }
            .onFailure { error ->
                if (error !is SerializationException && error !is IllegalArgumentException) {
                    TangemLogger.e("Socket frame decoding failed for ${deserializer.descriptor.serialName}", error)
                }
            }
            .getOrNull()

    private inner class ConnectionSession(private val connection: SocketConnection) : SocketSession {

        override fun send(text: String): Boolean = connection.send(text)

        override fun <T> send(serializer: SerializationStrategy<T>, value: T): Boolean =
            send(json.encodeToString(serializer, value))
    }

    private sealed interface Gate {

        data class Open(val generation: Int, val network: String) : Gate

        data class Closed(val state: SocketState) : Gate
    }

    private companion object {
        const val FRAME_BUFFER_CAPACITY = 256
        const val COLLECTOR_BUFFER_CAPACITY = 256
        val HEALTHY_CONNECTION_AGE = 30.seconds
        val LOOP_RESTART_DELAY = 5.seconds
    }
}