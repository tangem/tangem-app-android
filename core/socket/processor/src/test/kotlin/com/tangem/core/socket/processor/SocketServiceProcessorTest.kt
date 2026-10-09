package com.tangem.core.socket.processor

import com.google.common.truth.Truth.assertThat
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.kspSourcesDir
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File

@OptIn(ExperimentalCompilerApi::class)
internal class SocketServiceProcessorTest {

    @Test
    fun `GIVEN valid service WHEN processed THEN impl, fake and factory compile`() {
        // Arrange
        val source = service(
            """
            @Receive fun tickers(): Flow<TickerDto>
            @Receive fun state(): Flow<SocketState>
            @Send fun subscribe(frame: TickerDto): Boolean
            """,
        )

        // Act
        val (result, generated) = compile(source)

        // Assert
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(generated).contains("public class TickerSocketImpl(")
        assertThat(generated).contains("private val tickersDeserializer: KSerializer<TickerDto> = serializer<TickerDto>()")
        assertThat(generated).contains("channel.receive(tickersDeserializer)")
        assertThat(generated).contains("this.channel.send(serializer<TickerDto>(),")
        assertThat(generated).contains("public class FakeTickerSocket : TickerSocket")
        assertThat(generated).contains("public val subscribeSent: MutableList<TickerDto>")
        assertThat(generated).contains("public fun SocketChannelFactory.createTickerSocket(spec: SocketSpec)")
    }

    @ParameterizedTest
    @ValueSource(strings = ["channel", "sendResult", "subscribeSent"])
    fun `GIVEN send parameter named like a generated member WHEN processed THEN impl and fake compile`(name: String) {
        // Arrange
        val source = service("@Send fun subscribe($name: TickerDto): Boolean")

        // Act
        val (result, generated) = compile(source)

        // Assert
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `GIVEN StateFlow state WHEN processed THEN impl returns the channel StateFlow`() {
        // Arrange
        val source = service("@Receive fun state(): StateFlow<SocketState>")

        // Act
        val (result, generated) = compile(source)

        // Assert
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(generated).contains("override fun state(): StateFlow<SocketState> = channel.state")
    }

    @Test
    fun `GIVEN subtype of a sealed serializable base WHEN processed THEN it is decoded through the base`() {
        // Arrange
        val source = service("@Receive fun trades(): Flow<EventDto.Trade>")

        // Act
        val (result, generated) = compile(source)

        // Assert
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(generated).contains("channel.receive(tradesDeserializer).filterIsInstance<EventDto.Trade>()")
    }

    @Test
    fun `GIVEN plain DTO WHEN processed THEN it is decoded directly`() {
        // Arrange
        val source = service("@Receive fun tickers(): Flow<TickerDto>")

        // Act
        val (_, generated) = compile(source)

        // Assert
        assertThat(generated).doesNotContain("filterIsInstance")
    }

    @Test
    fun `GIVEN invalid signatures WHEN processed THEN compilation fails with a message per function`() {
        // Arrange
        val source = service(
            """
            @Receive fun withParameter(id: String): Flow<TickerDto>
            @Receive fun notFlow(): TickerDto
            @Receive fun stateFlowOfDto(): StateFlow<TickerDto>
            @Send fun returnsUnit(frame: TickerDto)
            fun notAnnotated(): Flow<TickerDto>
            """,
        )

        // Act
        val (result, _) = compile(source)

        // Assert
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("withParameter: @Receive must take no parameters and return Flow<T>")
        assertThat(result.messages).contains("notFlow: @Receive must take no parameters and return Flow<T>")
        assertThat(result.messages).contains("stateFlowOfDto: @Receive may return StateFlow only for SocketState")
        assertThat(result.messages).contains("returnsUnit: @Send must take exactly one parameter and return Boolean")
        assertThat(result.messages).contains("notAnnotated: must be annotated with exactly one of @Receive or @Send")
    }

    @Test
    fun `GIVEN overloaded send functions WHEN processed THEN compilation fails with the duplicate name`() {
        // Arrange
        val source = service(
            """
            @Send fun subscribe(frame: TickerDto): Boolean
            @Send fun subscribe(symbol: String): Boolean
            """,
        )

        // Act
        val (result, _) = compile(source)

        // Assert
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("@SocketService functions must have unique names: subscribe")
    }

    @Test
    fun `GIVEN names clashing in the fake WHEN processed THEN compilation fails with the clashing name`() {
        // Arrange
        val source = service(
            """
            @Receive fun subscribeSent(): Flow<TickerDto>
            @Send fun subscribe(frame: TickerDto): Boolean
            """,
        )

        // Act
        val (result, _) = compile(source)

        // Assert
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("@SocketService names clash in the generated fake: subscribeSent")
    }

    @Test
    fun `GIVEN a name clashing with a fake accessor WHEN processed THEN compilation fails with the name`() {
        // Arrange
        val source = service(
            """
            @Receive fun isConnected(): Flow<TickerDto>
            @Receive fun tickers(): Flow<TickerDto>
            @Receive fun getTickers(): Flow<TickerDto>
            """,
        )

        // Act
        val (result, _) = compile(source)

        // Assert
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages)
            .contains("@SocketService names clash with the generated fake's JVM accessors: isConnected, getTickers")
    }

    @Test
    fun `GIVEN suspend, extension and vararg functions WHEN processed THEN each is rejected`() {
        // Arrange
        val source = service(
            """
            @Receive suspend fun tickers(): Flow<TickerDto>
            @Receive fun String.trades(): Flow<TickerDto>
            @Send fun subscribe(vararg frames: TickerDto): Boolean
            """,
        )

        // Act
        val (result, _) = compile(source)

        // Assert
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("tickers: must not be suspend")
        assertThat(result.messages).contains("trades: must not have an extension receiver")
        assertThat(result.messages).contains("subscribe: @Send must take exactly one parameter and return Boolean")
    }

    @Test
    fun `GIVEN service nested in an internal class WHEN processed THEN generated code is internal and compiles`() {
        // Arrange
        val source = SourceFile.kotlin(
            "Nested.kt",
            """
            package test

            import com.tangem.core.socket.Receive
            import com.tangem.core.socket.SocketService
            import com.tangem.core.socket.SocketState
            import kotlinx.coroutines.flow.Flow

            internal class Outer {
                @SocketService
                interface StateSocket {
                    @Receive fun state(): Flow<SocketState>
                }
            }
            """.trimIndent(),
        )

        // Act
        val (result, generated) = compile(source)

        // Assert
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(generated).contains("internal class OuterStateSocketImpl(")
    }

    @Test
    fun `GIVEN function inherited from a generic interface WHEN processed THEN impl uses the type argument`() {
        // Arrange
        val source = SourceFile.kotlin(
            "EventsSocket.kt",
            """
            package test

            import com.tangem.core.socket.Receive
            import com.tangem.core.socket.Send
            import com.tangem.core.socket.SocketService
            import kotlinx.coroutines.flow.Flow
            import kotlinx.serialization.Serializable

            @Serializable
            data class EventDto(val id: String)

            interface Base<T> {
                @Receive fun events(): Flow<T>
                @Send fun publish(event: T): Boolean
            }

            @SocketService
            interface EventsSocket : Base<EventDto>
            """.trimIndent(),
        )

        // Act
        val (result, generated) = compile(source)

        // Assert
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(generated).contains("override fun events(): Flow<EventDto>")
        assertThat(generated).contains("override fun publish(event: EventDto): Boolean")
    }

    @Test
    fun `GIVEN annotated class WHEN processed THEN compilation fails`() {
        // Arrange
        val source = SourceFile.kotlin(
            "NotInterface.kt",
            """
            package test

            import com.tangem.core.socket.SocketService

            @SocketService
            abstract class NotInterface
            """.trimIndent(),
        )

        // Act
        val (result, _) = compile(source)

        // Assert
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("@SocketService must annotate an interface")
    }

    private fun service(body: String): SourceFile = SourceFile.kotlin(
        "TickerSocket.kt",
        """
        package test

        import com.tangem.core.socket.Receive
        import com.tangem.core.socket.Send
        import com.tangem.core.socket.SocketService
        import com.tangem.core.socket.SocketState
        import kotlinx.coroutines.flow.Flow
        import kotlinx.coroutines.flow.StateFlow
        import kotlinx.serialization.Serializable

        @Serializable
        data class TickerDto(val symbol: String)

        @Serializable
        sealed interface EventDto {
            @Serializable
            data class Trade(val price: String) : EventDto
        }

        @SocketService
        interface TickerSocket {
        ${body.trimIndent()}
        }
        """.trimIndent(),
    )

    private fun compile(source: SourceFile): Pair<JvmCompilationResult, String> {
        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            inheritClassPath = true
            languageVersion = "1.9"
            kotlincArguments += "-Xskip-prerelease-check"
            configureKsp(useKsp2 = false) {
                withCompilation = true
                symbolProcessorProviders += SocketServiceProcessorProvider()
            }
        }
        val result = compilation.compile()
        val generated = compilation.kspSourcesDir.walkTopDown()
            .filter(File::isFile)
            .joinToString(separator = "\n") { it.readText() }
        return result to generated
    }
}