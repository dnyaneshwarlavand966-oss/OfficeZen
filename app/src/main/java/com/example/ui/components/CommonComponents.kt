// OfficeZen shared Compose UI components & CameraX capture dialog
package com.example.ui.components

import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.MemberRole
import com.example.hardware.LocationAndImageHelper
import com.example.localization.IndianLanguages
import com.example.localization.LocalAppStrings
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenBg

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficeTopBar(
  officeName: String,
  role: MemberRole?,
  selectedLanguageCode: String,
  onOpenLanguagePicker: () -> Unit,
  onSignOut: () -> Unit,
) {
  val strings = LocalAppStrings.current
  val currentLang = remember(selectedLanguageCode) { IndianLanguages.findByCode(selectedLanguageCode) }

  TopAppBar(
    colors =
      TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.background,
      ),
    title = {
      Column {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = officeName.ifBlank { strings.appName },
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (role != null) {
            val roleLabel =
              when (role) {
                MemberRole.OWNER -> strings.roleOwner
                MemberRole.MANAGER -> strings.roleManager
                MemberRole.EMPLOYEE -> strings.roleEmployee
              }
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = RoundedCornerShape(8.dp),
            ) {
              Text(
                text = roleLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
              )
            }
          }
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.CloudDone,
            contentDescription = strings.offlineReady,
            tint = StatusGreen,
            modifier = Modifier.size(13.dp),
          )
          Text(
            text = strings.offlineReady,
            style = MaterialTheme.typography.labelMedium,
            color = StatusGreen,
          )
        }
      }
    },
    actions = {
      TextButton(
        onClick = onOpenLanguagePicker,
        modifier = Modifier.testTag("top_bar_language_button"),
      ) {
        Icon(Icons.Rounded.Language, contentDescription = strings.language, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(currentLang.nativeName, maxLines = 1)
      }
      IconButton(
        onClick = onSignOut,
        modifier = Modifier.testTag("top_bar_sign_out_button"),
      ) {
        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = strings.signOut)
      }
    },
  )
}

@Composable
fun LanguagePickerDialog(
  selectedCode: String,
  onSelect: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val strings = LocalAppStrings.current
  var query by remember { mutableStateOf("") }
  val filtered =
    remember(query) {
      if (query.isBlank()) IndianLanguages.all
      else {
        val q = query.trim().lowercase()
        IndianLanguages.all.filter {
          it.englishName.lowercase().contains(q) ||
            it.nativeName.lowercase().contains(q) ||
            it.code.lowercase().contains(q)
        }
      }
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text("${strings.language} (22 Official Indian + English)")
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          placeholder = { Text("Search Hindi, Marathi, Tamil...") },
          leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          items(filtered, key = { it.code }) { lang ->
            val isSelected = lang.code == selectedCode
            Surface(
              color =
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface,
              shape = RoundedCornerShape(12.dp),
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .clickable {
                    onSelect(lang.code)
                    onDismiss()
                  }
                  .testTag("lang_option_${lang.code}"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column {
                  Text(
                    text = lang.nativeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  )
                  Text(
                    text = "${lang.englishName}${if (lang.isRtl) " · RTL" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                if (isSelected) {
                  Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("Close") }
    },
  )
}

@Composable
fun CameraCaptureDialog(
  title: String,
  useFrontCamera: Boolean,
  onCaptured: (Bitmap) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  var captureError by remember { mutableStateOf<String?>(null) }
  var isCapturing by remember { mutableStateOf(false) }

  val previewView = remember {
    PreviewView(context).apply {
      layoutParams =
        ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT,
        )
      scaleType = PreviewView.ScaleType.FILL_CENTER
    }
  }
  val imageCapture = remember {
    ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
  }

  DisposableEffect(useFrontCamera) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    val executor = ContextCompat.getMainExecutor(context)
    var cameraProvider: ProcessCameraProvider? = null
    cameraProviderFuture.addListener(
      {
        try {
          cameraProvider = cameraProviderFuture.get()
          val preview =
            Preview.Builder().build().also {
              it.surfaceProvider = previewView.surfaceProvider
            }
          val selector =
            if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA
            else CameraSelector.DEFAULT_BACK_CAMERA
          cameraProvider?.unbindAll()
          cameraProvider?.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
        } catch (e: Exception) {
          captureError = e.localizedMessage ?: "Camera unavailable on this device"
        }
      },
      executor,
    )
    onDispose {
      cameraProvider?.unbindAll()
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(title, style = MaterialTheme.typography.titleLarge)
          IconButton(onClick = onDismiss) {
            Icon(Icons.Rounded.Close, contentDescription = "Close camera")
          }
        }

        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(300.dp)
              .clip(RoundedCornerShape(18.dp)),
          contentAlignment = Alignment.Center,
        ) {
          AndroidView(factory = { previewView }, modifier = Modifier.matchParentSize())
          if (captureError != null) {
            Surface(
              color = MaterialTheme.colorScheme.errorContainer,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.padding(16.dp),
            ) {
              Text(
                text = captureError!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(12.dp),
              )
            }
          }
        }

        Button(
          onClick = {
            isCapturing = true
            imageCapture.takePicture(
              ContextCompat.getMainExecutor(context),
              object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                  val bmp = image.toBitmap()
                  val rotated =
                    LocationAndImageHelper.rotateBitmap(
                      bmp,
                      image.imageInfo.rotationDegrees.toFloat(),
                    )
                  image.close()
                  isCapturing = false
                  onCaptured(rotated)
                }

                override fun onError(exception: ImageCaptureException) {
                  isCapturing = false
                  captureError = exception.localizedMessage ?: "Capture failed"
                }
              },
            )
          },
          enabled = !isCapturing && captureError == null,
          modifier = Modifier.fillMaxWidth().height(54.dp).testTag("camera_capture_button"),
          shape = RoundedCornerShape(14.dp),
        ) {
          Icon(Icons.Rounded.CameraAlt, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text(if (isCapturing) "Capturing..." else "Capture Photo")
        }
      }
    }
  }
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null) {
  Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleLarge,
      color = MaterialTheme.colorScheme.onBackground,
    )
    if (!subtitle.isNullOrBlank()) {
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
fun StatusPill(
  text: String,
  containerColor: androidx.compose.ui.graphics.Color = StatusGreenBg,
  contentColor: androidx.compose.ui.graphics.Color = StatusGreen,
) {
  Surface(color = containerColor, shape = RoundedCornerShape(50)) {
    Text(
      text = text,
      color = contentColor,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

@Composable
fun EmptyPlaceholderCard(title: String, body: String) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Text(title, style = MaterialTheme.typography.titleMedium)
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
      Text(
        body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
