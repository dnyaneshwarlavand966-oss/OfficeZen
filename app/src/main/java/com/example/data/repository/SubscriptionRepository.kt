// OfficeZen feature access manager and subscription repository
package com.example.data.repository

import android.content.Context
import com.example.data.local.OfficeDatabase
import com.example.data.local.SubscriptionDao
import com.example.data.local.toEntity
import com.example.data.model.OfficeSubscription
import com.example.data.model.PaymentTransaction
import com.example.data.model.PlanTier
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SubscriptionRepository(
  private val dao: SubscriptionDao,
) {
  constructor(context: Context) : this(OfficeDatabase.getInstance(context.applicationContext).subscriptionDao())

  fun observeSubscription(officeId: String): Flow<OfficeSubscription> =
    dao.observeSubscription(officeId).map { entity ->
      entity?.toDomain() ?: OfficeSubscription(officeId = officeId, planId = PlanTier.FREE.wire, status = "active")
    }

  suspend fun getSubscription(officeId: String): OfficeSubscription =
    dao.getSubscription(officeId)?.toDomain()
      ?: OfficeSubscription(officeId = officeId, planId = PlanTier.FREE.wire, status = "active")

  suspend fun upsertSubscription(sub: OfficeSubscription) {
    dao.upsertSubscription(sub.toEntity())
  }

  fun observePaymentHistory(officeId: String): Flow<List<PaymentTransaction>> =
    dao.observePaymentHistory(officeId).map { list -> list.map { it.toDomain() } }

  suspend fun recordPayment(transaction: PaymentTransaction) {
    dao.insertPaymentTransaction(transaction.toEntity())
  }

  suspend fun initializeTrialIfEligible(officeId: String, ownerUid: String): OfficeSubscription {
    val existing = dao.getSubscription(officeId)?.toDomain()
    if (existing != null) return existing

    val now = System.currentTimeMillis()
    val trialDurationMs = 7L * 24L * 60L * 60L * 1000L // 7 Days
    val trialSub =
      OfficeSubscription(
        officeId = officeId,
        planId = PlanTier.BUSINESS.wire, // 7 days Business trial
        status = "trialing",
        trialStartAtEpochMs = now,
        trialEndAtEpochMs = now + trialDurationMs,
        startAtEpochMs = now,
        expiryAtEpochMs = now + trialDurationMs,
        lastVerifiedAtEpochMs = now,
        updatedAtEpochMs = now,
      )
    dao.upsertSubscription(trialSub.toEntity())
    return trialSub
  }

  /**
   * Stub for Firebase Cloud Functions backend verification (Phase 4 / Phase 5).
   * Verifies Google Play purchase token and purchase status securely on the server.
   */
  suspend fun verifyPurchaseOnBackend(purchaseToken: String, productId: String): Result<Boolean> {
    // TODO: Implement actual Firebase Functions call in Phase 4/5
    return Result.success(true)
  }

  /**
   * Stub for server-authoritative trial activation via Firebase Cloud Functions (Phase 4).
   */
  suspend fun requestServerAuthoritativeTrial(officeId: String, ownerUid: String): Result<OfficeSubscription> {
    val sub = initializeTrialIfEligible(officeId, ownerUid)
    return Result.success(sub)
  }
}

object FeatureAccessManager {

  fun evaluateTier(sub: OfficeSubscription?, nowMs: Long = System.currentTimeMillis()): PlanTier {
    if (sub == null) return PlanTier.FREE
    // If cache validity expired and unverified (offline > 7 days without re-verification), fallback to FREE
    if (!sub.isCacheValid(nowMs)) {
      return PlanTier.FREE
    }
    // If trial is active, grant Business tier
    if (sub.isTrialActive(nowMs)) return PlanTier.BUSINESS
    // If paid subscription is active, grant tier
    if (sub.isPaidActive(nowMs)) return sub.tier
    // If within grace period (3 days post expiry), grant tier with grace status
    if (sub.isInGracePeriod(nowMs)) return sub.tier
    // Fallback to FREE
    return PlanTier.FREE
  }

  fun canAddEmployee(currentStaffCount: Int, tier: PlanTier): Boolean {
    return currentStaffCount < tier.maxEmployees
  }

  fun canUploadDocument(currentDocCount: Int, tier: PlanTier): Boolean {
    return currentDocCount < tier.maxDocuments
  }

  fun hasPayrollAccess(tier: PlanTier): Boolean =
    tier == PlanTier.STARTER || tier == PlanTier.BUSINESS

  fun hasAdvancedReportsAccess(tier: PlanTier): Boolean =
    tier == PlanTier.BUSINESS

  fun hasMultipleOfficeAccess(tier: PlanTier): Boolean =
    tier == PlanTier.BUSINESS

  fun hasAdvancedEmployeeManagement(tier: PlanTier): Boolean =
    tier == PlanTier.BUSINESS
}
