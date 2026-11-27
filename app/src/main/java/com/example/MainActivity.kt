// OfficeZen main entry point (Offline-first Room + DataStore + Firebase Auth/Sync + Responsive Navigation)
package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.WorkInfo
import com.example.auth.GoogleAuthHelper
import com.example.data.model.MemberRole
import com.example.data.prefs.AppPreferences
import com.example.data.repository.OfficeRepository
import com.example.data.repository.SubscriptionRepository
import com.example.hardware.LocationAndImageHelper
import com.example.localization.AppLocalization
import com.example.localization.GlobalLocaleConfig
import com.example.localization.IndianLanguages
import com.example.localization.LocalAppStrings
import com.example.localization.LocalGlobalLocaleConfig
import com.example.localization.ThemeMode
import com.example.notifications.NotificationHelper
import com.example.notifications.ReminderScheduler
import com.example.sync.AttendanceSyncScheduler
import com.example.sync.NetworkConnectivityObserver
import com.example.ui.components.FloatingPillBottomBar
import com.example.ui.components.LanguagePickerDialog
import com.example.ui.components.OfficeTopBar
import com.example.ui.components.ProfileAvatarDialog
import com.example.ui.components.ZenAmbientBackground
import com.example.ui.components.ZenCard
import com.example.ui.navigation.NavigationChromeMode
import com.example.ui.navigation.OfficeZenRailAndDrawerShell
import com.example.ui.navigation.OfficeZenRoute
import com.example.ui.navigation.buildPrimaryDestinations
import com.example.ui.navigation.resolveNavigationChromeMode
import com.example.ui.navigation.toDockNavItems
import com.example.ui.screens.ChaiDocsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.MoreAuditScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SubscriptionScreen
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
      OfficeZenRoot()
    }
  }
}

@Composable
private fun OfficeZenRoot() {
  val context = LocalContext.current
  val appContext = context.applicationContext
  val scope = rememberCoroutineScope()
  val preferences = remember(appContext) { AppPreferences(appContext) }

  val languageCode by preferences.languageCodeFlow.collectAsStateWithLifecycle(initialValue = "en")
  val themeMode by preferences.themeModeFlow.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
  val defaultLocale = remember { GlobalLocaleConfig.defaultFromSystem() }
  val globalLocaleConfig by preferences.globalLocaleConfigFlow.collectAsStateWithLifecycle(initialValue = defaultLocale)
  val currentLang = remember(languageCode) { IndianLanguages.findByCode(languageCode) }
  val strings = remember(languageCode) { AppLocalization.forLanguage(languageCode) }
  val layoutDirection = if (currentLang.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

  var showLanguageDialog by remember { mutableStateOf(false) }

  OfficeZenTheme(themeMode = themeMode) {
    CompositionLocalProvider(
      LocalAppStrings provides strings,
      LocalLayoutDirection provides layoutDirection,
      LocalGlobalLocaleConfig provides globalLocaleConfig,
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

      LocalOfficeApp(
        preferences = preferences,
        themeMode = themeMode,
        globalLocaleConfig = globalLocaleConfig,
        onOpenLanguagePicker = { showLanguageDialog = true },
      )
    }
  }
}

@Composable
private fun LocalOfficeApp(
  preferences: AppPreferences,
  themeMode: ThemeMode,
  globalLocaleConfig: GlobalLocaleConfig,
  onOpenLanguagePicker: () -> Unit,
) {
  val context = LocalContext.current
  val appContext = context.applicationContext
  val scope = rememberCoroutineScope()
  val strings = LocalAppStrings.current

  val credentialManager = remember(appContext) { CredentialManager.create(appContext) }
  var currentGoogleUser by remember {
    mutableStateOf<FirebaseUser?>(runCatching { Firebase.auth.currentUser }.getOrNull())
  }
  var isAuthLoading by remember { mutableStateOf(false) }

  DisposableEffect(Unit) {
    val auth = runCatching { Firebase.auth }.getOrNull()
    val listener =
      FirebaseAuth.AuthStateListener { firebaseAuth ->
        currentGoogleUser = firebaseAuth.currentUser
      }
    auth?.addAuthStateListener(listener)
    onDispose {
      auth?.removeAuthStateListener(listener)
    }
  }

  val connectivityObserver =
    remember(appContext) {
      NetworkConnectivityObserver(appContext) {
        AttendanceSyncScheduler.enqueueSyncWhenConnected(appContext)
      }
    }
  DisposableEffect(connectivityObserver) {
    connectivityObserver.startObserving()
    onDispose { connectivityObserver.stopObserving() }
  }
  val isNetworkOnline by connectivityObserver.isOnline.collectAsStateWithLifecycle()

  val viewModel =
    remember(appContext) {
      OfficeViewModel(
        repository = OfficeRepository(appContext),
        preferences = preferences,
        onAttendanceQueuedForSync = {
          AttendanceSyncScheduler.enqueueSyncWhenConnected(appContext)
          AttendanceSyncScheduler.ensurePeriodicSyncScheduled(appContext)
        },
        subscriptionRepository = SubscriptionRepository(appContext),
      )
    }

  val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
  val officeState by viewModel.officeState.collectAsStateWithLifecycle()
  val allOffices by viewModel.allOfficesState.collectAsStateWithLifecycle()
  val localUserName by viewModel.localUserName.collectAsStateWithLifecycle()
  val office = (officeState as? UiState.Success)?.data
  val onboardingError by viewModel.onboardingError.collectAsStateWithLifecycle()
  val me by viewModel.myMemberState.collectAsStateWithLifecycle()
  val members by viewModel.membersState.collectAsStateWithLifecycle()
  val payrollProfiles by viewModel.payrollProfilesState.collectAsStateWithLifecycle()
  val attendance by viewModel.attendanceState.collectAsStateWithLifecycle()
  val pendingSyncCount by viewModel.pendingAttendanceCountState.collectAsStateWithLifecycle()
  val syncedAttendanceCount by viewModel.syncedAttendanceCountState.collectAsStateWithLifecycle()
  val lastSyncEpochMs by viewModel.lastSyncEpochMsState.collectAsStateWithLifecycle()
  val lastSyncMessage by viewModel.lastSyncMessageState.collectAsStateWithLifecycle()
  val syncWorkInfos by
    remember(appContext) { AttendanceSyncScheduler.observeSyncWorkInfos(appContext) }
      .collectAsStateWithLifecycle(initialValue = emptyList())
  val isSyncRunning =
    remember(syncWorkInfos) {
      syncWorkInfos.any {
        it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
      }
    }
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
  val subscription by viewModel.subscriptionState.collectAsStateWithLifecycle()
  val paymentHistory by viewModel.paymentHistoryState.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(Unit) {
    AttendanceSyncScheduler.ensurePeriodicSyncScheduled(appContext)
    runCatching {
      GoogleAuthHelper.attemptAutoSignIn(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = {
          currentGoogleUser = runCatching { Firebase.auth.currentUser }.getOrNull()
        },
        onUnauthenticated = {},
        scope = scope,
      )
    }
  }

  LaunchedEffect(pendingSyncCount, isNetworkOnline, currentGoogleUser?.uid) {
    if (pendingSyncCount > 0 && isNetworkOnline && currentGoogleUser != null) {
      AttendanceSyncScheduler.enqueueSyncWhenConnected(appContext)
    }
  }

  val onSignInWithGoogle: () -> Unit = {
    isAuthLoading = true
    GoogleAuthHelper.onGoogleSignInClicked(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = {
        isAuthLoading = false
        currentGoogleUser = runCatching { Firebase.auth.currentUser }.getOrNull()
        viewModel.triggerAttendanceSync()
        viewModel.showMessage("Signed in · Syncing pending attendance")
      },
      onAuthError = { msg ->
        isAuthLoading = false
        viewModel.showMessage(msg)
      },
      onAuthCancelled = {
        isAuthLoading = false
        viewModel.showMessage("Google Sign-In cancelled")
      },
      scope = scope,
    )
  }

  val onSignOutGoogle: () -> Unit = {
    GoogleAuthHelper.signOut(
      credentialManager = credentialManager,
      scope = scope,
      onSignOutComplete = {
        currentGoogleUser = null
        viewModel.showMessage("Signed out of Google sync (local Room data preserved)")
      },
    )
  }

  val onTriggerSyncNow: () -> Unit = {
    viewModel.triggerAttendanceSync()
    viewModel.showMessage(
      if (isNetworkOnline) "Uploading pending attendance to Firestore..."
      else "Offline · Auto-upload scheduled when network reconnects",
    )
  }

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
          "${office.name}: Month-End Report Ready",
          "Export & share payroll, attendance and tea/coffee summary via WhatsApp, Email, PDF or Excel CSV.",
        )
        preferences.setLastNotifiedMonth(currentMonth)
      }
    }
  }

  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route
  val primaryDestinations = remember(strings) { buildPrimaryDestinations(strings) }
  val selectedPrimaryIndex =
    remember(currentRoute, primaryDestinations) {
      primaryDestinations.indexOfFirst { it.route.route == currentRoute }
    }

  var chaiDocsInitialSubTab by rememberSaveable { mutableIntStateOf(0) }
  var moreInitialSubTab by rememberSaveable { mutableIntStateOf(0) }
  var openLeaveDialogOnMore by rememberSaveable { mutableStateOf(false) }
  var showTermsDialog by remember { mutableStateOf(false) }

  fun navigateToRoute(route: OfficeZenRoute) {
    if (route == OfficeZenRoute.Subscription) {
      navController.navigate(route.route) {
        launchSingleTop = true
      }
    } else {
      navController.navigate(route.route) {
        popUpTo(OfficeZenRoute.Home.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
      }
    }
  }

  val preferInitialsAvatar by preferences.preferInitialsAvatarFlow.collectAsStateWithLifecycle(initialValue = false)
  var profilePhotoVersion by remember { mutableIntStateOf(0) }
  var showProfileDialog by remember { mutableStateOf(false) }

  val todayKey = OfficeViewModel.currentDateKey()
  val myTodayRecord =
    remember(attendance, me, todayKey) {
      attendance.firstOrNull { it.userId == me?.userId && it.dateKey == todayKey }
    }
  val myTodaySelfie =
    remember(myTodayRecord?.id, myTodayRecord?.selfieBase64) {
      val rec = myTodayRecord ?: return@remember null
      LocationAndImageHelper.loadAttendanceSelfie(
        context = context,
        recordId = rec.id,
        fallbackBase64 = rec.selfieBase64,
      )
    }

  val profileUserId = me?.userId?.takeIf { it.isNotBlank() } ?: viewModel.currentUserId
  val savedProfilePhoto =
    remember(profileUserId, profilePhotoVersion) {
      LocationAndImageHelper.loadProfilePhoto(appContext, profileUserId)
    }
  val effectiveAvatarBitmap = if (preferInitialsAvatar) null else (savedProfilePhoto ?: myTodaySelfie)

  val profilePhotoLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        scope.launch {
          val savedBitmap = LocationAndImageHelper.saveProfilePhotoFromUri(appContext, profileUserId, uri)
          if (savedBitmap != null) {
            preferences.setPreferInitialsAvatar(false)
            profilePhotoVersion++
            viewModel.showMessage("Profile photo updated")
          } else {
            viewModel.showMessage("Could not load selected image")
          }
        }
      }
    }

  if (showProfileDialog && office != null) {
    val resolvedName = (me?.displayName ?: localUserName).ifBlank { "Office Owner" }
    val roleLabel =
      when (me?.roleEnum) {
        MemberRole.OWNER -> strings.roleOwner
        MemberRole.MANAGER -> strings.roleManager
        MemberRole.EMPLOYEE -> strings.roleEmployee
        null -> strings.roleOwner
      }
    val subtitle =
      if (!me?.designation.isNullOrBlank()) "${me?.designation} · $roleLabel"
      else roleLabel

    ProfileAvatarDialog(
      displayName = resolvedName,
      subtitle = subtitle,
      officeName = office.name,
      profilePhotoBitmap = effectiveAvatarBitmap,
      onPickPhoto = {
        profilePhotoLauncher.launch(
          PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
      },
      onRemovePhoto = {
        scope.launch {
          LocationAndImageHelper.deleteProfilePhoto(appContext, profileUserId)
          preferences.setPreferInitialsAvatar(true)
          profilePhotoVersion++
          viewModel.showMessage("Switched to initials avatar")
        }
      },
      onDismiss = { showProfileDialog = false },
    )
  }

  if (showTermsDialog) {
    AlertDialog(
      onDismissRequest = { showTermsDialog = false },
      shape = RoundedCornerShape(24.dp),
      title = { Text("Terms & Privacy Policy", style = MaterialTheme.typography.titleLarge) },
      text = {
        Text(
          text =
            "OfficeZen stores operational records locally in encrypted device Room storage and syncs attendance to your configured Firebase workspace when signed in. Paid subscription verification and Play Billing checkout activate in Phase 10; cancelled subscriptions retain paid access through expiry and historical exports remain permanently readable.",
          style = MaterialTheme.typography.bodyMedium,
        )
      },
      confirmButton = {
        TextButton(onClick = { showTermsDialog = false }) {
          Text("Close")
        }
      },
    )
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val chromeMode = resolveNavigationChromeMode(maxWidth.value)

    ZenAmbientBackground {
      Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
          OfficeTopBar(
            officeName = office?.name ?: strings.appName,
            role = me?.roleEnum,
            selectedLanguageCode = languageCode,
            onOpenLanguagePicker = onOpenLanguagePicker,
            onSwitchWorkspace =
              if (office != null) {
                { viewModel.switchWorkspace() }
              } else {
                null
              },
            userDisplayName = me?.displayName ?: localUserName,
            userDesignation = me?.designation ?: "",
            userSelfieBitmap = effectiveAvatarBitmap,
            showProfileStrip = office != null,
            isNetworkOnline = isNetworkOnline,
            onProfileClick = if (office != null) { { showProfileDialog = true } } else null,
          )
        },
        bottomBar = {
          val activeOffice = office
          if (activeOffice != null && chromeMode == NavigationChromeMode.BOTTOM_BAR) {
            FloatingPillBottomBar(
              items = primaryDestinations.toDockNavItems(),
              selectedIndex = selectedPrimaryIndex,
              onSelect = { idx ->
                val dest = primaryDestinations[idx]
                if (dest.route == OfficeZenRoute.MoreAudit) {
                  moreInitialSubTab = 0
                }
                navigateToRoute(dest.route)
              },
            )
          }
        },
      ) { innerPadding ->
        Box(
          modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
          when (val profileState = userProfileState) {
            UiState.Loading -> {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 3.dp)
              }
            }

            is UiState.Error -> {
              LocalErrorPane(
                message = profileState.message,
                actionLabel = "Create / Select Office",
                onAction = { viewModel.switchWorkspace() },
              )
            }

            is UiState.Success -> {
              val activeOfficeId = profileState.data?.activeOfficeId.orEmpty()
              if (activeOfficeId.isBlank()) {
                OnboardingScreen(
                  isBusy = isBusy,
                  defaultOwnerName = localUserName,
                  existingOffices = allOffices,
                  errorMessage = onboardingError,
                  onClearError = { viewModel.clearOnboardingError() },
                  onSelectExistingOffice = { id -> viewModel.selectExistingOffice(id) },
                  onCreateOffice = { name, ownerName, phone, lat, lng, radius, startM, endM, graceM, days ->
                    viewModel.createOffice(name, ownerName, phone, lat, lng, radius, startM, endM, graceM, days)
                  },
                  onJoinOffice = { code, desig, phone ->
                    viewModel.joinOffice(code, desig, phone)
                  },
                )
              } else {
                when (val currentOfficeState = officeState) {
                  UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                      CircularProgressIndicator(strokeWidth = 3.dp)
                    }
                  }

                  is UiState.Error -> {
                    LocalErrorPane(
                      message = currentOfficeState.message,
                      actionLabel = "Switch Workspace",
                      onAction = { viewModel.switchWorkspace() },
                    )
                  }

                  is UiState.Success -> {
                    val activeOffice = currentOfficeState.data
                    if (activeOffice == null) {
                      LocalErrorPane(
                        message = "Local office workspace not found on device.",
                        actionLabel = "Create / Select Office",
                        onAction = { viewModel.switchWorkspace() },
                      )
                    } else {
                      val isManagerOrOwner =
                        me?.roleEnum == MemberRole.OWNER || me?.roleEnum == MemberRole.MANAGER

                      OfficeZenRailAndDrawerShell(
                        chromeMode = chromeMode,
                        officeName = activeOffice.name,
                        destinations = primaryDestinations,
                        currentRoute = currentRoute,
                        showSubscriptionShortcut = true,
                        onNavigate = { route ->
                          if (route == OfficeZenRoute.MoreAudit) {
                            moreInitialSubTab = 0
                          }
                          navigateToRoute(route)
                        },
                      ) {
                        NavHost(
                          navController = navController,
                          startDestination = OfficeZenRoute.Home.route,
                          enterTransition = { fadeIn(animationSpec = tween(180)) },
                          exitTransition = { fadeOut(animationSpec = tween(140)) },
                          popEnterTransition = { fadeIn(animationSpec = tween(180)) },
                          popExitTransition = { fadeOut(animationSpec = tween(140)) },
                        ) {
                          composable(OfficeZenRoute.Home.route) {
                            DashboardScreen(
                              office = activeOffice,
                              me = me,
                              members = members,
                              attendance = attendance,
                              leaves = leaves,
                              teaLogs = teaLogs,
                              documents = documents,
                              monthEndSummary = monthEndSummary,
                              isBusy = isBusy,
                              selectedLanguageCode = languageCode,
                              showEmbeddedHeader = false,
                              profilePhotoBitmap = effectiveAvatarBitmap,
                              preferInitialsAvatar = preferInitialsAvatar,
                              pendingSyncCount = pendingSyncCount,
                              syncedCount = syncedAttendanceCount,
                              isNetworkOnline = isNetworkOnline,
                              isSyncRunning = isSyncRunning,
                              lastSyncEpochMs = lastSyncEpochMs,
                              lastSyncMessage = lastSyncMessage,
                              googleUserEmail = currentGoogleUser?.email,
                              isAuthLoading = isAuthLoading,
                              onSignInWithGoogle = onSignInWithGoogle,
                              onSignOutGoogle = onSignOutGoogle,
                              onTriggerSyncNow = onTriggerSyncNow,
                              onProfileClick = { showProfileDialog = true },
                              onOpenLanguagePicker = onOpenLanguagePicker,
                              onSwitchWorkspace = { viewModel.switchWorkspace() },
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
                                moreInitialSubTab = 1
                                openLeaveDialogOnMore = true
                                navigateToRoute(OfficeZenRoute.MoreAudit)
                              },
                              onOpenTeaCoffeeTab = {
                                chaiDocsInitialSubTab = 0
                                navigateToRoute(OfficeZenRoute.ChaiDocs)
                              },
                              onOpenDocumentsTab = {
                                chaiDocsInitialSubTab = 1
                                navigateToRoute(OfficeZenRoute.ChaiDocs)
                              },
                              onOpenTeamPayTab = {
                                navigateToRoute(OfficeZenRoute.TeamPay)
                              },
                              onOpenReportsTab = {
                                moreInitialSubTab = 0
                                openLeaveDialogOnMore = false
                                navigateToRoute(OfficeZenRoute.MoreAudit)
                              },
                              onOpenSettingsTab = {
                                moreInitialSubTab = if (isManagerOrOwner) 3 else 2
                                openLeaveDialogOnMore = false
                                navigateToRoute(OfficeZenRoute.MoreAudit)
                              },
                              onOpenApprovalsTab = {
                                moreInitialSubTab = 1
                                openLeaveDialogOnMore = false
                                navigateToRoute(OfficeZenRoute.MoreAudit)
                              },
                              onShowError = { msg -> viewModel.showMessage(msg) },
                              onOpenSubscription = {
                                navigateToRoute(OfficeZenRoute.Subscription)
                              },
                            )
                          }

                          composable(OfficeZenRoute.TeamPay.route) {
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
                              onAddLocalMember = { name, desig, role, salary, otRate, phone ->
                                viewModel.addLocalMember(name, desig, role, salary, otRate, phone)
                              },
                            )
                          }

                          composable(OfficeZenRoute.ChaiDocs.route) {
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
                          }

                          composable(OfficeZenRoute.MoreAudit.route) {
                            MoreAuditScreen(
                              initialSubTab = moreInitialSubTab,
                              openLeaveDialogInitially = openLeaveDialogOnMore,
                              onClearLeaveDialogFlag = { openLeaveDialogOnMore = false },
                              office = activeOffice,
                              me = me,
                              leaves = leaves,
                              auditLogs = auditLogs,
                              attendance = attendance,
                              documents = documents,
                              monthEndSummary = monthEndSummary,
                              selectedLanguageCode = languageCode,
                              remindersEnabled = remindersEnabled,
                              pendingSyncCount = pendingSyncCount,
                              syncedCount = syncedAttendanceCount,
                              isNetworkOnline = isNetworkOnline,
                              isSyncRunning = isSyncRunning,
                              lastSyncEpochMs = lastSyncEpochMs,
                              lastSyncMessage = lastSyncMessage,
                              googleUserEmail = currentGoogleUser?.email,
                              isAuthLoading = isAuthLoading,
                              onSignInWithGoogle = onSignInWithGoogle,
                              onSignOutGoogle = onSignOutGoogle,
                              onTriggerSyncNow = onTriggerSyncNow,
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
                              themeMode = themeMode,
                              globalLocaleConfig = globalLocaleConfig,
                              onSetThemeMode = { viewModel.setThemeMode(it) },
                              onUpdateGlobalLocaleConfig = { viewModel.updateGlobalLocaleConfig(it) },
                              onOpenSubscription = { navigateToRoute(OfficeZenRoute.Subscription) },
                            )
                          }

                          composable(OfficeZenRoute.Subscription.route) {
                            Column(modifier = Modifier.fillMaxSize()) {
                              Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                              ) {
                                IconButton(
                                  onClick = { navController.popBackStack() },
                                  modifier = Modifier.testTag("subscription_back_button"),
                                ) {
                                  Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back",
                                  )
                                }
                                Text(
                                  text = "Plans & Billing",
                                  style = MaterialTheme.typography.titleMedium,
                                  fontWeight = FontWeight.Bold,
                                )
                              }
                              HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                thickness = 0.75.dp,
                              )
                              SubscriptionScreen(
                                subscription = subscription,
                                paymentHistory = paymentHistory,
                                onUpgradeSelected = { tier, isYearly ->
                                  val cycleLabel = if (isYearly) "Yearly" else "Monthly"
                                  viewModel.showMessage(
                                    "Selected ${tier.name} ($cycleLabel). Google Play Billing checkout connects in Phase 10.",
                                  )
                                },
                                onRestorePurchases = {
                                  viewModel.showMessage(
                                    "Restore Purchases activates with Google Play Billing in Phase 10; local status preserved.",
                                  )
                                },
                                onOpenTermsAndPrivacy = { showTermsDialog = true },
                                modifier = Modifier.weight(1f),
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
}

@Composable
private fun LocalErrorPane(
  message: String,
  actionLabel: String,
  onAction: () -> Unit,
) {
  Box(
    modifier = Modifier.fillMaxSize().padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    ZenCard(
      cornerRadius = 24.dp,
      modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(22.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Surface(
          color = MaterialTheme.colorScheme.errorContainer,
          shape = CircleShape,
          modifier = Modifier.size(48.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              Icons.Rounded.Storage,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(24.dp),
            )
          }
        }
        Text(
          text = "Local Storage Notice",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = message,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.error,
          textAlign = TextAlign.Center,
        )
        Button(
          onClick = onAction,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("local_error_action_button"),
        ) {
          Text(actionLabel)
        }
      }
    }
  }
}
