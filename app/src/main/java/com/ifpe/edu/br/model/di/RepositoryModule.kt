package com.ifpe.edu.br.di

import android.content.Context
import com.ifpe.edu.br.model.repository.AdminRepository
import com.ifpe.edu.br.model.repository.persistence.dao.TokenDao
import com.ifpe.edu.br.model.repository.persistence.manager.SharedPrefManager
import com.ifpe.edu.br.model.repository.remote.api.AdminServerManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAdminRepository(
        @ApplicationContext context: Context,
        adminServerManager: AdminServerManager, // O Hilt puxa isso do NetworkModule
        tokenDao: TokenDao,                     // O Hilt puxa isso do DatabaseModule
        sharedPrefManager: SharedPrefManager    // O Hilt puxa isso do DatabaseModule
    ): AdminRepository {

        // Constrói o repositório com tudo injetado em cascata!
        return AdminRepository(
            context = context,
            adminServerManager = adminServerManager,
            tokenDao = tokenDao,
            prefs = sharedPrefManager
        )
    }
}