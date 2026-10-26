package com.tangem.data.mcp.di

import com.tangem.data.mcp.repository.DefaultMcpRepository
import com.tangem.data.mcp.request.DefaultMcpRequestService
import com.tangem.domain.mcp.McpRequestService
import com.tangem.domain.mcp.repository.McpRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface McpDataModule {

    @Binds
    @Singleton
    fun bindMcpRepository(impl: DefaultMcpRepository): McpRepository

    @Binds
    @Singleton
    fun bindMcpRequestService(impl: DefaultMcpRequestService): McpRequestService
}