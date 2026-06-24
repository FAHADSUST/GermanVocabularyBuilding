package com.studio71.germanlinia2_b2.data.sync

import android.content.Context
import androidx.room.withTransaction
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.studio71.germanlinia2_b2.data.local.AppDatabase
import com.studio71.germanlinia2_b2.data.local.ProgressEntity
import com.studio71.germanlinia2_b2.data.local.WordCommentEntity
import com.studio71.germanlinia2_b2.data.local.WordMarkEntity
import com.studio71.germanlinia2_b2.data.settings.SettingsStore
import com.studio71.germanlinia2_b2.data.settings.TtsHistoryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await

data class CloudSyncUiState(
    val configured: Boolean = false,
    val isSignedIn: Boolean = false,
    val email: String? = null,
    val uid: String? = null,
    val isBusy: Boolean = false,
    val statusMessage: String = "Firebase ist nicht konfiguriert.",
    val errorMessage: String? = null,
    val lastSyncedAtEpochMs: Long? = null
)

/**
 * Read-only snapshot of the current cloud/Firebase environment so the user can
 * confirm everything is configured correctly. This is independent of the noisy
 * "Failed to get service from broker" log line, which Play Services emits
 * internally and which does not stop Auth or Firestore.
 */
data class CloudDiagnostics(
    val firebaseConfigured: Boolean,
    val projectId: String?,
    val applicationId: String?,
    val packageName: String,
    val firestoreDatabaseId: String,
    val playServicesOk: Boolean,
    val playServicesStatusCode: Int,
    val playServicesStatusText: String,
    val isSignedIn: Boolean,
    val email: String?
)

/**
 * Manual cloud sync manager for Firebase Spark:
 * - email/password login
 * - explicit "Sync now"
 * - snapshot sync of progress, marks, comments and TTS history
 */
class CloudSyncManager(
    context: Context,
    private val settingsStore: SettingsStore
) {
    private val appContext = context.applicationContext
    private val db = AppDatabase.get(appContext)
    private val progressDao = db.progressDao()
    private val wordMarkDao = db.wordMarkDao()
    private val wordCommentDao = db.wordCommentDao()
    private val googleApiAvailability = GoogleApiAvailability.getInstance()

    private val firebaseConfigured = FirebaseApp.getApps(appContext).isNotEmpty()
    private val auth: FirebaseAuth? = if (firebaseConfigured) FirebaseAuth.getInstance() else null
    private val firestore: FirebaseFirestore? = if (firebaseConfigured) {
        // Use the configured Firestore database. The "(default)" database of a
        // project may be locked to Datastore Mode (incompatible with this SDK);
        // in that case create a Native-mode named database and set its id here.
        if (FIRESTORE_DATABASE_ID == DEFAULT_FIRESTORE_DATABASE_ID) FirebaseFirestore.getInstance()
        else FirebaseFirestore.getInstance(FIRESTORE_DATABASE_ID)
    } else null

    private val _uiState = MutableStateFlow(
        CloudSyncUiState(
            configured = firebaseConfigured,
            statusMessage = when {
                !firebaseConfigured -> "Firebase ist nicht konfiguriert (google-services.json fehlt oder ist ungultig)."
                !hasUsablePlayServices() -> "Google Play-Dienste fehlen oder sind veraltet."
                else -> "Bitte anmelden, dann manuell synchronisieren."
            },
            lastSyncedAtEpochMs = SyncStateTracker.lastSyncedAt(appContext).takeIf { it > 0L }
        )
    )
    val uiState: StateFlow<CloudSyncUiState> = _uiState.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { updateAuthState(it.currentUser?.email, it.currentUser?.uid) }

    init {
        auth?.addAuthStateListener(authListener)
        updateAuthState(auth?.currentUser?.email, auth?.currentUser?.uid)
    }

    suspend fun signIn(email: String, password: String): Result<Unit> =
        runAction("Anmeldung lauft...") {
            ensureGooglePlayServicesForAuth()
            val cleanEmail = email.trim()
            require(cleanEmail.isNotEmpty()) { "E-Mail eingeben." }
            require(password.isNotEmpty()) { "Passwort eingeben." }
            auth!!.signInWithEmailAndPassword(cleanEmail, password).await()
            updateAuthState(auth.currentUser?.email, auth.currentUser?.uid)
            _uiState.update { it.copy(statusMessage = "Angemeldet als ${auth.currentUser?.email.orEmpty()}") }
        }

    suspend fun register(email: String, password: String): Result<Unit> =
        runAction("Konto wird erstellt...") {
            ensureGooglePlayServicesForAuth()
            val cleanEmail = email.trim()
            require(cleanEmail.isNotEmpty()) { "E-Mail eingeben." }
            require(password.length >= 6) { "Passwort muss mindestens 6 Zeichen haben." }
            auth!!.createUserWithEmailAndPassword(cleanEmail, password).await()
            updateAuthState(auth.currentUser?.email, auth.currentUser?.uid)
            _uiState.update { it.copy(statusMessage = "Konto erstellt: ${auth.currentUser?.email.orEmpty()}") }
        }

    fun signOut() {
        if (!firebaseConfigured) return
        auth?.signOut()
        updateAuthState(null, null)
        _uiState.update { it.copy(statusMessage = "Abgemeldet.") }
    }

    /** Snapshot of current Firebase/Play-Services state for the diagnostics panel. */
    fun diagnostics(): CloudDiagnostics {
        val options = runCatching { FirebaseApp.getInstance().options }.getOrNull()
        val statusCode = googleApiAvailability.isGooglePlayServicesAvailable(appContext)
        return CloudDiagnostics(
            firebaseConfigured = firebaseConfigured,
            projectId = options?.projectId,
            applicationId = options?.applicationId,
            packageName = appContext.packageName,
            firestoreDatabaseId = FIRESTORE_DATABASE_ID,
            playServicesOk = statusCode == ConnectionResult.SUCCESS,
            playServicesStatusCode = statusCode,
            playServicesStatusText = googleApiAvailability.getErrorString(statusCode),
            isSignedIn = !auth?.currentUser?.uid.isNullOrBlank(),
            email = auth?.currentUser?.email
        )
    }

    /**
     * Lightweight end-to-end check that proves Firebase actually works despite the
     * benign broker log: verifies config, Play Services, sign-in, then does a real
     * Firestore read on the user's own sync document.
     */
    suspend fun testConnection(): Result<Unit> = runAction("Verbindung wird getestet...") {
        ensureGooglePlayServicesForAuth()
        val uid = auth?.currentUser?.uid ?: error("Bitte zuerst anmelden.")
        // A real network round-trip to Firestore on the user's own document.
        firestore!!
            .collection(USERS_COLLECTION)
            .document(uid)
            .collection(SYNC_COLLECTION)
            .document(SYNC_DOC)
            .get()
            .await()
        _uiState.update {
            it.copy(statusMessage = "Verbindung OK: Firebase Auth und Firestore funktionieren.")
        }
    }

    suspend fun syncNow(): Result<Unit> = runAction("Synchronisierung lauft...") {
        val uid = auth?.currentUser?.uid ?: error("Bitte zuerst anmelden.")

        val local = readLocalSnapshot()
        val remote = readRemoteSnapshot(uid)

        val lastSyncedAt = SyncStateTracker.lastSyncedAt(appContext)
        val localDirty = SyncStateTracker.localMutatedAt(appContext) > lastSyncedAt

        val finalSnapshot = when {
            remote == null -> local
            remote.updatedAtEpochMs > lastSyncedAt && !localDirty -> remote.toLocal()
            remote.updatedAtEpochMs > lastSyncedAt && localDirty -> mergeSnapshots(local, remote.toLocal())
            else -> local
        }

        replaceLocalSnapshot(finalSnapshot)

        val now = System.currentTimeMillis()
        writeRemoteSnapshot(uid, RemoteSnapshot.fromLocal(finalSnapshot, now))
        SyncStateTracker.markSynced(appContext, now)

        _uiState.update {
            it.copy(
                statusMessage = "Sync fertig: ${finalSnapshot.progress.size} Fortschritt, " +
                    "${finalSnapshot.marks.size} Markierungen, ${finalSnapshot.comments.size} Kommentare, " +
                    "${finalSnapshot.ttsHistory.size} TTS-Verlauf.",
                errorMessage = null,
                lastSyncedAtEpochMs = now
            )
        }
    }

    private suspend fun runAction(message: String, block: suspend () -> Unit): Result<Unit> {
        if (!firebaseConfigured) {
            return Result.failure(IllegalStateException("Firebase ist nicht konfiguriert."))
        }

        _uiState.update { it.copy(isBusy = true, errorMessage = null, statusMessage = message) }
        return runCatching { block() }
            .onSuccess {
                _uiState.update { state -> state.copy(isBusy = false, errorMessage = null) }
            }
            .onFailure { error ->
                _uiState.update { state ->
                    state.copy(
                        isBusy = false,
                        errorMessage = friendlyMessage(error),
                        statusMessage = "Fehler bei der Cloud-Synchronisierung."
                    )
                }
            }
    }

    private fun updateAuthState(email: String?, uid: String?) {
        _uiState.update { current ->
            val signedIn = !uid.isNullOrBlank()
            current.copy(
                isSignedIn = signedIn,
                email = email,
                uid = uid,
                errorMessage = null,
                statusMessage = when {
                    !firebaseConfigured -> current.statusMessage
                    signedIn -> "Angemeldet als ${email ?: uid}"
                    else -> "Nicht angemeldet."
                }
            )
        }
    }

    private suspend fun readLocalSnapshot(): LocalSnapshot {
        return LocalSnapshot(
            progress = progressDao.getAll(),
            marks = wordMarkDao.getAll(),
            comments = wordCommentDao.getAll(),
            ttsHistory = settingsStore.state.value.ttsHistory
        )
    }

    private suspend fun replaceLocalSnapshot(snapshot: LocalSnapshot) {
        db.withTransaction {
            progressDao.deleteAll()
            wordMarkDao.deleteAll()
            wordCommentDao.deleteAll()

            if (snapshot.progress.isNotEmpty()) progressDao.upsertAll(snapshot.progress)
            if (snapshot.marks.isNotEmpty()) wordMarkDao.upsertAll(snapshot.marks)
            if (snapshot.comments.isNotEmpty()) wordCommentDao.upsertAll(snapshot.comments)
        }
        settingsStore.replaceTtsHistoryFromSync(snapshot.ttsHistory)
    }

    private suspend fun readRemoteSnapshot(uid: String): RemoteSnapshot? {
        val doc = firestore!!
            .collection(USERS_COLLECTION)
            .document(uid)
            .collection(SYNC_COLLECTION)
            .document(SYNC_DOC)
            .get()
            .await()

        if (!doc.exists()) return null
        return RemoteSnapshot.fromMap(doc.data.orEmpty())
    }

    private suspend fun writeRemoteSnapshot(uid: String, snapshot: RemoteSnapshot) {
        firestore!!
            .collection(USERS_COLLECTION)
            .document(uid)
            .collection(SYNC_COLLECTION)
            .document(SYNC_DOC)
            .set(snapshot.toMap(), SetOptions.merge())
            .await()
    }

    private fun mergeSnapshots(local: LocalSnapshot, remote: LocalSnapshot): LocalSnapshot {
        val mergedProgress = buildMap {
            val remoteMap = remote.progress.associateBy { it.wordId }
            val localMap = local.progress.associateBy { it.wordId }
            (remoteMap.keys + localMap.keys).forEach { wordId ->
                val localValue = localMap[wordId]
                val remoteValue = remoteMap[wordId]
                when {
                    localValue == null -> put(wordId, remoteValue!!)
                    remoteValue == null -> put(wordId, localValue)
                    else -> put(wordId, chooseProgress(localValue, remoteValue))
                }
            }
        }.values.toList()

        val mergedMarks = buildMap {
            remote.marks.forEach { put(it.wordId, it) }
            local.marks.forEach { put(it.wordId, it) }
        }.values.toList()

        val mergedComments = buildMap {
            val remoteMap = remote.comments.associateBy { it.wordId }
            val localMap = local.comments.associateBy { it.wordId }
            (remoteMap.keys + localMap.keys).forEach { wordId ->
                val l = localMap[wordId]
                val r = remoteMap[wordId]
                when {
                    l == null -> put(wordId, r!!)
                    r == null -> put(wordId, l)
                    l.updatedAtEpochMs >= r.updatedAtEpochMs -> put(wordId, l)
                    else -> put(wordId, r)
                }
            }
        }.values.toList()

        val mergedHistory = (local.ttsHistory + remote.ttsHistory)
            .associateBy { it.id }
            .values
            .sortedByDescending { it.playedAtEpochMs }
            .take(TTS_HISTORY_LIMIT)

        return LocalSnapshot(
            progress = mergedProgress,
            marks = mergedMarks,
            comments = mergedComments,
            ttsHistory = mergedHistory
        )
    }

    private fun chooseProgress(local: ProgressEntity, remote: ProgressEntity): ProgressEntity {
        val localStamp = local.lastReviewed ?: local.firstLearned
        val remoteStamp = remote.lastReviewed ?: remote.firstLearned

        return when {
            localStamp != remoteStamp -> if (localStamp > remoteStamp) local else remote
            local.intervalIndex != remote.intervalIndex -> if (local.intervalIndex > remote.intervalIndex) local else remote
            local.box != remote.box -> if (local.box > remote.box) local else remote
            local.nextDue != remote.nextDue -> if (local.nextDue > remote.nextDue) local else remote
            else -> local
        }
    }

    private fun hasUsablePlayServices(): Boolean {
        return googleApiAvailability.isGooglePlayServicesAvailable(appContext) == ConnectionResult.SUCCESS
    }

    private fun ensureGooglePlayServicesForAuth() {
        if (hasUsablePlayServices()) return
        throw IllegalStateException(
            "Google Play-Dienste fehlen oder sind veraltet. Aktualisiere Play Store und Play-Dienste und starte die App neu."
        )
    }

    private fun friendlyMessage(error: Throwable): String = when (error) {
        is IllegalArgumentException -> error.message.orEmpty()
        is IllegalStateException -> error.message.orEmpty()
        is SecurityException -> {
            val raw = error.message.orEmpty()
            if (raw.contains("Unknown calling package name 'com.google.android.gms'")) {
                "Google Play-Service Broker Fehler. Prufe google-services.json (Package com.studio71.germanlinia2_b2), deinstalliere die App, installiere neu und aktualisiere Play-Dienste."
            } else {
                raw.ifBlank { "Sicherheitsfehler beim Zugriff auf Firebase." }
            }
        }
        is FirebaseAuthInvalidUserException -> "Dieses Konto existiert nicht."
        is FirebaseAuthInvalidCredentialsException -> "E-Mail oder Passwort ist ungultig."
        is FirebaseAuthUserCollisionException -> "Dieses Konto ist bereits registriert."
        is FirebaseFirestoreException -> firestoreMessage(error)
        else -> {
            val raw = error.message.orEmpty()
            when {
                raw.contains("Datastore Mode") ->
                    "Die Firestore-Datenbank dieses Projekts ist im Datastore-Modus und ist nicht mit der App kompatibel. Erstelle eine Firestore-Datenbank im Native-Modus (neues Firebase-Projekt oder benannte Native-DB) und aktualisiere google-services.json."
                raw.contains("client is offline", ignoreCase = true) ->
                    "Firestore ist offline. Meist fehlt eine Native-Mode-Firestore-Datenbank oder das Internet ist nicht erreichbar."
                else -> error.message ?: "Unbekannter Fehler."
            }
        }
    }

    private fun firestoreMessage(error: FirebaseFirestoreException): String {
        val raw = error.message.orEmpty()
        return when {
            raw.contains("Datastore Mode") ->
                "Die Firestore-Datenbank dieses Projekts ist im Datastore-Modus und ist nicht mit der App kompatibel. Erstelle eine Firestore-Datenbank im Native-Modus (neues Firebase-Projekt oder benannte Native-DB) und aktualisiere google-services.json."
            error.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Firestore ist nicht erreichbar (offline). Prufe Internet und ob die Firestore-Datenbank im Native-Modus existiert."
            error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Zugriff verweigert. Prufe die Firestore-Sicherheitsregeln fur users/{uid}/sync/main."
            else -> raw.ifBlank { "Firestore-Fehler (${error.code})." }
        }
    }

    private data class LocalSnapshot(
        val progress: List<ProgressEntity>,
        val marks: List<WordMarkEntity>,
        val comments: List<WordCommentEntity>,
        val ttsHistory: List<TtsHistoryEntry>
    )

    private data class RemoteSnapshot(
        val updatedAtEpochMs: Long,
        val progress: Map<String, ProgressEntity>,
        val marks: Map<String, WordMarkEntity>,
        val comments: Map<String, WordCommentEntity>,
        val ttsHistory: List<TtsHistoryEntry>
    ) {
        fun toLocal(): LocalSnapshot = LocalSnapshot(
            progress = progress.values.toList(),
            marks = marks.values.toList(),
            comments = comments.values.toList(),
            ttsHistory = ttsHistory.sortedByDescending { it.playedAtEpochMs }.take(TTS_HISTORY_LIMIT)
        )

        fun toMap(): Map<String, Any> = mapOf(
            "updatedAtEpochMs" to updatedAtEpochMs,
            "progress" to progress.mapValues { (_, p) ->
                buildMap {
                    put("wordId", p.wordId)
                    put("firstLearned", p.firstLearned)
                    put("nextDue", p.nextDue)
                    put("intervalIndex", p.intervalIndex)
                    put("box", p.box)
                    p.lastReviewed?.let { put("lastReviewed", it) }
                }
            },
            "marks" to marks.mapValues { (_, m) -> mapOf("wordId" to m.wordId, "marker" to m.marker) },
            "comments" to comments.mapValues { (_, c) ->
                mapOf(
                    "wordId" to c.wordId,
                    "comment" to c.comment,
                    "updatedAtEpochMs" to c.updatedAtEpochMs
                )
            },
            "ttsHistory" to ttsHistory.map { h ->
                mapOf(
                    "id" to h.id,
                    "playedAtEpochMs" to h.playedAtEpochMs,
                    "level" to h.level,
                    "book" to h.book,
                    "chapter" to h.chapter,
                    "pos" to h.pos,
                    "grammarGroup" to h.grammarGroup,
                    "markerValue" to h.markerValue,
                    "query" to h.query,
                    "sortModeKey" to h.sortModeKey,
                    "startIndex" to h.startIndex,
                    "lastIndex" to h.lastIndex,
                    "totalCount" to h.totalCount
                )
            }
        )

        companion object {
            fun fromLocal(local: LocalSnapshot, updatedAtEpochMs: Long): RemoteSnapshot =
                RemoteSnapshot(
                    updatedAtEpochMs = updatedAtEpochMs,
                    progress = local.progress.associateBy { it.wordId },
                    marks = local.marks.associateBy { it.wordId },
                    comments = local.comments.associateBy { it.wordId },
                    ttsHistory = local.ttsHistory.sortedByDescending { it.playedAtEpochMs }.take(TTS_HISTORY_LIMIT)
                )

            fun fromMap(data: Map<String, Any>): RemoteSnapshot {
                val progressMap = mutableMapOf<String, ProgressEntity>()
                val marksMap = mutableMapOf<String, WordMarkEntity>()
                val commentsMap = mutableMapOf<String, WordCommentEntity>()

                data.mapValue("progress").forEach { (wordId, raw) ->
                    val map = raw as? Map<*, *> ?: return@forEach
                    val parsedWordId = map.string("wordId") ?: wordId
                    progressMap[parsedWordId] = ProgressEntity(
                        wordId = parsedWordId,
                        firstLearned = map.long("firstLearned") ?: 0L,
                        lastReviewed = map.long("lastReviewed"),
                        nextDue = map.long("nextDue") ?: 0L,
                        intervalIndex = map.int("intervalIndex") ?: 0,
                        box = map.int("box") ?: 1
                    )
                }

                data.mapValue("marks").forEach { (wordId, raw) ->
                    val map = raw as? Map<*, *> ?: return@forEach
                    val parsedWordId = map.string("wordId") ?: wordId
                    marksMap[parsedWordId] = WordMarkEntity(
                        wordId = parsedWordId,
                        marker = map.int("marker") ?: 0
                    )
                }

                data.mapValue("comments").forEach { (wordId, raw) ->
                    val map = raw as? Map<*, *> ?: return@forEach
                    val parsedWordId = map.string("wordId") ?: wordId
                    commentsMap[parsedWordId] = WordCommentEntity(
                        wordId = parsedWordId,
                        comment = map.string("comment").orEmpty(),
                        updatedAtEpochMs = map.long("updatedAtEpochMs") ?: 0L
                    )
                }

                val history = data.listValue("ttsHistory").mapNotNull { raw ->
                    val map = raw as? Map<*, *> ?: return@mapNotNull null
                    val id = map.long("id") ?: return@mapNotNull null
                    TtsHistoryEntry(
                        id = id,
                        playedAtEpochMs = map.long("playedAtEpochMs") ?: 0L,
                        level = map.string("level"),
                        book = map.string("book"),
                        chapter = map.string("chapter"),
                        pos = map.string("pos"),
                        grammarGroup = map.string("grammarGroup"),
                        markerValue = map.int("markerValue"),
                        query = map.string("query").orEmpty(),
                        sortModeKey = map.string("sortModeKey").orEmpty(),
                        startIndex = map.int("startIndex") ?: 0,
                        lastIndex = map.int("lastIndex") ?: 0,
                        totalCount = map.int("totalCount") ?: 0
                    )
                }.sortedByDescending { it.playedAtEpochMs }
                    .take(TTS_HISTORY_LIMIT)

                return RemoteSnapshot(
                    updatedAtEpochMs = data.long("updatedAtEpochMs") ?: 0L,
                    progress = progressMap,
                    marks = marksMap,
                    comments = commentsMap,
                    ttsHistory = history
                )
            }
        }
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val SYNC_COLLECTION = "sync"
        const val SYNC_DOC = "main"
        const val TTS_HISTORY_LIMIT = 20

        // Firestore database id. Keep "(default)" if the project's default DB is
        // Native mode. If the default DB is locked to Datastore Mode, create a
        // Native-mode named database in the same project and put its id here.
        const val DEFAULT_FIRESTORE_DATABASE_ID = "(default)"
        const val FIRESTORE_DATABASE_ID = "germansync"
    }
}

private fun Map<*, *>.mapValue(key: String): Map<String, Any> {
    val raw = this[key] as? Map<*, *> ?: return emptyMap()
    return raw.entries.mapNotNull { (k, v) ->
        val stringKey = k as? String ?: return@mapNotNull null
        stringKey to (v ?: return@mapNotNull null)
    }.toMap()
}

private fun Map<*, *>.listValue(key: String): List<Any> {
    val raw = this[key] as? List<*> ?: return emptyList()
    return raw.filterNotNull()
}


private fun Map<*, *>.long(key: String): Long? = when (val value = this[key]) {
    is Number -> value.toLong()
    is String -> value.toLongOrNull()
    else -> null
}

private fun Map<*, *>.int(key: String): Int? = when (val value = this[key]) {
    is Number -> value.toInt()
    is String -> value.toIntOrNull()
    else -> null
}

private fun Map<*, *>.string(key: String): String? = (this[key] as? String)?.ifBlank { null }

