package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.DriverAccountEntity
import com.example.data.local.entity.RideEntity
import com.example.domain.FinanceSummaryCalculator
import com.example.domain.RideCalculator
import com.example.ui.viewmodel.DailyReportGroup
import java.io.File

data class StorageEntry(
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModifiedMillis: Long,
    val documentId: String? = null,
    val documentUri: Uri? = null,
    val localFile: File? = null,
    val mimeType: String = ""
)

object LocalStorageManager {

    const val MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    const val MIME_PDF = "application/pdf"
    const val DATA_FOLDER_NAME = "data"

    fun ensureAppDataFolder(context: Context): File {
        val externalBase = runCatching { context.getExternalFilesDir(null) }.getOrNull()
        val candidate = File(externalBase ?: context.filesDir, DATA_FOLDER_NAME)
        if (candidate.exists() || candidate.mkdirs()) {
            return candidate
        }
        val internalFallback = File(context.filesDir, DATA_FOLDER_NAME)
        internalFallback.mkdirs()
        return internalFallback
    }

    fun getAppDataFolderDisplayPath(context: Context): String {
        val dir = ensureAppDataFolder(context)
        val path = dir.absolutePath
        val marker = "/Android/data/"
        val idx = path.indexOf(marker)
        return if (idx >= 0) path.substring(idx + 1) else path
    }

    fun saveBytesToAppDataFolder(
        context: Context,
        fileName: String,
        bytes: ByteArray
    ): Result<File> {
        return runCatching {
            val dir = ensureAppDataFolder(context)
            val safeName = sanitizeFileName(fileName)
            val target = File(dir, safeName)
            target.writeBytes(bytes)
            target
        }
    }

    fun syncDriverDataBackup(
        context: Context,
        driver: DriverAccountEntity,
        settings: AppSettingsEntity,
        rides: List<RideEntity>
    ): Result<File> {
        return runCatching {
            val groups = rides
                .groupBy { RideCalculator.computeShiftDateIso(it.rideDateMillis, settings) }
                .entries
                .sortedByDescending { it.key }
                .map { (shiftIso, dayRides) ->
                    DailyReportGroup(
                        dateIso = shiftIso,
                        summary = FinanceSummaryCalculator.aggregateRides(dayRides),
                        rides = dayRides.sortedByDescending { it.rideDateMillis }
                    )
                }
            val totalSummary = FinanceSummaryCalculator.aggregateRides(rides)
            val bytes = ReportExporter.buildXlsxBytes(
                driver = driver,
                settings = settings,
                periodLabel = "All Local Data (${driver.driverId})",
                summary = totalSummary,
                dailyGroups = groups
            )
            saveBytesToAppDataFolder(
                context = context,
                fileName = "RideLedger_${driver.driverId}_data.xlsx",
                bytes = bytes
            ).getOrThrow()
        }
    }

    fun persistTreePermission(context: Context, treeUri: Uri): Result<String> {
        return runCatching {
            val rwFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(treeUri, rwFlags)
            } catch (_: SecurityException) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
            treeUri.toString()
        }
    }

    fun hasValidTreePermission(context: Context, folderUriString: String?): Boolean {
        if (folderUriString.isNullOrBlank()) return false
        val treeUri = runCatching { Uri.parse(folderUriString) }.getOrNull() ?: return false
        val hasPersisted = runCatching {
            context.contentResolver.persistedUriPermissions.any { perm ->
                perm.uri == treeUri && perm.isReadPermission
            }
        }.getOrDefault(false)
        if (!hasPersisted) return false

        return runCatching {
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)
            context.contentResolver.query(
                docUri,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                null,
                null,
                null
            )?.use { cursor -> cursor.moveToFirst() } ?: false
        }.getOrDefault(false)
    }

    fun hasWriteTreePermission(context: Context, folderUriString: String?): Boolean {
        if (!hasValidTreePermission(context, folderUriString)) return false
        val treeUri = runCatching { Uri.parse(folderUriString) }.getOrNull() ?: return false
        return runCatching {
            context.contentResolver.persistedUriPermissions.any { perm ->
                perm.uri == treeUri && perm.isReadPermission && perm.isWritePermission
            }
        }.getOrDefault(false)
    }

    fun getTreeFolderDisplayName(context: Context, folderUriString: String?): String {
        if (folderUriString.isNullOrBlank()) return "Not selected"
        val treeUri = runCatching { Uri.parse(folderUriString) }.getOrNull() ?: return "Selected Folder"
        val queried = runCatching {
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)
            context.contentResolver.query(
                docUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
        if (!queried.isNullOrBlank()) return queried
        return treeUri.lastPathSegment?.substringAfterLast(':')?.ifBlank { "Selected Folder" }
            ?: "Selected Folder"
    }

    fun writeBytesToTreeFolder(
        context: Context,
        folderUriString: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): Result<String> {
        return runCatching {
            val treeUri = Uri.parse(folderUriString)
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)
            val safeName = sanitizeFileName(fileName)

            val createdUri = try {
                DocumentsContract.createDocument(context.contentResolver, parentUri, mimeType, safeName)
            } catch (_: Exception) {
                DocumentsContract.createDocument(
                    context.contentResolver,
                    parentUri,
                    "application/octet-stream",
                    safeName
                )
            } ?: DocumentsContract.createDocument(
                context.contentResolver,
                parentUri,
                "application/octet-stream",
                safeName
            ) ?: error("Unable to create file inside destination folder. Check folder write permission.")

            val outputStream = runCatching {
                context.contentResolver.openOutputStream(createdUri, "w")
            }.getOrNull() ?: context.contentResolver.openOutputStream(createdUri)
                ?: error("Unable to open output stream for destination file.")

            outputStream.use { out ->
                out.write(bytes)
                out.flush()
            }
            safeName
        }
    }

    fun listTreeChildren(
        context: Context,
        folderUriString: String,
        folderDocumentId: String? = null
    ): Result<List<StorageEntry>> {
        return runCatching {
            val treeUri = Uri.parse(folderUriString)
            val parentId = folderDocumentId ?: DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )

            val entries = mutableListOf<StorageEntry>()
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                val modIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idIdx) ?: continue
                    val name = cursor.getString(nameIdx) ?: "Unnamed"
                    val mime = cursor.getString(mimeIdx) ?: ""
                    val size = if (cursor.isNull(sizeIdx)) 0L else cursor.getLong(sizeIdx)
                    val modified = if (cursor.isNull(modIdx)) 0L else cursor.getLong(modIdx)
                    val isDir = mime == DocumentsContract.Document.MIME_TYPE_DIR
                    val isExcel = name.endsWith(".xlsx", ignoreCase = true) ||
                        mime.equals(MIME_XLSX, ignoreCase = true)
                    if (isDir || isExcel) {
                        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        entries.add(
                            StorageEntry(
                                name = name,
                                isDirectory = isDir,
                                sizeBytes = size,
                                lastModifiedMillis = modified,
                                documentId = docId,
                                documentUri = docUri,
                                mimeType = mime
                            )
                        )
                    }
                }
            }
            entries.sortedWith(
                compareByDescending<StorageEntry> { it.isDirectory }
                    .thenByDescending { it.lastModifiedMillis }
                    .thenBy { it.name.lowercase() }
            )
        }
    }

    fun listLocalFolderChildren(dir: File): List<StorageEntry> {
        val children = dir.listFiles()?.toList().orEmpty()
        return children
            .filter { file ->
                file.isDirectory || file.name.endsWith(".xlsx", ignoreCase = true)
            }
            .map { file ->
                StorageEntry(
                    name = file.name,
                    isDirectory = file.isDirectory,
                    sizeBytes = if (file.isDirectory) 0L else file.length(),
                    lastModifiedMillis = file.lastModified(),
                    localFile = file
                )
            }
            .sortedWith(
                compareByDescending<StorageEntry> { it.isDirectory }
                    .thenByDescending { it.lastModifiedMillis }
                    .thenBy { it.name.lowercase() }
            )
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        if (bytes < 1024L) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024.0) return String.format(java.util.Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(java.util.Locale.US, "%.1f MB", mb)
    }

    private fun sanitizeFileName(raw: String): String {
        return raw.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }
}
