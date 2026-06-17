package com.ywb.focusguard.di

import android.content.Context
import androidx.room.Room
import com.ywb.focusguard.data.local.database.FocusGuardDatabase
import com.ywb.focusguard.data.repository.EnvironmentRepository
import com.ywb.focusguard.data.repository.EnvironmentRepositoryImpl
import com.ywb.focusguard.data.repository.FocusRepository
import com.ywb.focusguard.data.repository.FocusRepositoryImpl
import com.ywb.focusguard.data.repository.SettingsRepository
import com.ywb.focusguard.data.repository.SettingsRepositoryImpl
import com.ywb.focusguard.domain.analyzer.EnvironmentAnalyzer
import com.ywb.focusguard.domain.analyzer.FocusScoreAnalyzer
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindFocusRepository(repository: FocusRepositoryImpl): FocusRepository

    @Binds
    @Singleton
    abstract fun bindEnvironmentRepository(repository: EnvironmentRepositoryImpl): EnvironmentRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(repository: SettingsRepositoryImpl): SettingsRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FocusGuardDatabase =
        Room.databaseBuilder(
            context,
            FocusGuardDatabase::class.java,
            "focus_guard.db"
        ).build()

    @Provides
    @Singleton
    fun provideEnvironmentAnalyzer(): EnvironmentAnalyzer = EnvironmentAnalyzer()

    @Provides
    @Singleton
    fun provideFocusScoreAnalyzer(): FocusScoreAnalyzer = FocusScoreAnalyzer()
}
