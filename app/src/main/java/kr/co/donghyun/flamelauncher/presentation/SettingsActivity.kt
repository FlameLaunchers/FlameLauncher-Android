package kr.co.donghyun.flamelauncher.presentation

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import kr.co.donghyun.flamelauncher.presentation.base.BaseActivity
import kr.co.donghyun.flamelauncher.presentation.settings.SettingsViewModel
import kr.co.donghyun.flamelauncher.presentation.ui.screen.SettingsScreen
import kr.co.donghyun.flamelauncher.presentation.ui.theme.FlameLauncherTheme

@AndroidEntryPoint
class SettingsActivity : BaseActivity() {
    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, SettingsActivity::class.java))
        }
    }

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreated() {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(scrim = android.graphics.Color.TRANSPARENT)
        )

        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val totalRamMb = (memInfo.totalMem / 1024 / 1024).toInt()
        val maxHeapCeilingMb = (totalRamMb / 256) * 256

        setContent {
            FlameLauncherTheme {
                val settings by viewModel.settings.collectAsState()
                val saved by viewModel.saved.collectAsState()
                val globalRenderer by viewModel.globalRenderer.collectAsState()

                SettingsScreen(
                    onBack = { finish() },
                    settings = settings,
                    saved = saved,
                    globalRenderer = globalRenderer,
                    totalRamMb = totalRamMb,
                    maxHeapCeilingMb = maxHeapCeilingMb,
                    onSettingsChange = { viewModel.updateSettings(it) },
                    onReset = { viewModel.resetSettings() },
                    onSave = { viewModel.saveExplicitly() },
                    onGlobalRendererChange = { viewModel.setGlobalRenderer(it) },
                )
            }
        }
    }
}
