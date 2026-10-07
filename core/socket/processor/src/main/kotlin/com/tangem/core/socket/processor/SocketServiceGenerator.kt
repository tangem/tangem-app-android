package com.tangem.core.socket.processor

import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MUTABLE_LIST
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName

internal object SocketServiceGenerator {

    private const val CHANNEL = "channel"
    private const val FAKE_BUFFER_CAPACITY = 64

    fun generate(model: SocketServiceModel): FileSpec {
        val service = model.declaration.toClassName()
        val baseName = service.simpleNames.joinToString(separator = "")
        val impl = ClassName(service.packageName, "${baseName}Impl")
        val fake = ClassName(service.packageName, "Fake$baseName")

        return FileSpec.builder(service.packageName, "${baseName}Socket")
            .addType(implType(model = model, service = service, impl = impl))
            .addType(fakeType(model = model, service = service, fake = fake))
            .addFunction(
                factoryFunction(
                    model = model,
                    service = service,
                    impl = impl,
                    baseName = baseName,
                ),
            )
            .build()
    }

    private fun implType(model: SocketServiceModel, service: ClassName, impl: ClassName): TypeSpec {
        val builder = TypeSpec.classBuilder(impl)
            .addModifiers(model.visibility)
            .addSuperinterface(service)
            .primaryConstructor(FunSpec.constructorBuilder().addParameter(CHANNEL, SocketNames.socketChannel).build())
            .addProperty(
                PropertySpec.builder(
                    CHANNEL,
                    SocketNames.socketChannel,
                    KModifier.PRIVATE,
                ).initializer(CHANNEL).build(),
            )

        model.methods.forEach { method ->
            val function = FunSpec.builder(method.name).addModifiers(KModifier.OVERRIDE)
            when (method) {
                is SocketMethod.Receive -> function
                    .returns(SocketNames.flow.parameterizedBy(method.type))
                    .addStatement("%L", method.receiveStatement())
                is SocketMethod.State -> function
                    .returns(method.returnType())
                    .addStatement("return %N.state", CHANNEL)
                is SocketMethod.Send -> function
                    .addParameter(method.parameterName, method.type)
                    .returns(BOOLEAN)
                    .addStatement(
                        "return %N.send(%M<%T>(), %N)",
                        CHANNEL,
                        SocketNames.serializer,
                        method.type,
                        method.parameterName,
                    )
            }
            builder.addFunction(function.build())
        }

        return builder.build()
    }

    private fun fakeType(model: SocketServiceModel, service: ClassName, fake: ClassName): TypeSpec {
        val builder = TypeSpec.classBuilder(fake)
            .addModifiers(model.visibility)
            .addSuperinterface(service)
            .addProperty(PropertySpec.builder("sendResult", BOOLEAN).mutable().initializer("true").build())

        model.methods.forEach { method ->
            val function = FunSpec.builder(method.name).addModifiers(KModifier.OVERRIDE)
            when (method) {
                is SocketMethod.Receive -> {
                    val type = SocketNames.mutableSharedFlow.parameterizedBy(method.type)
                    builder.addProperty(
                        PropertySpec.builder(method.name, type)
                            .initializer(
                                "%T(extraBufferCapacity = %L)",
                                SocketNames.mutableSharedFlow,
                                FAKE_BUFFER_CAPACITY,
                            )
                            .build(),
                    )
                    function.returns(
                        SocketNames.flow.parameterizedBy(method.type),
                    ).addStatement("return %N", method.name)
                }
                is SocketMethod.State -> {
                    val type = SocketNames.mutableStateFlow.parameterizedBy(SocketNames.socketState)
                    builder.addProperty(
                        PropertySpec.builder(method.name, type)
                            .initializer("%T(%T.Idle)", SocketNames.mutableStateFlow, SocketNames.socketState)
                            .build(),
                    )
                    function.returns(method.returnType())
                        .addStatement("return %N", method.name)
                }
                is SocketMethod.Send -> {
                    val sent = "${method.name}Sent"
                    builder.addProperty(
                        PropertySpec.builder(sent, MUTABLE_LIST.parameterizedBy(method.type))
                            .initializer("mutableListOf()")
                            .build(),
                    )
                    function.addParameter(method.parameterName, method.type)
                        .returns(BOOLEAN)
                        .addStatement("%N += %N", sent, method.parameterName)
                        .addStatement("return sendResult")
                }
            }
            builder.addFunction(function.build())
        }

        return builder.build()
    }

    private fun factoryFunction(model: SocketServiceModel, service: ClassName, impl: ClassName, baseName: String) =
        FunSpec.builder("create$baseName")
            .addModifiers(model.visibility)
            .receiver(SocketNames.socketChannelFactory)
            .addParameter("spec", SocketNames.socketSpec)
            .returns(service)
            .addStatement("return %T(create(spec))", impl)
            .build()

    private fun SocketMethod.State.returnType() =
        (if (isStateFlow) SocketNames.stateFlow else SocketNames.flow).parameterizedBy(SocketNames.socketState)

    private fun SocketMethod.Receive.receiveStatement(): CodeBlock = if (decodeAs == null) {
        CodeBlock.of("return %N.receive(%M<%T>())", CHANNEL, SocketNames.serializer, type)
    } else {
        CodeBlock.of(
            "return %N.receive(%M<%T>()).%M<%T>()",
            CHANNEL,
            SocketNames.serializer,
            decodeAs,
            SocketNames.filterIsInstance,
            type,
        )
    }
}