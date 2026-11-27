// OfficeZen Subscription & Plans Screen (Material 3, Deep Emerald / Warm Alabaster palette)
package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.OfficeSubscription
import com.example.data.model.PaymentTransaction
import com.example.data.model.PlanTier
import com.example.data.repository.FeatureAccessManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SubscriptionScreen(
  subscription: OfficeSubscription?,
  paymentHistory: List<PaymentTransaction>,
  onUpgradeSelected: (PlanTier, Boolean) -> Unit, // tier, isYearly
  onRestorePurchases: () -> Unit,
  onOpenTermsAndPrivacy: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isYearly by remember { mutableStateOf(false) }
  val now = System.currentTimeMillis()
  val activeTier = subscription?.let { FeatureAccessManager.evaluateTier(it, now) } ?: PlanTier.FREE
  val isTrial = subscription?.isTrialActive(now) == true
  val trialDaysLeft =
    if (isTrial && subscription != null) {
      ((subscription.trialEndAtEpochMs - now) / (1000 * 60 * 60 * 24)).coerceAtLeast(0L)
    } else {
      0L
    }

  LazyColumn(
    modifier = modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Header Banner
    item {
      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth().testTag("subscription_header_card"),
      ) {
        Column(
          modifier = Modifier.padding(20.dp).fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    Icons.Rounded.WorkspacePremium,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                  )
                }
              }
              Column {
                Text(
                  text = "OfficeZen Subscriptions",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                  text = "Current Plan: ${activeTier.name}${if (isTrial) " (7-Day Trial)" else ""}",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
              }
            }
          }

          if (isTrial) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
              Text(
                text = "Free Trial Active: $trialDaysLeft days remaining before auto-switch to Free plan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              )
            }
          }
        }
      }
    }

    // Billing Frequency Toggle (Monthly / Yearly)
    item {
      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Monthly Billing", fontWeight = FontWeight.SemiBold, color = if (!isYearly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
          checked = isYearly,
          onCheckedChange = { isYearly = it },
          modifier = Modifier.testTag("billing_cycle_switch"),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text("Yearly Billing (Save 15%)", fontWeight = FontWeight.SemiBold, color = if (isYearly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }

    // Plan Comparison Cards
    item {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PlanCard(
          tier = PlanTier.FREE,
          priceMonthly = "₹0",
          priceYearly = "₹0",
          isYearly = isYearly,
          isCurrent = activeTier == PlanTier.FREE && !isTrial,
          features = listOf("Up to 3 employees", "Basic attendance", "Basic office dashboard", "10 documents limit"),
          onSelect = { onUpgradeSelected(PlanTier.FREE, isYearly) },
        )

        PlanCard(
          tier = PlanTier.STARTER,
          priceMonthly = "₹99",
          priceYearly = "₹999",
          isYearly = isYearly,
          isCurrent = activeTier == PlanTier.STARTER,
          features = listOf("Up to 10 employees", "Inward & Outward Register", "Tea & Coffee management", "Basic payroll & PDF reports", "100 documents limit"),
          onSelect = { onUpgradeSelected(PlanTier.STARTER, isYearly) },
        )

        PlanCard(
          tier = PlanTier.BUSINESS,
          priceMonthly = "₹299",
          priceYearly = "₹2,999",
          isYearly = isYearly,
          isCurrent = activeTier == PlanTier.BUSINESS || isTrial,
          isPopular = true,
          features = listOf("Up to 50 employees", "Complete payroll management", "Advanced PDF reports", "Multiple office management", "1,000 documents limit"),
          onSelect = { onUpgradeSelected(PlanTier.BUSINESS, isYearly) },
        )
      }
    }

    // Payment History Section
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Payment History",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )
          if (paymentHistory.isEmpty()) {
            Text(
              text = "No previous payment transactions recorded locally.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    if (paymentHistory.isNotEmpty()) {
      items(paymentHistory) { tx ->
        Card(
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(tx.productId, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
              val dateStr = remember(tx.createdAtEpochMs) {
                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(tx.createdAtEpochMs))
              }
              Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("₹${tx.amountInr}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
          }
        }
      }
    }

    // Actions & Footer
    item {
      Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        TextButton(onClick = onRestorePurchases, modifier = Modifier.testTag("restore_purchases_button")) {
          Text("Restore Purchases")
        }
        TextButton(onClick = onOpenTermsAndPrivacy, modifier = Modifier.testTag("terms_privacy_button")) {
          Text("Terms of Service & Privacy Policy")
        }
        Text(
          text = "Google Play Billing Secure Verification via Firebase Backend",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}

@Composable
private fun PlanCard(
  tier: PlanTier,
  priceMonthly: String,
  priceYearly: String,
  isYearly: Boolean,
  isCurrent: Boolean,
  isPopular: Boolean = false,
  features: List<String>,
  onSelect: () -> Unit,
) {
  val price = if (isYearly) priceYearly else priceMonthly
  val period = if (isYearly) "/year" else "/month"

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isPopular) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
    ),
    border = if (isPopular) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    modifier = Modifier.fillMaxWidth().testTag("plan_card_${tier.wire}"),
  ) {
    Column(
      modifier = Modifier.padding(20.dp).fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = when (tier) {
              PlanTier.FREE -> "Free Plan"
              PlanTier.STARTER -> "Starter Plan"
              PlanTier.BUSINESS -> "Business Plan"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          if (isPopular) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.primary,
            ) {
              Text(
                text = "MOST POPULAR",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
        }
        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = price,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
          if (tier != PlanTier.FREE) {
            Text(
              text = period,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      HorizontalDivider()

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        features.forEach { feature ->
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              Icons.Rounded.CheckCircle,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp),
            )
            Text(feature, style = MaterialTheme.typography.bodySmall)
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      Button(
        onClick = onSelect,
        enabled = !isCurrent,
        colors = ButtonDefaults.buttonColors(
          containerColor = if (isCurrent) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("select_plan_${tier.wire}"),
      ) {
        Text(if (isCurrent) "Current Plan" else "Select ${tier.name}")
      }
    }
  }
}
