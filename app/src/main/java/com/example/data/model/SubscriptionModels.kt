// OfficeZen subscription models & feature tier enforcement logic
package com.example.data.model

enum class PlanTier(val wire: String, val maxEmployees: Int, val maxDocuments: Int) {
  FREE("free", 3, 10),
  STARTER("starter", 10, 100),
  BUSINESS("business", 50, 1000);

  companion object {
    fun fromWire(value: String): PlanTier =
      entries.firstOrNull { it.wire.equals(value, ignoreCase = true) } ?: FREE
  }
}

data class OfficeSubscription(
  val officeId: String = "",
  val planId: String = PlanTier.FREE.wire,
  val status: String = "active", // active, trialing, cancelled, grace_period, expired
  val trialStartAtEpochMs: Long = 0L,
  val trialEndAtEpochMs: Long = 0L,
  val startAtEpochMs: Long = 0L,
  val expiryAtEpochMs: Long = 0L, // Paid period expiry
  val purchaseToken: String = "",
  val productId: String = "",
  val billingPeriod: String = "monthly", // monthly, yearly
  val autoRenewing: Boolean = true,
  val lastVerifiedAtEpochMs: Long = 0L,
  val updatedAtEpochMs: Long = 0L,
) {
  val tier: PlanTier get() = PlanTier.fromWire(planId)

  companion object {
    const val CACHE_VALIDITY_DURATION_MS = 7L * 24L * 60L * 60L * 1000L // 7 Days cache validity
    const val GRACE_PERIOD_DURATION_MS = 3L * 24L * 60L * 60L * 1000L // 3 Days grace period
  }

  fun isTrialActive(nowMs: Long = System.currentTimeMillis()): Boolean =
    trialEndAtEpochMs > 0L && nowMs <= trialEndAtEpochMs

  fun isPaidActive(nowMs: Long = System.currentTimeMillis()): Boolean =
    expiryAtEpochMs > 0L && nowMs <= expiryAtEpochMs

  fun isInGracePeriod(nowMs: Long = System.currentTimeMillis()): Boolean =
    expiryAtEpochMs > 0L && nowMs > expiryAtEpochMs && nowMs <= expiryAtEpochMs + GRACE_PERIOD_DURATION_MS

  fun isClockTampered(nowMs: Long = System.currentTimeMillis()): Boolean {
    // If device wall clock is set backward before last verified timestamp, or if negative time delta occurs
    if (lastVerifiedAtEpochMs > 0L && nowMs < lastVerifiedAtEpochMs) {
      return true
    }
    return false
  }

  fun isCacheValid(nowMs: Long = System.currentTimeMillis()): Boolean {
    if (isClockTampered(nowMs)) return false
    return lastVerifiedAtEpochMs > 0L && (nowMs - lastVerifiedAtEpochMs) <= CACHE_VALIDITY_DURATION_MS
  }

  fun isEffectiveActive(nowMs: Long = System.currentTimeMillis()): Boolean =
    !isClockTampered(nowMs) && (isTrialActive(nowMs) || isPaidActive(nowMs) || isInGracePeriod(nowMs) || tier == PlanTier.FREE)
}

data class PaymentTransaction(
  val id: String = "",
  val officeId: String = "",
  val ownerUid: String = "",
  val productId: String = "",
  val purchaseToken: String = "",
  val amountInr: Double = 0.0,
  val status: String = "verified", // verified, pending, failed, refunded
  val createdAtEpochMs: Long = 0L,
)
