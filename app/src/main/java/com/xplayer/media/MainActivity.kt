package com.xplayer.media

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.xplayer.media.domain.model.MediaType
import com.xplayer.media.ui.screens.MainScreen
import com.xplayer.media.ui.theme.XPlayerTheme
import com.xplayer.media.ui.viewmodel.MainViewModel
import com.xplayer.media.util.PermissionHelper

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(application as XPlayerApplication)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        syncPermissionState()
        handleIncomingIntent(intent)

        setContent {
            val userPrefs by viewModel.userPrefs.collectAsState()

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) {
                syncPermissionState()
            }

            val openDocumentLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    runCatching {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                    viewModel.openExternalUri(uri)
                }
            }

            XPlayerTheme(
                darkThemeMode = userPrefs.darkThemeMode,
                dynamicColor = userPrefs.dynamicColors
            ) {
                MainScreen(
                    viewModel = viewModel,
                    onRequestPermissions = {
                        permissionLauncher.launch(PermissionHelper.getRequiredMediaPermissions())
                    },
                    onOpenExternalFile = {
                        openDocumentLauncher.launch(arrayOf("audio/*", "video/*"))
                    },
                    onEnterPip = ::enterVideoPipMode,
                    onToggleOrientation = ::toggleScreenOrientation
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        syncPermissionState()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val state = viewModel.playbackState.value
        if (state.isPlaying && state.currentMedia?.mediaType == MediaType.VIDEO) {
            enterVideoPipMode()
        }
    }

    private fun syncPermissionState() {
        val hasAccess = PermissionHelper.hasMediaAccess(this)
        val isPartial = PermissionHelper.isPartialVisualAccessOnly(this)
        viewModel.updatePermissionStatus(granted = hasAccess, isPartial = isPartial)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val dataUri = intent.data ?: return
            viewModel.openExternalUri(dataUri)
        }
    }

    private fun enterVideoPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            runCatching { enterPictureInPictureMode(params) }
        }
    }

    private fun toggleScreenOrientation() {
        requestedOrientation = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }
}
