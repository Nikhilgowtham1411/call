package com.example.call.data

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Photo
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.PhoneLookup
import android.provider.ContactsContract.RawContacts
import com.example.call.data.model.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Reads and writes the phone's real contacts through the Contacts Provider.
 * All functions are safe to call without permission: they return empty / null / failure.
 */
class ContactsRepository(private val context: Context) {

    private val resolver get() = context.contentResolver

    /** Every contact that has a phone number, sorted by name, one row per contact. */
    suspend fun getContacts(): List<Contact> = withContext(Dispatchers.IO) {
        runCatching { queryContacts() }.getOrDefault(emptyList())
    }

    private fun queryContacts(): List<Contact> {
        val projection = arrayOf(
            Phone.CONTACT_ID,
            Phone.LOOKUP_KEY,
            Phone.DISPLAY_NAME,
            Phone.NUMBER,
            Phone.PHOTO_URI,
            Phone.PHOTO_THUMBNAIL_URI,
            Phone.IS_SUPER_PRIMARY
        )
        val byId = LinkedHashMap<Long, Contact>()
        val hasPrimary = HashSet<Long>()

        resolver.query(
            Phone.CONTENT_URI,
            projection,
            null,
            null,
            "${Phone.DISPLAY_NAME} COLLATE LOCALIZED ASC"
        )?.use { c ->
            val iId = c.getColumnIndexOrThrow(Phone.CONTACT_ID)
            val iKey = c.getColumnIndexOrThrow(Phone.LOOKUP_KEY)
            val iName = c.getColumnIndexOrThrow(Phone.DISPLAY_NAME)
            val iNumber = c.getColumnIndexOrThrow(Phone.NUMBER)
            val iPhoto = c.getColumnIndexOrThrow(Phone.PHOTO_URI)
            val iThumb = c.getColumnIndexOrThrow(Phone.PHOTO_THUMBNAIL_URI)
            val iPrimary = c.getColumnIndexOrThrow(Phone.IS_SUPER_PRIMARY)

            while (c.moveToNext()) {
                val id = c.getLong(iId)
                val number = c.getString(iNumber)?.trim().orEmpty()
                if (number.isEmpty()) continue
                val isPrimary = c.getInt(iPrimary) > 0
                val contact = Contact(
                    id = id,
                    lookupKey = c.getString(iKey).orEmpty(),
                    name = c.getString(iName)?.takeIf { it.isNotBlank() } ?: number,
                    number = number,
                    photoUri = c.getString(iPhoto) ?: c.getString(iThumb)
                )
                val existing = byId[id]
                if (existing == null || (isPrimary && id !in hasPrimary)) {
                    byId[id] = contact
                    if (isPrimary) hasPrimary.add(id)
                }
            }
        }
        return byId.values.toList()
    }

    /** Finds the contact that owns [number], for caller photo and name. Null if unknown. */
    suspend fun findByNumber(number: String): Contact? = withContext(Dispatchers.IO) {
        if (number.isBlank()) return@withContext null
        runCatching {
            val uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            val projection = arrayOf(
                PhoneLookup._ID,
                PhoneLookup.LOOKUP_KEY,
                PhoneLookup.DISPLAY_NAME,
                PhoneLookup.PHOTO_URI,
                PhoneLookup.PHOTO_THUMBNAIL_URI
            )
            resolver.query(uri, projection, null, null, null)?.use { c ->
                if (!c.moveToFirst()) return@use null
                Contact(
                    id = c.getLong(c.getColumnIndexOrThrow(PhoneLookup._ID)),
                    lookupKey = c.getString(c.getColumnIndexOrThrow(PhoneLookup.LOOKUP_KEY)).orEmpty(),
                    name = c.getString(c.getColumnIndexOrThrow(PhoneLookup.DISPLAY_NAME)).orEmpty(),
                    number = number,
                    photoUri = c.getString(c.getColumnIndexOrThrow(PhoneLookup.PHOTO_URI))
                        ?: c.getString(c.getColumnIndexOrThrow(PhoneLookup.PHOTO_THUMBNAIL_URI))
                )
            }
        }.getOrNull()
    }

    /** Adds a new contact straight into the phone's contacts database. */
    suspend fun addContact(name: String, number: String, photo: Uri?): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val photoBytes = photo?.let { readScaledJpeg(it) }
                val ops = ArrayList<ContentProviderOperation>()
                ops += ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                    .withValue(RawContacts.ACCOUNT_TYPE, null)
                    .withValue(RawContacts.ACCOUNT_NAME, null)
                    .build()
                ops += ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(StructuredName.DISPLAY_NAME, name.trim())
                    .build()
                ops += ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                    .withValue(Phone.NUMBER, number.trim())
                    .withValue(Phone.TYPE, Phone.TYPE_MOBILE)
                    .build()
                if (photoBytes != null) {
                    ops += ContentProviderOperation.newInsert(Data.CONTENT_URI)
                        .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                        .withValue(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE)
                        .withValue(Photo.PHOTO, photoBytes)
                        .build()
                }
                resolver.applyBatch(ContactsContract.AUTHORITY, ops)
                Unit
            }
        }

    /** Updates name, number and (optionally) photo of an existing contact. */
    suspend fun updateContact(contactId: Long, name: String, number: String, photo: Uri?): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val rawId = firstRawContactId(contactId)
                    ?: error("Contact not found")
                upsertData(
                    rawId, StructuredName.CONTENT_ITEM_TYPE,
                    ContentValues().apply { put(StructuredName.DISPLAY_NAME, name.trim()) }
                )
                upsertData(
                    rawId, Phone.CONTENT_ITEM_TYPE,
                    ContentValues().apply {
                        put(Phone.NUMBER, number.trim())
                        put(Phone.TYPE, Phone.TYPE_MOBILE)
                    }
                )
                val photoBytes = photo?.let { readScaledJpeg(it) }
                if (photoBytes != null) {
                    upsertData(
                        rawId, Photo.CONTENT_ITEM_TYPE,
                        ContentValues().apply { put(Photo.PHOTO, photoBytes) }
                    )
                }
            }
        }

    /** Deletes a contact (all of its raw contacts) from the phone. */
    suspend fun deleteContact(lookupKey: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey)
            val deleted = resolver.delete(uri, null, null)
            if (deleted <= 0) error("Nothing was deleted")
        }
    }

    private fun firstRawContactId(contactId: Long): Long? =
        resolver.query(
            RawContacts.CONTENT_URI,
            arrayOf(RawContacts._ID),
            "${RawContacts.CONTACT_ID}=? AND ${RawContacts.DELETED}=0",
            arrayOf(contactId.toString()),
            "${RawContacts._ID} ASC"
        )?.use { if (it.moveToFirst()) it.getLong(0) else null }

    /** Updates the first data row of [mime] for the raw contact, or inserts one if none exists. */
    private fun upsertData(rawId: Long, mime: String, values: ContentValues) {
        val rowId = resolver.query(
            Data.CONTENT_URI,
            arrayOf(Data._ID),
            "${Data.RAW_CONTACT_ID}=? AND ${Data.MIMETYPE}=?",
            arrayOf(rawId.toString(), mime),
            "${Data._ID} ASC"
        )?.use { if (it.moveToFirst()) it.getLong(0) else null }

        if (rowId != null) {
            resolver.update(Data.CONTENT_URI, values, "${Data._ID}=?", arrayOf(rowId.toString()))
        } else {
            val insert = ContentValues(values).apply {
                put(Data.RAW_CONTACT_ID, rawId)
                put(Data.MIMETYPE, mime)
            }
            resolver.insert(Data.CONTENT_URI, insert) ?: error("Could not save contact data")
        }
    }

    /** Reads an image from the gallery and shrinks it so it fits in a contact photo row. */
    private fun readScaledJpeg(uri: Uri, maxSide: Int = 720): ByteArray? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / sample > maxSide * 2 || bounds.outHeight / sample > maxSide * 2) sample *= 2

        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null

        val scale = maxSide.toFloat() / maxOf(decoded.width, decoded.height)
        val bitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else decoded

        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.toByteArray()
        }
    }
}
