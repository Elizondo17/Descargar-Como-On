package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PhoneContactEntry(
    val id: String,
    val name: String,
    val phone: String
)

object DeviceContactsHelper {

    fun hasReadContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun loadDeviceContacts(context: Context): List<PhoneContactEntry> = withContext(Dispatchers.IO) {
        if (!hasReadContactsPermission(context)) return@withContext emptyList()
        val seenKeys = hashSetOf<String>()
        val results = mutableListOf<PhoneContactEntry>()

        try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone._ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                val phoneIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext()) {
                    val rawName = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty().trim() else ""
                    val rawPhone = if (phoneIdx >= 0) cursor.getString(phoneIdx).orEmpty().trim() else ""
                    if (rawPhone.isBlank()) continue
                    val normalizedDigits = rawPhone.filter { it.isDigit() || it == '+' }
                    val cleanName = rawName.ifBlank { rawPhone }
                    val dedupKey = "${cleanName.lowercase()}|$normalizedDigits"
                    if (seenKeys.add(dedupKey)) {
                        val rowId = if (idIdx >= 0) cursor.getString(idIdx).orEmpty() else results.size.toString()
                        results += PhoneContactEntry(
                            id = rowId.ifBlank { "c_${results.size}" },
                            name = cleanName,
                            phone = rawPhone
                        )
                    }
                }
            }
        } catch (_: SecurityException) {
            return@withContext emptyList()
        } catch (_: Exception) {
            return@withContext emptyList()
        }

        results
    }
}
