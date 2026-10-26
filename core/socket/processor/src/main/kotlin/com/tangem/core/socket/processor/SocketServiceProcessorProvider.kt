package com.tangem.core.socket.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class SocketServiceProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor = SocketServiceProcessor(
        codeGenerator = environment.codeGenerator,
        logger = environment.logger,
    )
}