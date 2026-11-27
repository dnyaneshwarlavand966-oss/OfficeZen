// OfficeZen Room Database singleton
package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities = [
    UserProfileEntity::class,
    OfficeEntity::class,
    MemberEntity::class,
    PayrollProfileEntity::class,
    AttendanceRecordEntity::class,
    LeaveRequestEntity::class,
    SalaryAdjustmentEntity::class,
    TeaLogEntity::class,
    TeaPurchaseEntity::class,
    OfficeDocumentEntity::class,
    AuditLogEntity::class,
    OfficeSubscriptionEntity::class,
    PaymentTransactionEntity::class,
  ],
  version = 3,
  exportSchema = false,
)
abstract class OfficeDatabase : RoomDatabase() {
  abstract fun officeDao(): OfficeDao
  abstract fun subscriptionDao(): SubscriptionDao

  companion object {
    val MIGRATION_1_2 =
      object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            "ALTER TABLE attendance_records ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'pending'"
          )
          db.execSQL(
            "ALTER TABLE attendance_records ADD COLUMN lastSyncAttemptEpochMs INTEGER NOT NULL DEFAULT 0"
          )
          db.execSQL(
            "ALTER TABLE attendance_records ADD COLUMN syncError TEXT NOT NULL DEFAULT ''"
          )
        }
      }

    val MIGRATION_2_3 =
      object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS office_subscriptions (
              officeId TEXT NOT NULL PRIMARY KEY,
              planId TEXT NOT NULL,
              status TEXT NOT NULL,
              trialStartAtEpochMs INTEGER NOT NULL,
              trialEndAtEpochMs INTEGER NOT NULL,
              startAtEpochMs INTEGER NOT NULL,
              expiryAtEpochMs INTEGER NOT NULL,
              purchaseToken TEXT NOT NULL,
              productId TEXT NOT NULL,
              billingPeriod TEXT NOT NULL,
              autoRenewing INTEGER NOT NULL,
              lastVerifiedAtEpochMs INTEGER NOT NULL,
              updatedAtEpochMs INTEGER NOT NULL
            )
            """
          )
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS payment_transactions (
              id TEXT NOT NULL PRIMARY KEY,
              officeId TEXT NOT NULL,
              ownerUid TEXT NOT NULL,
              productId TEXT NOT NULL,
              purchaseToken TEXT NOT NULL,
              amountInr REAL NOT NULL,
              status TEXT NOT NULL,
              createdAtEpochMs INTEGER NOT NULL
            )
            """
          )
        }
      }

    @Volatile private var instance: OfficeDatabase? = null

    fun getInstance(context: Context): OfficeDatabase =
      instance
        ?: synchronized(this) {
          instance
            ?: Room.databaseBuilder(
                context.applicationContext,
                OfficeDatabase::class.java,
                "officezen_local.db",
              )
              .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
              .fallbackToDestructiveMigration(dropAllTables = false)
              .build()
              .also { instance = it }
        }
  }
}
