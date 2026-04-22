package com.ifpe.edu.br.model.di

import android.content.Context
import com.ifpe.edu.br.model.repository.remote.api.AdminServerManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAdminServerManager(@ApplicationContext context: Context): AdminServerManager {
        return AdminServerManager(context)
    }
}