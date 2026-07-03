package com.spendwise

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.documentfile.provider.DocumentFile
import com.spendwise.ui.SpendWiseApp
import com.spendwise.ui.settings.SettingsViewModel
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001

    private val settingsViewModel: SettingsViewModel by inject()

    private val openDocumentTree = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            val folderName = getFolderName(uri)
            settingsViewModel.setBackupFolderUri(uri.toString(), folderName)
        }
    }

    private val restoreFromFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            val folder = DocumentFile.fromTreeUri(this, uri)
            val file = folder?.findFile("SpendWise_Backup.json")
            if (file != null) {
                contentResolver.openInputStream(file.uri)?.use { inputStream ->
                    val content = inputStream.bufferedReader().use { it.readText() }
                    settingsViewModel.restoreFromBackup(content, uri.toString(), getFolderName(uri))
                }
            } else {
                settingsViewModel.showMessage("SpendWise_Backup.json not found in selected folder")
            }
        }
    }

    private fun getFolderName(uri: Uri): String? {
        val documentId = DocumentsContract.getTreeDocumentId(uri)
        val treeUri = DocumentsContract.buildDocumentUriUsingTree(uri, documentId)
        return contentResolver.query(treeUri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(0)
            } else null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST_CODE)
            }
        }


        setContent {
            SpendWiseApp(
                onSelectBackupFolder = {
                    openDocumentTree.launch(null)
                },
                onRestoreFromFolder = {
                    restoreFromFolder.launch(null)
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }

    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}