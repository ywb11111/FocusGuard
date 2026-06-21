package com.ywb.focusguard.di

import android.content.Context
import androidx.room.Room
import com.ywb.focusguard.data.local.dao.FocusSessionDao
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
    // @Binds 告诉 Hilt：当某处需要 FocusRepository 接口时，实际提供 FocusRepositoryImpl。
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
    // 数据库是全局单例。多个 Repository/DAO 共用同一个 RoomDatabase 实例，避免重复打开数据库。
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
    fun provideFocusSessionDao(database: FocusGuardDatabase): FocusSessionDao =
        database.focusSessionDao()

    @Provides
    @Singleton
    fun provideEnvironmentAnalyzer(): EnvironmentAnalyzer = EnvironmentAnalyzer()

    @Provides
    @Singleton
    fun provideFocusScoreAnalyzer(): FocusScoreAnalyzer = FocusScoreAnalyzer()
}
