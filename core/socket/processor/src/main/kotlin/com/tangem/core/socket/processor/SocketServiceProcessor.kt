package com.tangem.core.socket.processor

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Visibility
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

internal class SocketServiceProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val (valid, deferred) = resolver.getSymbolsWithAnnotation(
            SocketNames.SOCKET_SERVICE,
        ).partition { it.validate() }

        valid.forEach { symbol ->
            val model = parseService(symbol) ?: return@forEach
            val sources = model.declaration.consultedFiles().toTypedArray()
            SocketServiceGenerator.generate(model).writeTo(codeGenerator, Dependencies(aggregating = false, *sources))
        }

        return deferred
    }

    private fun parseService(symbol: KSAnnotated): SocketServiceModel? {
        if (symbol !is KSClassDeclaration || symbol.classKind != ClassKind.INTERFACE) {
            logger.error("@SocketService must annotate an interface", symbol)
            return null
        }
        if (symbol.typeParameters.isNotEmpty()) {
            logger.error("@SocketService interface must not declare type parameters", symbol)
            return null
        }
        if (symbol.getAllProperties().any { it.isAbstract() }) {
            logger.error("@SocketService interface must declare functions only", symbol)
            return null
        }
        val visibilities = generateSequence<KSDeclaration>(symbol) { it.parentDeclaration }.map { it.getVisibility() }
        if (visibilities.any { it == Visibility.PRIVATE || it == Visibility.PROTECTED }) {
            logger.error("@SocketService interface and its enclosing classes must not be private", symbol)
            return null
        }

        val methods = symbol.getAllFunctions().filter { it.isAbstract }.map(::parseMethod).toList()
        if (methods.any { it == null }) return null

        val duplicateNames = methods.filterNotNull().groupBy { it.name }.filterValues { it.size > 1 }.keys
        if (duplicateNames.isNotEmpty()) {
            logger.error("@SocketService functions must have unique names: ${duplicateNames.joinToString()}", symbol)
            return null
        }

        val fakeClashes = methods.filterNotNull()
            .fakePropertyNames()
            .groupBy { it }
            .filterValues { it.size > 1 }
            .keys
        if (fakeClashes.isNotEmpty()) {
            logger.error("@SocketService names clash in the generated fake: ${fakeClashes.joinToString()}", symbol)
            return null
        }

        val accessorClashes = methods.filterNotNull().fakeAccessorClashes()
        if (accessorClashes.isNotEmpty()) {
            logger.error(
                "@SocketService names clash with the generated fake's JVM accessors: ${accessorClashes.joinToString()}",
                symbol,
            )
            return null
        }

        return SocketServiceModel(
            declaration = symbol,
            visibility = if (Visibility.INTERNAL in visibilities) KModifier.INTERNAL else KModifier.PUBLIC,
            methods = methods.filterNotNull(),
        )
    }

    private fun parseMethod(function: KSFunctionDeclaration): SocketMethod? {
        val annotations = function.annotations
            .mapNotNull { annotation ->
                annotation.annotationType.resolve()
                    .declaration.qualifiedName?.asString()
            }
        val isReceive = SocketNames.RECEIVE in annotations
        val isSend = SocketNames.SEND in annotations

        return when {
            function.typeParameters.isNotEmpty() -> fail(function, "must not declare type parameters")
            Modifier.SUSPEND in function.modifiers -> fail(function, "must not be suspend")
            function.extensionReceiver != null -> fail(function, "must not have an extension receiver")
            isReceive == isSend -> fail(function, "must be annotated with exactly one of @Receive or @Send")
            isReceive -> parseReceive(function)
            else -> parseSend(function)
        }
    }

    private fun parseReceive(function: KSFunctionDeclaration): SocketMethod? {
        val returnType = function.returnType?.resolve()
        val returnTypeName = returnType?.declaration?.qualifiedName?.asString()
        val isStateFlow = returnTypeName == SocketNames.STATE_FLOW
        val isAnyFlow = returnTypeName == SocketNames.FLOW || isStateFlow
        if (function.parameters.isNotEmpty() || returnType == null || !isAnyFlow) {
            return fail(function, "@Receive must take no parameters and return Flow<T>")
        }

        val elementType = returnType.arguments.single().type
            ?: return fail(function, "@Receive needs a concrete Flow type")
        val name = function.simpleName.asString()
        val isState = elementType.resolve().declaration.qualifiedName?.asString() == SocketNames.SOCKET_STATE

        return when {
            isState -> SocketMethod.State(name = name, isStateFlow = isStateFlow)
            isStateFlow -> fail(function, "@Receive may return StateFlow only for SocketState")
            else -> SocketMethod.Receive(
                name = name,
                type = elementType.toTypeName(),
                decodeAs = (elementType.resolve().declaration as? KSClassDeclaration)
                    ?.sealedSerializableAncestor()
                    ?.toClassName(),
            )
        }
    }

    private fun parseSend(function: KSFunctionDeclaration): SocketMethod? {
        val parameter = function.parameters.singleOrNull()
        val returnTypeName = function.returnType?.resolve()
            ?.declaration?.qualifiedName?.asString()
        val isReturnsBoolean = returnTypeName == SocketNames.BOOLEAN
        if (parameter == null || parameter.isVararg || !isReturnsBoolean) {
            return fail(function, "@Send must take exactly one parameter and return Boolean")
        }

        return SocketMethod.Send(
            name = function.simpleName.asString(),
            parameterName = requireNotNull(parameter.name).asString(),
            type = parameter.type.toTypeName(),
        )
    }

    private fun fail(function: KSFunctionDeclaration, message: String): SocketMethod? {
        logger.error("${function.simpleName.asString()}: $message", function)
        return null
    }

    private fun List<SocketMethod>.fakePropertyNames(): List<String> = map { method ->
        if (method is SocketMethod.Send) "${method.name}Sent" else method.name
    } + FAKE_SEND_RESULT

    private fun List<SocketMethod>.fakeAccessorClashes(): List<String> {
        val getters = fakePropertyNames().map(::jvmGetterName).toSet()
        val noArgClashes = filter { it !is SocketMethod.Send && it.name in getters }.map { it.name }
        val setterClashes = filter { it is SocketMethod.Send && it.name == FAKE_SEND_RESULT_SETTER }.map { it.name }
        return noArgClashes + setterClashes
    }

    private fun jvmGetterName(property: String): String = if (property.matches(IS_PREFIXED)) {
        property
    } else {
        "get" + property.replaceFirstChar(Char::uppercaseChar)
    }

    private fun KSClassDeclaration.consultedFiles(): Set<KSFile> = buildSet {
        addAll(withAncestorFiles())
        getAllFunctions().filter { it.isAbstract }.forEach { function ->
            function.returnType?.resolve()?.arguments
                ?.mapNotNull { argument -> argument.type?.resolve()?.declaration as? KSClassDeclaration }
                ?.forEach { type -> addAll(type.withAncestorFiles()) }
        }
    }

    private fun KSClassDeclaration.withAncestorFiles(): List<KSFile> =
        listOfNotNull(containingFile) + getAllSuperTypes().mapNotNull { it.declaration.containingFile }

    private fun KSClassDeclaration.sealedSerializableAncestor(): KSClassDeclaration? = superTypes
        .mapNotNull { it.resolve().declaration as? KSClassDeclaration }
        .firstNotNullOfOrNull { parent ->
            parent.takeIf { Modifier.SEALED in it.modifiers && it.isSerializable() }
                ?: parent.sealedSerializableAncestor()
        }

    private fun KSClassDeclaration.isSerializable(): Boolean = annotations.any { annotation ->
        val declaration = annotation.annotationType.resolve().declaration
        declaration.qualifiedName?.asString() == SocketNames.SERIALIZABLE
    }

    private companion object {
        const val FAKE_SEND_RESULT = "sendResult"
        const val FAKE_SEND_RESULT_SETTER = "setSendResult"
        val IS_PREFIXED = Regex("is[A-Z].*")
    }
}