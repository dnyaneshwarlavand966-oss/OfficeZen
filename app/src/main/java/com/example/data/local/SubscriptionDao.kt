// OfficeZen subscription DAO operations
package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {

  @Query("SELECT * FROM office_subscriptions WHERE officeId = :officeId LIMIT 1")
  fun observeSubscription(officeId: String): Flow<OfficeSubscriptionEntity?>

  @Query("SELECT * FROM office_subscriptions WHERE officeId = :officeId LIMIT 1")
  suspend fun getSubscription(officeId: String): OfficeSubscriptionEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSubscription(subscription: OfficeSubscriptionEntity)

  @Query("SELECT * FROM payment_transactions WHERE officeId = :officeId ORDER BY createdAtEpochMs DESC")
  fun observePaymentHistory(officeId: String): Flow<List<PaymentTransactionEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPaymentTransaction(transaction: PaymentTransactionEntity)
}
