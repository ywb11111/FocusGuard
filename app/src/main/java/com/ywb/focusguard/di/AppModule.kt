package com.ywb.focusguard.di

import android.content.Context
import androidx.room.Room
import com.ywb.focusguard.data.local.dao.FocusSessionDao
import com.ywb.focusguard.data.local.dao.SampleDao
import com.ywb.focusguard.data.local.database.FocusGuardDatabase
import com.ywb.focusguard.data.repository.DataStoreSettingsRepository
import com.ywb.focusguard.data.repository.EnvironmentRepository
import com.ywb.focusguard.data.repository.EnvironmentRepositoryImpl
import com.ywb.focusguard.data.repository.FocusRepository
import com.ywb.focusguard.data.repository.FocusRepositoryImpl
import com.ywb.focusguard.data.repository.SettingsRepository
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
    /** 从单例数据库获取采样 DAO，用于保存和查询光照/噪声/移动样本。 */
    fun provideSampleDao(database: FocusGuardDatabase): SampleDao =
        database.sampleDao()

    @Provides
    @Singleton
    /** 环境分析器无可变状态，可作为全局单例复用。 */
    fun provideEnvironmentAnalyzer(): EnvironmentAnalyzer = EnvironmentAnalyzer()

    @Provides
    @Singleton
    /** 评分分析器无可变状态，可作为全局单例复用。 */
    fun provideFocusScoreAnalyzer(): FocusScoreAnalyzer = FocusScoreAnalyzer()

    /**
     * 提供 DataStore 实现的 SettingsRepository。
     *
     * 为什么用 @Provides 而不是 @Binds？
     * DataStoreSettingsRepository 构造函数需要 Context 参数，
     * @Binds 只能用于简单的构造函数注入场景。
     * @Provides 可以在方法内部完成复杂的依赖组装。
     */
    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository =
        DataStoreSettingsRepository(context)
}
