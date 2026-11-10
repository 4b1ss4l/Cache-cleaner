package com.example.shizuku

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.model.ShizukuStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

class ShizukuManager(private val context: Context) {

    companion object {
        const val TAG = "ShizukuManager"
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        val KNOWN_SHIZUKU_PACKAGES = listOf("moe.shizuku.privileged.api", "moe.shizuku.manager")
        const val SHIZUKU_REQUEST_CODE = 4001
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private val _status = MutableStateFlow<ShizukuStatus>(ShizukuStatus.Checking)
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        mainHandler.post {
            checkStatus()
        }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        mainHandler.post {
            _status.value = ShizukuStatus.ServiceStopped
        }
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_REQUEST_CODE) {
                mainHandler.post {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        checkStatus()
                    } else {
                        _status.value = ShizukuStatus.Unauthorized
                    }
                }
            }
        }

    init {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error attaching Shizuku listeners", e)
        }
        checkStatus()
    }

    fun isShizukuInstalled(): Boolean {
        val pm = context.packageManager
        for (pkg in KNOWN_SHIZUKU_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0)
                return true
            } catch (_: Exception) {}
        }
        return try {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val activities = pm.queryIntentActivities(intent, 0)
            activities.any { it.activityInfo.packageName in KNOWN_SHIZUKU_PACKAGES }
        } catch (_: Exception) {
            false
        }
    }

    fun checkStatus() {
        try {
            // 1. Priority check: is the Shizuku Binder currently alive and communicating?
            val isBinderAlive = try {
                Shizuku.pingBinder()
            } catch (e: Exception) {
                Log.w(TAG, "pingBinder threw exception: ${e.message}")
                false
            }

            if (isBinderAlive) {
                val isGranted = if (Shizuku.isPreV11()) {
                    false
                } else {
                    try {
                        val perm = Shizuku.checkSelfPermission()
                        perm == PackageManager.PERMISSION_GRANTED
                    } catch (e: Exception) {
                        Log.e(TAG, "checkSelfPermission failed: ${e.message}")
                        false
                    }
                }

                if (isGranted) {
                    val version = runCatching { Shizuku.getVersion() }.getOrDefault(0)
                    val uid = runCatching { Shizuku.getUid() }.getOrDefault(2000)
                    val seContext = runCatching { Shizuku.getSELinuxContext() }.getOrDefault("")
                    _status.value = ShizukuStatus.Authorized(version = version, uid = uid, seContext = seContext)
                } else {
                    _status.value = ShizukuStatus.Unauthorized
                }
                return
            }

            // 2. If binder is not active, check if Shizuku app is installed on device
            val installed = isShizukuInstalled()
            if (installed) {
                _status.value = ShizukuStatus.ServiceStopped
            } else {
                _status.value = ShizukuStatus.NotInstalled
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Shizuku status", e)
            _status.value = ShizukuStatus.Error(e.message ?: "Falha ao verificar serviço")
        }
    }

    fun requestPermission() {
        try {
            if (Shizuku.pingBinder()) {
                val currentPerm = try {
                    Shizuku.checkSelfPermission()
                } catch (_: Exception) {
                    PackageManager.PERMISSION_DENIED
                }
                if (currentPerm == PackageManager.PERMISSION_GRANTED) {
                    checkStatus()
                } else {
                    Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
                }
            } else {
                checkStatus()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting Shizuku permission", e)
            _status.value = ShizukuStatus.Error(e.message ?: "Erro ao solicitar permissão")
        }
    }

    suspend fun executeCommand(command: Array<String>): CommandResult = withContext(Dispatchers.IO) {
        if (!Shizuku.pingBinder()) {
            return@withContext CommandResult(
                success = false,
                exitCode = -1,
                output = "",
                error = "Serviço Shizuku não está ativo"
            )
        }

        try {
            val process = runCatching {
                val method = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                ).apply { isAccessible = true }
                method.invoke(null, command, null, null) as Process
            }.getOrElse {
                val method = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java,
                    Int::class.javaPrimitiveType
                ).apply { isAccessible = true }
                method.invoke(null, command, null, null, 0) as Process
            }

            val stdout = StringBuilder()
            val stderr = StringBuilder()

            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    stdout.appendLine(line)
                }
            }

            BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    stderr.appendLine(line)
                }
            }

            val exitCode = process.waitFor()
            CommandResult(
                success = exitCode == 0,
                exitCode = exitCode,
                output = stdout.toString().trim(),
                error = stderr.toString().trim()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error executing Shizuku command: ${command.joinToString(" ")}", e)
            CommandResult(
                success = false,
                exitCode = -1,
                output = "",
                error = e.message ?: "Erro ao executar comando"
            )
        }
    }

    suspend fun trimCaches(desiredBytes: Long = 999999999999L): CommandResult {
        return executeCommand(arrayOf("pm", "trim-caches", desiredBytes.toString()))
    }

    suspend fun clearPackageExternalCache(packageName: String): CommandResult {
        val targetPath = "/sdcard/Android/data/$packageName/cache"
        return executeCommand(arrayOf("sh", "-c", "rm -rf $targetPath/*"))
    }

    fun openShizukuApp(): Boolean {
        val pm = context.packageManager
        for (pkg in KNOWN_SHIZUKU_PACKAGES) {
            try {
                val intent = pm.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return true
                }
            } catch (_: Exception) {}
        }
        openShizukuWebsite()
        return false
    }

    fun openShizukuWebsite() {
        try {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://shizuku.rikka.app")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun destroy() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        } catch (_: Exception) {}
    }

    data class CommandResult(
        val success: Boolean,
        val exitCode: Int,
        val output: String,
        val error: String
    )
}
