package com.ifpe.edu.br.model.di

import android.content.Context
import com.ifpe.edu.br.model.repository.persistence.AirPowerDatabase
import com.ifpe.edu.br.model.repository.persistence.dao.TokenDao
import com.ifpe.edu.br.model.repository.persistence.manager.SharedPrefManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSharedPrefManager(@ApplicationContext context: Context): SharedPrefManager {
        return SharedPrefManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AirPowerDatabase {
        return AirPowerDatabase.getDataBaseInstance(context)
    }

    @Provides
    fun provideTokenDao(database: AirPowerDatabase): TokenDao {
        return database.tokenDaoInstance
    }
}