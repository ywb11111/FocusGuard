package com.ywb.focusguard.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库 1 → 2：新增睡眠监测的两张表。
 *
 * 为什么必须手写 Migration：
 * 已安装旧版本的手机上，focus_guard.db 仍是 version 1。新版本把 @Database.version 改成 2 后，
 * Room 打开数据库时会寻找 1→2 的迁移路径；找不到就直接抛 IllegalStateException 崩溃。
 * 另一个"省事"做法 fallbackToDestructiveMigration 会删库重建，用户的专注记录全部丢失，不能用。
 *
 * 写法要点：
 * 1. 只做"新增表"，不碰已有的 focus_sessions 等表，风险最小。
 * 2. SQL 必须和 Entity 生成的结构完全一致（列名、类型、NOT NULL、主键、索引名），
 *    迁移完成后 Room 会校验表结构，任何不一致都会报 "Migration didn't properly handle"。
 *    对照依据：Room 在 build 目录生成的 FocusGuardDatabase_Impl 中的 createAllTables SQL。
 * 3. Kotlin 类型到 SQLite 的映射：Long/Int/Boolean → INTEGER，Float → REAL，String → TEXT；
 *    非空类型追加 NOT NULL，可空类型不加。
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sleep_sessions` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`startTime` INTEGER NOT NULL, " +
                "`endTime` INTEGER, " +
                "`placement` TEXT NOT NULL, " +
                "`audioEnabled` INTEGER NOT NULL, " +
                "`sleepOnsetTime` INTEGER, " +
                "`finalWakeTime` INTEGER, " +
                "`monitoredMillis` INTEGER NOT NULL, " +
                "`totalSleepMillis` INTEGER NOT NULL, " +
                "`sleepLatencyMillis` INTEGER NOT NULL, " +
                "`wakeCount` INTEGER NOT NULL, " +
                "`snoreEventCount` INTEGER NOT NULL, " +
                "`averageNoiseDb` REAL NOT NULL, " +
                "`averageLightLux` REAL NOT NULL, " +
                "`score` INTEGER NOT NULL, " +
                "`note` TEXT)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sleep_epochs` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`sessionId` INTEGER NOT NULL, " +
                "`startTime` INTEGER NOT NULL, " +
                "`movementSeconds` INTEGER NOT NULL, " +
                "`averageNoiseDb` REAL NOT NULL, " +
                "`maxNoiseDb` REAL NOT NULL, " +
                "`soundEventCount` INTEGER NOT NULL, " +
                "`snoreEventCount` INTEGER NOT NULL, " +
                "`averageLightLux` REAL NOT NULL, " +
                "`screenOnSeconds` INTEGER NOT NULL)"
        )
        // 索引名必须是 Room 的默认命名 index_<表名>_<列名>，否则结构校验失败。
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_sleep_epochs_sessionId` ON `sleep_epochs` (`sessionId`)"
        )
    }
}

/**
 * 数据库 2 → 3：分心检测。
 *
 * 1. focus_sessions 新增 distractionTracked 列。
 *    给已有表加列用 ALTER TABLE ADD COLUMN；NOT NULL 列必须带 DEFAULT，否则已有行没有值，SQLite 会拒绝。
 *    Entity 上的 @ColumnInfo(defaultValue = "0") 要与这里的 DEFAULT 0 一致，Room 校验时会比较默认值。
 * 2. 新增 distraction_events 表和 sessionId 索引。
 *
 * 用户可能从 1 直接升级到 3，Room 会依次执行 MIGRATION_1_2、MIGRATION_2_3，所以每个迁移只管相邻两个版本。
 */
val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `focus_sessions` ADD COLUMN `distractionTracked` INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `distraction_events` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`sessionId` INTEGER NOT NULL, " +
                "`packageName` TEXT NOT NULL, " +
                "`appLabel` TEXT NOT NULL, " +
                "`startTime` INTEGER NOT NULL, " +
                "`endTime` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_distraction_events_sessionId` ON `distraction_events` (`sessionId`)"
        )
    }
}
