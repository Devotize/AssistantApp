package com.sychev.assistantapp.di

import com.sychev.assistantapp.network.ClothesService
import com.sychev.assistantapp.repository.ClothesRepository
import com.sychev.assistantapp.repository.ClothesRepository_Impl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Singleton
    @Provides
    fun provideClothesRepository(
        clothesService: ClothesService
    ): ClothesRepository {
        return ClothesRepository_Impl(
            service = clothesService
        )
    }
}