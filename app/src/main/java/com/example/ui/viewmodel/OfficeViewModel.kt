// OfficeZen main ViewModel state & actions
package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AttendanceRecord
import com.example.data.model.AuditLog
import com.example.data.model.LeaveRequest
import com.example.data.model.Member
import com.example.data.model.MemberRole
import com.example.data.model.Office
import com.example.data.model.OfficeDocument
import com.example.data.model.PayrollProfile
import com.example.data.model.SalaryAdjustment
import com.example.data.model.TeaLog
import com.example.data.model.TeaPurchase
import com.example.data.model.UserProfile
import com.example.data.prefs.AppPreferences
import com.example.data.repository.OfficeRepository
import com.example.domain.MonthEndSummary
import com.example.domain.PayrollCalculator
import com.google.firebase.firestore.FirebaseFirestoreException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

@OptIn(ExperimentalCoroutinesApi::class)
class OfficeViewModel(
  private val repository: OfficeRepository,
  private val preferences: AppPreferences,
  val currentUserId: String,
  val userDisplayName: String,
  val userEmail: String,
) : ViewModel() {

  private val _selectedMonthKey = MutableStateFlow(currentMonthKey())
  val selectedMonthKey: StateFlow<String> = _selectedMonthKey.asStateFlow()

  private val _bannerMessage = MutableStateFlow<String?>(null)
  val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  val languageCode: StateFlow<String> =
    preferences.languageCodeFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), "en")

  val remindersEnabled: StateFlow<Boolean> =
    preferences.remindersEnabledFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), true)

  val userProfileState: StateFlow<UiState<UserProfile?>> =
    repository
      .observeUserProfile(currentUserId)
      .toUiState("Failed to load user profile")
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  private val activeOfficeIdFlow: Flow<String> =
    userProfileState.map { state ->
      (state as? UiState.Success)?.data?.activeOfficeId ?: ""
    }

  val officeState: StateFlow<Office?> =
    activeOfficeIdFlow
      .flatMapLatest { officeId ->
        if (officeId.isBlank()) flowOf(null)
        else repository.observeOffice(officeId).catch { emit(null) }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

  val myMemberState: StateFlow<Member?> =
    activeOfficeIdFlow
      .flatMapLatest { officeId ->
        if (officeId.isBlank()) flowOf(null)
        else repository.observeMyMember(officeId, currentUserId).catch { emit(null) }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

  private val isManagerOrOwnerFlow: Flow<Boolean> =
    myMemberState.map { member ->
      val role = member?.roleEnum
      role == MemberRole.OWNER || role == MemberRole.MANAGER
    }

  val membersState: StateFlow<List<Member>> =
    activeOfficeIdFlow
      .flatMapLatest { officeId ->
        if (officeId.isBlank()) flowOf(emptyList())
        else repository.observeMembers(officeId).catch { emit(emptyList()) }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val payrollProfilesState: StateFlow<List<PayrollProfile>> =
    combine(activeOfficeIdFlow, isManagerOrOwnerFlow) { officeId, mgr -> officeId to mgr }
      .flatMapLatest { (officeId, mgr) ->
        if (officeId.isBlank()) flowOf(emptyList())
        else repository.observePayrollProfiles(officeId, mgr).catch { emit(emptyList()) }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val attendanceState: StateFlow<List<AttendanceRecord>> =
    combine(activeOfficeIdFlow, isManagerOrOwnerFlow) { officeId, mgr -> officeId to mgr }
      .flatMapLatest { (officeId, mgr) ->
        if (officeId.isBlank()) flowOf(emptyList())
        else repository.observeAttendance(officeId, mgr).catch { emit(emptyList()) }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val leavesState: StateFlow<List<LeaveRequest>> =
    combine(activeOfficeIdFlow, isManagerOrOwnerFlow) { officeId, mgr -> officeId to mgr }
      .flatMapLatest { (officeId, mgr) ->
        if (officeId.isBlank()) flowOf(emptyList())
        else repository.observeLeaves(officeId, mgr).catch { emit(emptyList()) }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val salaryAdjustmentsState: StateFlow<List<SalaryAdjustment>> =
    combine(activeOfficeIdFlow, isManagerOrOwnerFlow) { officeId, mgr -> officeId to mgr }
      .flatMapLatest { (officeId, mgr) ->
        if (officeId.isBlank()) flowOf(emptyList())
        else repository.observeSalaryAdjustments(officeId, mgr).catch { emit(emptyList()) }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val teaLogsState: StateFlow<List<TeaLog>> =
    activeOfficeIdFlow
      .flatMapLatest { officeId ->
        if (officeId.isBlank()) flowOf(emptyList())
        else repository.observeTeaLogs(officeId).catch { emit(emptyList()) }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val teaPurchasesState: StateFlow<List<TeaPurchase>> =
    activeOfficeIdFlow
      .flatMapLatest { officeId ->
        if (officeId.isBlank()) flowOf(emptyList())
        else repository.observeTeaPurchases(officeId).catch { emit(emptyList()) }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val documentsState: StateFlow<List<OfficeDocument>> =
    activeOfficeIdFlow
      .flatMapLatest { officeId ->
        if (officeId.isBlank()) flowOf(emptyList())
        else repository.observeDocuments(officeId).catch { emit(emptyList()) }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val auditLogsState: StateFlow<List<AuditLog>> =
    combine(activeOfficeIdFlow, isManagerOrOwnerFlow) { officeId, mgr -> officeId to mgr }
      .flatMapLatest { (officeId, mgr) ->
        if (officeId.isBlank() || !mgr) flowOf(emptyList())
        else repository.observeAuditLogs(officeId).catch { emit(emptyList()) }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val monthEndSummaryState: StateFlow<MonthEndSummary?> =
    combine(
      officeState,
      membersState,
      payrollProfilesState,
      selectedMonthKey,
      attendanceState,
      leavesState,
      salaryAdjustmentsState,
      teaLogsState,
      teaPurchasesState,
    ) { values ->
      val office = values[0] as Office? ?: return@combine null
      @Suppress("UNCHECKED_CAST")
      val members = values[1] as List<Member>
      @Suppress("UNCHECKED_CAST")
      val profiles = (values[2] as List<PayrollProfile>).associateBy { it.userId }
      val monthKey = values[3] as String
      @Suppress("UNCHECKED_CAST")
      val attendance = values[4] as List<AttendanceRecord>
      @Suppress("UNCHECKED_CAST")
      val leaves = values[5] as List<LeaveRequest>
      @Suppress("UNCHECKED_CAST")
      val adjustments = values[6] as List<SalaryAdjustment>
      @Suppress("UNCHECKED_CAST")
      val teaLogs = values[7] as List<TeaLog>
      @Suppress("UNCHECKED_CAST")
      val teaPurchases = values[8] as List<TeaPurchase>
      PayrollCalculator.calculateMonthEndSummary(
        office = office,
        members = members,
        profiles = profiles,
        monthKey = monthKey,
        attendance = attendance,
        leaves = leaves,
        adjustments = adjustments,
        teaLogs = teaLogs,
        teaPurchases = teaPurchases,
      )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

  init {
    viewModelScope.launch {
      repository.ensureUserProfile(
        displayName = userDisplayName,
        email = userEmail,
        preferredLanguage = languageCode.value,
      )
    }
  }

  fun clearBanner() {
    _bannerMessage.value = null
  }

  fun showMessage(msg: String) {
    _bannerMessage.value = msg
  }

  fun setMonthKey(monthKey: String) {
    _selectedMonthKey.value = monthKey
  }

  fun setLanguage(code: String) {
    viewModelScope.launch {
      preferences.setLanguageCode(code)
      repository.updatePreferredLanguage(code)
    }
  }

  fun setRemindersEnabled(enabled: Boolean) {
    viewModelScope.launch { preferences.setRemindersEnabled(enabled) }
  }

  fun createOffice(
    officeName: String,
    ownerPhone: String,
    officeLat: Double,
    officeLng: Double,
    geofenceRadiusMeters: Long,
    shiftStartMinutes: Long,
    shiftEndMinutes: Long,
    lateGraceMinutes: Long,
    workingDaysPerMonth: Long,
  ) {
    launchBusy {
      repository
        .createOffice(
          officeName = officeName,
          ownerDisplayName = userDisplayName.ifBlank { userEmail.substringBefore("@") },
          ownerEmail = userEmail,
          ownerPhone = ownerPhone,
          officeLat = officeLat,
          officeLng = officeLng,
          geofenceRadiusMeters = geofenceRadiusMeters,
          shiftStartMinutes = shiftStartMinutes,
          shiftEndMinutes = shiftEndMinutes,
          lateGraceMinutes = lateGraceMinutes,
          workingDaysPerMonth = workingDaysPerMonth,
        )
        .onSuccess { _bannerMessage.value = "Office created! Invite code: ${it.inviteCode}" }
        .onFailure { _bannerMessage.value = it.localizedMessage ?: "Failed to create office" }
    }
  }

  fun joinOffice(inviteCode: String, designation: String, phone: String) {
    launchBusy {
      repository
        .joinOfficeByInviteCode(
          rawCode = inviteCode,
          displayName = userDisplayName.ifBlank { userEmail.substringBefore("@") },
          email = userEmail,
          phone = phone,
          designation = designation,
        )
        .onSuccess { _bannerMessage.value = "Joined office successfully!" }
        .onFailure { _bannerMessage.value = it.localizedMessage ?: "Could not join office" }
    }
  }

  fun switchWorkspace() {
    launchBusy {
      repository.leaveActiveOfficeWorkspace()
    }
  }

  fun updateOfficeSettings(updated: Office) {
    val actor = myMemberState.value ?: return
    launchBusy {
      repository
        .updateOfficeSettings(updated, actor.displayName, actor.roleEnum)
        .onSuccess { _bannerMessage.value = "Office settings saved" }
        .onFailure { _bannerMessage.value = it.localizedMessage ?: "Failed to save settings" }
    }
  }

  fun updateMemberRoleAndSalary(
    member: Member,
    newRole: MemberRole,
    newDesignation: String,
    active: Boolean,
    monthlySalaryInr: Double,
    overtimeHourlyRateInr: Double,
    phone: String,
    email: String,
  ) {
    val office = officeState.value ?: return
    val actor = myMemberState.value ?: return
    launchBusy {
      repository
        .updateMemberRoleAndSalary(
          officeId = office.id,
          member = member,
          newRole = newRole,
          newDesignation = newDesignation,
          active = active,
          monthlySalaryInr = monthlySalaryInr,
          overtimeHourlyRateInr = overtimeHourlyRateInr,
          phone = phone,
          email = email,
          actorName = actor.displayName,
          actorRole = actor.roleEnum,
        )
        .onSuccess { _bannerMessage.value = "Updated ${member.displayName}" }
        .onFailure { _bannerMessage.value = it.localizedMessage ?: "Update failed" }
    }
  }

  fun checkIn(
    lat: Double,
    lng: Double,
    distanceMeters: Double,
    isOutOfRange: Boolean,
    selfieBase64: String,
    note: String,
  ) {
    val office = officeState.value ?: return
    val actor = myMemberState.value ?: return
    val nowMs = System.currentTimeMillis()
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = nowMs }
    val currentMinutes = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60L + cal.get(java.util.Calendar.MINUTE)
    val lateCutoff = office.shiftStartMinutes + office.lateGraceMinutes
    val lateMins = (currentMinutes - office.shiftStartMinutes).coerceAtLeast(0L)
    val status =
      when {
        currentMinutes > office.shiftStartMinutes + 180L -> "half_day"
        currentMinutes > lateCutoff -> "late"
        else -> "on_time"
      }

    launchBusy {
      repository
        .checkIn(
          officeId = office.id,
          record =
            AttendanceRecord(
              userId = currentUserId,
              officeId = office.id,
              userName = actor.displayName,
              dateKey = currentDateKey(),
              checkInEpochMs = nowMs,
              checkInLat = lat,
              checkInLng = lng,
              distanceMeters = distanceMeters,
              isOutOfRange = isOutOfRange,
              isMockLocation = false,
              status = status,
              lateMinutes = if (status == "on_time") 0L else lateMins,
              selfieBase64 = selfieBase64,
              note = note,
            ),
          actorRole = actor.roleEnum,
        )
        .onSuccess { _bannerMessage.value = "Checked in ($status)" }
        .onFailure { _bannerMessage.value = it.localizedMessage ?: "Check-in failed" }
    }
  }

  fun checkOut(todayRecord: AttendanceRecord, note: String) {
    val office = officeState.value ?: return
    val actor = myMemberState.value ?: return
    val nowMs = System.currentTimeMillis()
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = nowMs }
    val currentMinutes = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60L + cal.get(java.util.Calendar.MINUTE)
    val overtimeMins = (currentMinutes - office.shiftEndMinutes).coerceAtLeast(0L)
    launchBusy {
      repository
        .checkOut(
          officeId = office.id,
          record = todayRecord,
          checkOutEpochMs = nowMs,
          overtimeMinutes = overtimeMins,
          note = note,
          actorName = actor.displayName,
          actorRole = actor.roleEnum,
        )
        .onSuccess { _bannerMessage.value = "Checked out! Overtime: ${overtimeMins}m" }
        .onFailure { _bannerMessage.value = it.localizedMessage ?: "Check-out failed" }
    }
  }

  fun submitLeave(
    leaveType: String,
    startDateKey: String,
    endDateKey: String,
    daysCount: Double,
    reason: String,
  ) {
    val office = officeState.value ?: return
    val actor = myMemberState.value ?: return
    launchBusy {
      repository
        .submitLeave(
          officeId = office.id,
          leave =
            LeaveRequest(
              userId = currentUserId,
              officeId = office.id,
              userName = actor.displayName,
              leaveType = leaveType,
              startDateKey = startDateKey,
              endDateKey = endDateKey,
              daysCount = daysCount,
              reason = reason,
            ),
          actorRole = actor.roleEnum,
        )
        .onSuccess { _bannerMessage.value = "Leave request submitted" }
        .onFailure { _bannerMessage.value = it.localizedMessage ?: "Leave request failed" }
    }
  }

  fun reviewLeave(leave: LeaveRequest, approved: Boolean, reviewNote: String) {
    val office = officeState.value ?: return
    val actor = myMemberState.value ?: return
    launchBusy {
      repository
        .reviewLeave(office.id, leave, approved, reviewNote, actor.displayName, actor.roleEnum)
        .onSuccess {
          _bannerMessage.value = if (approved) "Leave approved" else "Leave rejected"
        }
        .onFailure { _bannerMessage.value = it.localizedMessage ?: "Action failed" }
    }
  }

  fun addSalaryAdjustment(
    targetMember: Member,
    type: String,
    amountInr: Double,
    note: String,
  ) {
    val office = officeState.value ?: return
    val actor = myMemberState.value ?: return
    launchBusy {
      repository
        .addSalaryAdjustment(
          officeId = office.id,
          targetUserId = targetMember.userId,
          targetUserName = targetMember.displayName,
          monthKey = selectedMonthKey.value,
          type = type,
          amountInr = amountInr,
          note = note,
          actorName = actor.displayName,
          actorRole = actor.roleEnum,
        )
        .onSuccess { _bannerMessage.value = "Recorded $type for ${targetMember.displayName}" }
        .onFailure { _bannerMessage.value = it.localizedMessage ?: "Failed to save adjustment" }
    }
  }

  fun logOneTapTeaOrCoffee(item: String) {
    val office = officeState.value ?: return
    val actor = myMemberState.value ?: return
    val unitCost = if (item == "coffee") office.coffeeDefaultPriceInr else office.teaDefaultPriceInr
    viewModelScope.launch {
      repository
        .logTeaOrCoffee(
          officeId = office.id,
          userName = actor.displayName,
          item = item,
          quantity = 1L,
          unitCostInr = unitCost,
          dateKey = currentDateKey(),
          monthKey = currentMonthKey(),
          actorRole = actor.roleEnum,
        )
        .onSuccess { _bannerMessage.value = "Logged 1 ${item.replaceFirstChar { it.uppercase() }}!" }
        .onFailure { _bannerMessage.value = it.localizedMessage ?: "Failed to log $item" }
    }
  }

  fun deleteTeaLog(logId: String) {
    val office = officeState.value ?: return
    viewModelScope.launch {
      repository.deleteTeaLog(office.id, logId)
    }
  }

  fun addTeaPurchase(category: String, quantityLabel: String, amountInr: Double, note: String) {
    val office = officeState.value ?: return
    val actor = myMemberState.value ?: return
    launchBusy {
      repository
        .addTeaPurchase(
          officeId = office.id,
          addedByName = actor.displayName,
          category = category,
          quantityLabel = quantityLabel,
          amountInr = amountInr,
          dateKey = currentDateKey(),
          monthKey = currentMonthKey(),
          note = note,
          actorRole = actor.roleEnum,
        )
        .onSuccess { _bannerMessage.value = "Pantry purchase recorded" }
        .onFailure { _bannerMessage.value = it.localizedMessage ?: "Failed to record purchase" }
    }
  }

  fun addDocument(
    direction: String,
    partyName: String,
    subject: String,
    status: String,
    scanBase64: String,
    note: String,
  ) {
    val office = officeState.value ?: return
    val actor = myMemberState.value ?: return
    val year = currentDateKey().take(4)
    val prefix = if (direction == "inward") "IN" else "OUT"
    val nextNum = documentsState.value.count { it.direction == direction } + 1
    val serial = "$prefix-$year-${nextNum.toString().padStart(3, '0')}"

    launchBusy {
      repository
        .addDocument(
          officeId = office.id,
          document =
            OfficeDocument(
              officeId = office.id,
              createdBy = currentUserId,
              createdByName = actor.displayName,
              serialNo = serial,
              direction = direction,
              dateKey = currentDateKey(),
              partyName = partyName,
              subject = subject,
              status = status,
              scanBase64 = scanBase64,
              note = note,
            ),
          actorRole = actor.roleEnum,
        )
        .onSuccess { _bannerMessage.value = "Added $serial to register" }
        .onFailure { _bannerMessage.value = it.localizedMessage ?: "Failed to add document" }
    }
  }

  fun updateDocumentStatus(doc: OfficeDocument, newStatus: String) {
    val office = officeState.value ?: return
    val actor = myMemberState.value ?: return
    viewModelScope.launch {
      repository
        .updateDocumentStatus(office.id, doc, newStatus, actor.displayName, actor.roleEnum)
        .onSuccess { _bannerMessage.value = "${doc.serialNo} marked $newStatus" }
        .onFailure { _bannerMessage.value = it.localizedMessage ?: "Update failed" }
    }
  }

  private fun launchBusy(block: suspend () -> Unit) {
    viewModelScope.launch {
      _isBusy.value = true
      try {
        block()
      } finally {
        _isBusy.value = false
      }
    }
  }

  private fun <T> Flow<T>.toUiState(fallbackMsg: String): Flow<UiState<T>> =
    map<T, UiState<T>> { UiState.Success(it) }.catch { error ->
      val firestoreError =
        generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      val root = firestoreError ?: error
      emit(UiState.Error(root.message ?: fallbackMsg))
    }

  companion object {
    fun currentDateKey(): String =
      SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun currentMonthKey(): String =
      SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
  }
}
