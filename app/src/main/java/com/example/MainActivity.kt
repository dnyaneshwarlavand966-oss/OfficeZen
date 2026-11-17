package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MemberRole
import com.example.data.prefs.AppPreferences
import com.example.data.repository.OfficeRepository
import com.example.localization.AppLocalization
import com.example.localization.IndianLanguages
import com.example.localization.LocalAppStrings
import com.example.notifications.NotificationHelper
import com.example.notifications.ReminderScheduler
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.GoogleAuthHelper
import com.example.ui.components.LanguagePickerDialog
import com.example.ui.components.OfficeTopBar
import com.example.ui.screens.ChaiDocsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MoreAuditScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.TeamPayScreen
import com.example.ui.theme.OfficeZenTheme
import com.example.ui.viewmodel.OfficeViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import java.util.Calendar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      OfficeZenTheme {
        OfficeZenRoot()
      }
    }
  }
}

@Composable
private fun OfficeZenRoot() {
  val context = LocalContext.current
  val appContext = context.applicationContext
  val scope = rememberCoroutineScope()
  val preferences = remember(appContext) { AppPreferences(appContext) }
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val languageCode by preferences.languageCodeFlow.collectAsStateWithLifecycle(initialValue = "en")
  val currentLang = remember(languageCode) { IndianLanguages.findByCode(languageCode) }
  val strings = remember(languageCode) { AppLocalization.forLanguage(languageCode) }
  val layoutDirection = if (currentLang.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

  var showLanguageDialog by remember { mutableStateOf(false) }
  var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }
  var authInitialized by remember { mutableStateOf(Firebase.auth.currentUser != null) }

  DisposableEffect(Unit) {
    val auth = Firebase.auth
    val listener =
      FirebaseAuth.AuthStateListener { firebaseAuth ->
        currentUser = firebaseAuth.currentUser
      }
    auth.addAuthStateListener(listener)
    onDispose { auth.removeAuthStateListener(listener) }
  }

  LaunchedEffect(Unit) {
    if (Firebase.auth.currentUser == null) {
      GoogleAuthHelper.attemptAutoSignIn(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = {
          currentUser = Firebase.auth.currentUser
          authInitialized = true
        },
        onUnauthenticated = { authInitialized = true },
        scope = scope,
      )
    } else {
      authInitialized = true
    }
  }

  CompositionLocalProvider(
    LocalAppStrings provides strings,
    LocalLayoutDirection provides layoutDirection,
  ) {
    if (showLanguageDialog) {
      LanguagePickerDialog(
        selectedCode = languageCode,
        onSelect = { code ->
          scope.launch { preferences.setLanguageCode(code) }
        },
        onDismiss = { showLanguageDialog = false },
      )
    }

    val user = currentUser
    if (!authInitialized) {
      Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator()
      }
    } else if (user == null) {
      AuthScreen(
        selectedLanguageCode = languageCode,
        onOpenLanguagePicker = { showLanguageDialog = true },
        onAuthSuccess = { currentUser = Firebase.auth.currentUser },
      )
    } else {
      AuthenticatedOfficeApp(
        user = user,
        preferences = preferences,
        onOpenLanguagePicker = { showLanguageDialog = true },
        onSignOut = {
          GoogleAuthHelper.signOut(
            credentialManager = credentialManager,
            scope = scope,
            onSignOutComplete = { currentUser = null },
          )
        },
      )
    }
  }
}

@Composable
private fun AuthenticatedOfficeApp(
  user: FirebaseUser,
  preferences: AppPreferences,
  onOpenLanguagePicker: () -> Unit,
  onSignOut: () -> Unit,
) {
  val context = LocalContext.current
  val appContext = context.applicationContext
  val strings = LocalAppStrings.current

  val viewModel =
    remember(user.uid) {
      OfficeViewModel(
        repository = OfficeRepository(appContext),
        preferences = preferences,
        currentUserId = user.uid,
        userDisplayName = user.displayName ?: "",
        userEmail = user.email ?: "",
      )
    }

  val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
  val officeState by viewModel.officeState.collectAsStateWithLifecycle()
  val office = (officeState as? UiState.Success)?.data
  val onboardingError by viewModel.onboardingError.collectAsStateWithLifecycle()
  val profileSyncError by viewModel.profileSyncError.collectAsStateWithLifecycle()
  val me by viewModel.myMemberState.collectAsStateWithLifecycle()
  val members by viewModel.membersState.collectAsStateWithLifecycle()
  val payrollProfiles by viewModel.payrollProfilesState.collectAsStateWithLifecycle()
  val attendance by viewModel.attendanceState.collectAsStateWithLifecycle()
  val leaves by viewModel.leavesState.collectAsStateWithLifecycle()
  val teaLogs by viewModel.teaLogsState.collectAsStateWithLifecycle()
  val teaPurchases by viewModel.teaPurchasesState.collectAsStateWithLifecycle()
  val documents by viewModel.documentsState.collectAsStateWithLifecycle()
  val auditLogs by viewModel.auditLogsState.collectAsStateWithLifecycle()
  val monthEndSummary by viewModel.monthEndSummaryState.collectAsStateWithLifecycle()
  val selectedMonthKey by viewModel.selectedMonthKey.collectAsStateWithLifecycle()
  val languageCode by viewModel.languageCode.collectAsStateWithLifecycle()
  val remindersEnabled by viewModel.remindersEnabled.collectAsStateWithLifecycle()
  val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
  val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(bannerMessage) {
    val msg = bannerMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearBanner()
    }
  }

  LaunchedEffect(remindersEnabled, me?.roleEnum, office?.id) {
    ReminderScheduler.scheduleDailyAndMonthEndReminders(appContext, remindersEnabled)
    val isManagerOrOwner = me?.roleEnum == MemberRole.OWNER || me?.roleEnum == MemberRole.MANAGER
    if (remindersEnabled && isManagerOrOwner && office != null) {
      val cal = Calendar.getInstance()
      val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
      val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
      val currentMonth = OfficeViewModel.currentMonthKey()
      val lastNotified = preferences.lastNotifiedMonthFlow.first()
      if (dayOfMonth >= maxDay - 1 && lastNotified != currentMonth) {
        NotificationHelper.showNotification(
          appContext,
          9091,
          "${office!!.name}: Month-End Report Ready",
          "Export & share payroll, attendance and tea/coffee summary via WhatsApp, Email, PDF or Excel CSV.",
        )
        preferences.setLastNotifiedMonth(currentMonth)
      }
    }
  }

  var currentTab by rememberSaveable { mutableIntStateOf(0) }
  var chaiDocsInitialSubTab by rememberSaveable { mutableIntStateOf(0) }
  var openLeaveDialogOnMore by rememberSaveable { mutableStateOf(false) }

  BackHandler(enabled = currentTab != 0) { currentTab = 0 }

  Scaffold(
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      OfficeTopBar(
        officeName = office?.name ?: strings.appName,
        role = me?.roleEnum,
        selectedLanguageCode = languageCode,
        onOpenLanguagePicker = onOpenLanguagePicker,
        onSignOut = onSignOut,
      )
    },
    bottomBar = {
      val activeOffice = office
      if (activeOffice != null) {
        BoxWithConstraints {
          if (maxWidth < 600.dp) {
            NavigationBar {
              NavigationBarItem(
                selected = currentTab == 0,
                onClick = { currentTab = 0 },
                icon = { Icon(Icons.Rounded.Home, contentDescription = strings.navHome) },
                label = { Text(strings.navHome, maxLines = 1) },
                modifier = Modifier.testTag("nav_home"),
              )
              NavigationBarItem(
                selected = currentTab == 1,
                onClick = { currentTab = 1 },
                icon = { Icon(Icons.Rounded.Groups, contentDescription = strings.navPeoplePay) },
                label = { Text(strings.navPeoplePay, maxLines = 1) },
                modifier = Modifier.testTag("nav_team_pay"),
              )
              NavigationBarItem(
                selected = currentTab == 2,
                onClick = { currentTab = 2 },
                icon = { Icon(Icons.Rounded.Coffee, contentDescription = strings.navChaiDocs) },
                label = { Text(strings.navChaiDocs, maxLines = 1) },
                modifier = Modifier.testTag("nav_chai_docs"),
              )
              NavigationBarItem(
                selected = currentTab == 3,
                onClick = { currentTab = 3 },
                icon = { Icon(Icons.Rounded.Menu, contentDescription = strings.navMore) },
                label = { Text(strings.navMore, maxLines = 1) },
                modifier = Modifier.testTag("nav_more"),
              )
            }
          }
        }
      }
    },
  ) { innerPadding ->
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
          .padding(innerPadding),
    ) {
      when (val profileState = userProfileState) {
        UiState.Loading -> {
          val syncErr = profileSyncError
          if (syncErr != null) {
            FirestoreErrorPane(
              message = syncErr,
              primaryActionLabel = "Retry",
              onPrimaryAction = { viewModel.retryConnection() },
              secondaryActionLabel = strings.signOut,
              onSecondaryAction = onSignOut,
            )
          } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              CircularProgressIndicator()
            }
          }
        }

        is UiState.Error -> {
          FirestoreErrorPane(
            message = profileState.message,
            primaryActionLabel = "Retry",
            onPrimaryAction = { viewModel.retryConnection() },
            secondaryActionLabel = strings.signOut,
            onSecondaryAction = onSignOut,
          )
        }

        is UiState.Success -> {
          val syncErr = profileSyncError
          if (profileState.data == null && syncErr != null) {
            FirestoreErrorPane(
              message = syncErr,
              primaryActionLabel = "Retry",
              onPrimaryAction = { viewModel.retryConnection() },
              secondaryActionLabel = strings.signOut,
              onSecondaryAction = onSignOut,
            )
          } else {
            val activeOfficeId = profileState.data?.activeOfficeId.orEmpty()
            if (activeOfficeId.isBlank()) {
              OnboardingScreen(
                isBusy = isBusy,
                errorMessage = onboardingError,
                onClearError = { viewModel.clearOnboardingError() },
                onCreateOffice = { name, phone, lat, lng, radius, startM, endM, graceM, days ->
                  viewModel.createOffice(name, phone, lat, lng, radius, startM, endM, graceM, days)
                },
                onJoinOffice = { code, desig, phone ->
                  viewModel.joinOffice(code, desig, phone)
                },
              )
            } else {
              when (val currentOfficeState = officeState) {
                UiState.Loading -> {
                  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                  }
                }

                is UiState.Error -> {
                  FirestoreErrorPane(
                    message = currentOfficeState.message,
                    primaryActionLabel = "Retry",
                    onPrimaryAction = { viewModel.retryConnection() },
                    secondaryActionLabel = "Switch Workspace",
                    onSecondaryAction = { viewModel.switchWorkspace() },
                  )
                }

                is UiState.Success -> {
                  val activeOffice = currentOfficeState.data
                  if (activeOffice == null) {
                    FirestoreErrorPane(
                      message = "Office workspace not found or access was removed.",
                      primaryActionLabel = "Retry",
                      onPrimaryAction = { viewModel.retryConnection() },
                      secondaryActionLabel = "Switch Workspace",
                      onSecondaryAction = { viewModel.switchWorkspace() },
                    )
                  } else {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
              val useRail = maxWidth >= 600.dp
              Row(modifier = Modifier.fillMaxSize()) {
                if (useRail) {
                  NavigationRail(modifier = Modifier.fillMaxHeight()) {
                    NavigationRailItem(
                      selected = currentTab == 0,
                      onClick = { currentTab = 0 },
                      icon = { Icon(Icons.Rounded.Home, contentDescription = strings.navHome) },
                      label = { Text(strings.navHome) },
                      modifier = Modifier.testTag("nav_home"),
                    )
                    NavigationRailItem(
                      selected = currentTab == 1,
                      onClick = { currentTab = 1 },
                      icon = { Icon(Icons.Rounded.Groups, contentDescription = strings.navPeoplePay) },
                      label = { Text(strings.navPeoplePay) },
                      modifier = Modifier.testTag("nav_team_pay"),
                    )
                    NavigationRailItem(
                      selected = currentTab == 2,
                      onClick = { currentTab = 2 },
                      icon = { Icon(Icons.Rounded.Coffee, contentDescription = strings.navChaiDocs) },
                      label = { Text(strings.navChaiDocs) },
                      modifier = Modifier.testTag("nav_chai_docs"),
                    )
                    NavigationRailItem(
                      selected = currentTab == 3,
                      onClick = { currentTab = 3 },
                      icon = { Icon(Icons.Rounded.Menu, contentDescription = strings.navMore) },
                      label = { Text(strings.navMore) },
                      modifier = Modifier.testTag("nav_more"),
                    )
                  }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                  when (currentTab) {
                    0 ->
                      HomeScreen(
                        office = activeOffice,
                        me = me,
                        attendance = attendance,
                        leaves = leaves,
                        isBusy = isBusy,
                        onCheckIn = { lat, lng, dist, outOfRange, selfieB64, note ->
                          viewModel.checkIn(lat, lng, dist, outOfRange, selfieB64, note)
                        },
                        onCheckOut = { rec, note ->
                          viewModel.checkOut(rec, note)
                        },
                        onOneTapTea = { item ->
                          viewModel.logOneTapTeaOrCoffee(item)
                        },
                        onOpenApplyLeave = {
                          openLeaveDialogOnMore = true
                          currentTab = 3
                        },
                        onOpenDocumentsTab = {
                          chaiDocsInitialSubTab = 1
                          currentTab = 2
                        },
                        onOpenApprovalsTab = {
                          openLeaveDialogOnMore = false
                          currentTab = 3
                        },
                        onShowError = { msg -> viewModel.showMessage(msg) },
                      )

                    1 ->
                      TeamPayScreen(
                        office = activeOffice,
                        me = me,
                        members = members,
                        profiles = payrollProfiles,
                        monthKey = selectedMonthKey,
                        summary = monthEndSummary,
                        onMonthChange = { viewModel.setMonthKey(it) },
                        onUpdateMember = { member, role, desig, active, salary, otRate, phone, email ->
                          viewModel.updateMemberRoleAndSalary(
                            member,
                            role,
                            desig,
                            active,
                            salary,
                            otRate,
                            phone,
                            email,
                          )
                        },
                        onAddAdjustment = { target, type, amount, note ->
                          viewModel.addSalaryAdjustment(target, type, amount, note)
                        },
                      )

                    2 ->
                      ChaiDocsScreen(
                        initialSubTab = chaiDocsInitialSubTab,
                        office = activeOffice,
                        me = me,
                        teaLogs = teaLogs,
                        teaPurchases = teaPurchases,
                        documents = documents,
                        monthEndSummary = monthEndSummary,
                        onOneTapTea = { item -> viewModel.logOneTapTeaOrCoffee(item) },
                        onDeleteTeaLog = { logId -> viewModel.deleteTeaLog(logId) },
                        onAddPurchase = { cat, qty, amt, note ->
                          viewModel.addTeaPurchase(cat, qty, amt, note)
                        },
                        onAddDocument = { dir, party, subj, status, scanB64, note ->
                          viewModel.addDocument(dir, party, subj, status, scanB64, note)
                        },
                        onUpdateDocumentStatus = { doc, newStatus ->
                          viewModel.updateDocumentStatus(doc, newStatus)
                        },
                      )

                    else ->
                      MoreAuditScreen(
                        openLeaveDialogInitially = openLeaveDialogOnMore,
                        onClearLeaveDialogFlag = { openLeaveDialogOnMore = false },
                        office = activeOffice,
                        me = me,
                        leaves = leaves,
                        auditLogs = auditLogs,
                        attendance = attendance,
                        documents = documents,
                        selectedLanguageCode = languageCode,
                        remindersEnabled = remindersEnabled,
                        onToggleReminders = { viewModel.setRemindersEnabled(it) },
                        onOpenLanguagePicker = onOpenLanguagePicker,
                        onSubmitLeave = { type, start, end, days, reason ->
                          viewModel.submitLeave(type, start, end, days, reason)
                        },
                        onReviewLeave = { lv, approved, note ->
                          viewModel.reviewLeave(lv, approved, note)
                        },
                        onUpdateOfficeSettings = { updated ->
                          viewModel.updateOfficeSettings(updated)
                        },
                        onSwitchWorkspace = { viewModel.switchWorkspace() },
                      )
                  }
                }
              }
            }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun FirestoreErrorPane(
  message: String,
  primaryActionLabel: String,
  onPrimaryAction: () -> Unit,
  secondaryActionLabel: String,
  onSecondaryAction: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxSize().padding(24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = message,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.error,
    )
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Button(
        onClick = onPrimaryAction,
        modifier = Modifier.testTag("retry_firestore_button"),
      ) {
        Text(primaryActionLabel)
      }
      OutlinedButton(
        onClick = onSecondaryAction,
        modifier = Modifier.testTag("secondary_error_action_button"),
      ) {
        Text(secondaryActionLabel)
      }
    }
  }
}
