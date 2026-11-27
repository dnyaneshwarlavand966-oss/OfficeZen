// OfficeZen Room subscription entities and migration 2 -> 3
package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.OfficeSubscription
import com.example.data.model.PaymentTransaction

@Entity(tableName = "office_subscriptions")
data class OfficeSubscriptionEntity(
  @PrimaryKey val officeId: String,
  val planId: String,
  val status: String,
  val trialStartAtEpochMs: Long,
  val trialEndAtEpochMs: Long,
  val startAtEpochMs: Long,
  val expiryAtEpochMs: Long,
  val purchaseToken: String,
  val productId: String,
  val billingPeriod: String,
  val autoRenewing: Boolean,
  val lastVerifiedAtEpochMs: Long,
  val updatedAtEpochMs: Long,
) {
  fun toDomain(): OfficeSubscription =
    OfficeSubscription(
      officeId = officeId,
      planId = planId,
      status = status,
      trialStartAtEpochMs = trialStartAtEpochMs,
      trialEndAtEpochMs = trialEndAtEpochMs,
      startAtEpochMs = startAtEpochMs,
      expiryAtEpochMs = expiryAtEpochMs,
      purchaseToken = purchaseToken,
      productId = productId,
      billingPeriod = billingPeriod,
      autoRenewing = autoRenewing,
      lastVerifiedAtEpochMs = lastVerifiedAtEpochMs,
      updatedAtEpochMs = updatedAtEpochMs,
    )
}

fun OfficeSubscription.toEntity(): OfficeSubscriptionEntity =
  OfficeSubscriptionEntity(
    officeId = officeId,
    planId = planId,
    status = status,
    trialStartAtEpochMs = trialStartAtEpochMs,
    trialEndAtEpochMs = trialEndAtEpochMs,
    startAtEpochMs = startAtEpochMs,
    expiryAtEpochMs = expiryAtEpochMs,
    purchaseToken = purchaseToken,
    productId = productId,
    billingPeriod = billingPeriod,
    autoRenewing = autoRenewing,
    lastVerifiedAtEpochMs = lastVerifiedAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
  )

@Entity(tableName = "payment_transactions")
data class PaymentTransactionEntity(
  @PrimaryKey val id: String,
  val officeId: String,
  val ownerUid: String,
  val productId: String,
  val purchaseToken: String,
  val amountInr: Double,
  val status: String,
  val createdAtEpochMs: Long,
) {
  fun toDomain(): PaymentTransaction =
    PaymentTransaction(
      id = id,
      officeId = officeId,
      ownerUid = ownerUid,
      productId = productId,
      purchaseToken = purchaseToken,
      amountInr = amountInr,
      status = status,
      createdAtEpochMs = createdAtEpochMs,
    )
}

fun PaymentTransaction.toEntity(): PaymentTransactionEntity =
  PaymentTransactionEntity(
    id = id.ifBlank { "tx_${System.currentTimeMillis()}" },
    officeId = officeId,
    ownerUid = ownerUid,
    productId = productId,
    purchaseToken = purchaseToken,
    amountInr = amountInr,
    status = status,
    createdAtEpochMs = createdAtEpochMs,
  )
