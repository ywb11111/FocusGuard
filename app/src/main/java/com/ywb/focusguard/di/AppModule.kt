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

/** 将 Repository 接口绑定到默认实现，供 ViewModel 按接口注入。 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    // @Binds 告诉 Hilt：当某处需要 FocusRepository 接口时，实际提供 FocusRepositoryImpl。
    @Binds
    @Singleton
    abstract fun bindFocusRepository(repository: FocusRepositoryImpl): FocusRepository

    @Binds
    @Singleton
    /** 环境 Repository 在全局共享，避免每个页面重复创建协调对象。 */
    abstract fun bindEnvironmentRepository(repository: EnvironmentRepositoryImpl): EnvironmentRepository

    @Binds
    @Singleton
    /** 当前绑定内存设置实现，后续可无感替换为 DataStore 实现。 */
    abstract fun bindSettingsRepository(repository: SettingsRepositoryImpl): SettingsRepository
}

/** 提供无法直接使用构造函数注入的 Room 和无状态 Analyzer 对象。 */
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
    /** 从单例数据库获取会话 DAO，确保 Repository 使用同一个数据库连接。 */
    fun provideFocusSessionDao(database: FocusGuardDatabase): FocusSessionDao =
        database.focusSessionDao()

    @Provides
    @Singleton
    /** 环境分析器无可变状态，可作为全局单例复用。 */
    fun provideEnvironmentAnalyzer(): EnvironmentAnalyzer = EnvironmentAnalyzer()

    @Provides
    @Singleton
    /** 评分分析器无可变状态，可作为全局单例复用。 */
    fun provideFocusScoreAnalyzer(): FocusScoreAnalyzer = FocusScoreAnalyzer()
}
