package com.example.data.firestore

import android.content.Context
import com.example.R
import com.example.data.ExchangeExecutionResult
import com.example.data.ExchangeRecordEntity
import com.example.data.HousieDataRepository
import com.example.data.WalletActivityEntity
import com.example.data.WalletStateEntity
import com.example.domain.ChatMessage
import com.example.domain.ChatThread
import com.example.domain.ExchangeCalculator
import com.example.domain.FounderAuditEntry
import com.example.domain.FounderConfig
import com.example.domain.ManagedUserAccount
import com.example.domain.PlatformExchangeLedgerItem
import com.example.domain.PlatformSettings
import com.example.domain.ValidationError
import com.example.domain.VirtualIdGenerator
import com.example.domain.VirtualPlayerLookup
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreHousieRepository(
    private val db: FirebaseFirestore,
    private val currentUserId: String,
    private val isFounderSession: Boolean,
    private val auth: FirebaseAuth = Firebase.auth
) : HousieDataRepository {

    constructor(
        context: Context,
        currentUserId: String,
        isFounderSession: Boolean
    ) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        currentUserId = currentUserId,
        isFounderSession = isFounderSession
    )

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        check(uid == currentUserId) { "Signed-in user changed; refusing cross-user Firestore operation." }
        return uid
    }

    private fun unwrapFirestoreException(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    override val currentUserAccount: Flow<ManagedUserAccount?> =
        if (currentUserId.isBlank()) {
            flowOf(null)
        } else {
            val path = "users/$currentUserId"
            db.collection("users").document(currentUserId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        UserAccountDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )?.toDomain()
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreException(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(fsErr ?: error, OperationType.GET, path)
                    }
                    throw error
                }
        }

    override val platformSettings: Flow<PlatformSettings> =
        if (currentUserId.isBlank()) {
            flowOf(PlatformSettings())
        } else {
            val path = "app_config/platform"
            db.collection("app_config").document("platform")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            PlatformConfigDoc::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )?.toDomain() ?: PlatformSettings()
                    } else {
                        PlatformSettings()
                    }
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreException(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(fsErr ?: error, OperationType.GET, path)
                    }
                    emit(PlatformSettings())
                }
        }

    override val globalExchangeLedger: Flow<List<PlatformExchangeLedgerItem>> =
        if (!isFounderSession || currentUserId.isBlank()) {
            flowOf(emptyList())
        } else {
            val path = "exchange_ledger"
            db.collection("exchange_ledger")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PlatformExchangeLedgerDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).map { it.toDomain() }
                        .sortedByDescending { it.timestampMillis }
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreException(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        }

    override val allUsersForFounder: Flow<List<ManagedUserAccount>> =
        if (!isFounderSession || currentUserId.isBlank()) {
            flowOf(emptyList())
        } else {
            val path = "users"
            db.collection("users")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        UserAccountDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).map { it.toDomain() }
                        .sortedWith(compareByDescending<ManagedUserAccount> { it.isFounder }.thenBy { it.displayName.lowercase() })
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreException(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        }

    override val founderAuditLogs: Flow<List<FounderAuditEntry>> =
        if (!isFounderSession || currentUserId.isBlank()) {
            flowOf(emptyList())
        } else {
            val path = "founder_audit_logs"
            db.collection("founder_audit_logs")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FounderAuditLogDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).map { it.toDomain() }
                        .sortedByDescending { it.timestampMillis }
                        .take(50)
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreException(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        }

    override val walletState: Flow<WalletStateEntity> = combine(
        currentUserAccount,
        globalExchangeLedger
    ) { account, ledger ->
        if (account == null) {
            WalletStateEntity()
        } else if (isFounderSession) {
            WalletStateEntity(
                diamonds = account.diamonds,
                coins = account.coins,
                adminFeeProfitCoins = ledger.sumOf { it.adminFeeCoins },
                totalExchangesCount = ledger.size,
                totalDiamondsExchanged = ledger.sumOf { it.diamondsExchanged },
                totalCoinsDistributed = ledger.sumOf { it.netCoinsReceived }
            )
        } else {
            WalletStateEntity(
                diamonds = account.diamonds,
                coins = account.coins,
                adminFeeProfitCoins = 0L,
                totalExchangesCount = account.totalExchangesCount,
                totalDiamondsExchanged = account.totalDiamondsExchanged,
                totalCoinsDistributed = account.totalCoinsDistributed
            )
        }
    }

    override val exchangeRecords: Flow<List<ExchangeRecordEntity>> =
        if (currentUserId.isBlank()) {
            flowOf(emptyList())
        } else {
            val path = "users/$currentUserId/exchanges"
            db.collection("users").document(currentUserId).collection("exchanges")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        UserExchangeDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).mapIndexed { index, doc ->
                        ExchangeRecordEntity(
                            id = doc.timestampMillis * 1000L + index,
                            diamondsExchanged = doc.diamondsExchanged,
                            grossCoins = doc.grossCoins,
                            netCoinsReceived = doc.netCoinsReceived,
                            adminFeeCoins = doc.adminFeeCoins,
                            timestampMillis = doc.timestampMillis
                        )
                    }.sortedByDescending { it.timestampMillis }
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreException(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        }

    override val walletActivities: Flow<List<WalletActivityEntity>> =
        if (currentUserId.isBlank()) {
            flowOf(emptyList())
        } else {
            val path = "users/$currentUserId/activities"
            db.collection("users").document(currentUserId).collection("activities")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        UserActivityDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).mapIndexed { index, doc ->
                        WalletActivityEntity(
                            id = doc.timestampMillis * 1000L + index,
                            type = doc.type,
                            title = doc.title,
                            subtitle = doc.subtitle,
                            amountDeltaText = doc.amountDeltaText,
                            timestampMillis = doc.timestampMillis
                        )
                    }.sortedByDescending { it.timestampMillis }
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreException(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        }

    override val chatThreads: Flow<List<ChatThread>> =
        if (currentUserId.isBlank()) {
            flowOf(emptyList())
        } else {
            val path = "chats"
            db.collection("chats")
                .whereArrayContains("participantIds", currentUserId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatThreadDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).map { it.toDomain(currentUserId) }
                        .sortedByDescending { it.lastMessageAtMillis }
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreException(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    emit(emptyList())
                }
        }

    override fun observeChatMessages(chatId: String): Flow<List<ChatMessage>> =
        if (currentUserId.isBlank() || chatId.isBlank()) {
            flowOf(emptyList())
        } else {
            val path = "chats/$chatId/messages"
            db.collection("chats").document(chatId).collection("messages")
                .whereArrayContains("participantIds", currentUserId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatMessageDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).map { it.toDomain(currentUserId) }
                        .sortedBy { it.timestampMillis }
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreException(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    emit(emptyList())
                }
        }

    override suspend fun initialize() {
        val path = "users/$currentUserId"
        try {
            val user = auth.currentUser ?: return
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            val existing = userRef.get().await()
            val defaultVirtualId = VirtualIdGenerator.fromUid(uid)
            val safeEmail = (user.email ?: "").trim().take(200)
            val safeName = (user.displayName?.takeIf { it.isNotBlank() }
                ?: safeEmail.substringBefore("@").ifBlank { "Player" }).take(100)

            val resolvedVirtualId: String
            val resolvedDisplayName: String
            val resolvedNickname: String
            val resolvedGender: String
            val resolvedPhoto: String

            if (!existing.exists()) {
                val role = if (FounderConfig.isFounderEmail(user.email, user.isEmailVerified)) {
                    FounderConfig.ROLE_FOUNDER
                } else {
                    FounderConfig.ROLE_PLAYER
                }
                resolvedVirtualId = defaultVirtualId
                resolvedDisplayName = safeName
                resolvedNickname = ""
                resolvedGender = ""
                resolvedPhoto = ""

                val payload = mapOf(
                    "userId" to uid,
                    "virtualId" to resolvedVirtualId,
                    "email" to safeEmail,
                    "displayName" to resolvedDisplayName,
                    "nickname" to resolvedNickname,
                    "gender" to resolvedGender,
                    "dob" to "",
                    "photoBase64" to resolvedPhoto,
                    "role" to role,
                    "accountStatus" to FounderConfig.STATUS_ACTIVE,
                    "diamonds" to ExchangeCalculator.INITIAL_DIAMONDS,
                    "coins" to 0L,
                    "totalExchangesCount" to 0L,
                    "totalDiamondsExchanged" to 0L,
                    "totalCoinsDistributed" to 0L,
                    "gamesPlayed" to 0L,
                    "gamesWon" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.set(payload).await()
            } else {
                val existingVirtualId = existing.getString("virtualId")?.takeIf { it.isNotBlank() }
                resolvedVirtualId = existingVirtualId ?: defaultVirtualId
                resolvedDisplayName = existing.getString("displayName")?.takeIf { it.isNotBlank() } ?: safeName
                resolvedNickname = existing.getString("nickname") ?: ""
                resolvedGender = existing.getString("gender") ?: ""
                resolvedPhoto = existing.getString("photoBase64") ?: ""
                if (existingVirtualId == null) {
                    userRef.update(
                        mapOf(
                            "virtualId" to resolvedVirtualId,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            val virtualRef = db.collection("virtual_ids").document(resolvedVirtualId)
            if (!virtualRef.get().await().exists()) {
                virtualRef.set(
                    mapOf(
                        "virtualId" to resolvedVirtualId,
                        "userId" to uid,
                        "displayName" to resolvedDisplayName,
                        "nickname" to resolvedNickname,
                        "gender" to resolvedGender,
                        "photoBase64" to resolvedPhoto,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            if (isFounderSession) {
                val configRef = db.collection("app_config").document("platform")
                if (!configRef.get().await().exists()) {
                    val defaults = PlatformSettings()
                    configRef.set(platformConfigPayload(defaults, user.email ?: FounderConfig.FOUNDER_EMAIL)).await()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    override suspend fun updateUserProfile(
        displayName: String,
        nickname: String,
        gender: String,
        dob: String,
        photoBase64: String
    ): Result<Unit> {
        var docPath = "users/$currentUserId"
        return try {
            val cleanName = displayName.trim().take(100)
            require(cleanName.isNotBlank()) { "Name cannot be blank" }
            val cleanNick = nickname.trim().take(40)
            val cleanGender = gender.trim().take(30)
            val cleanDob = dob.trim().take(20)
            val cleanPhoto = photoBase64.take(150_000)

            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            docPath = userRef.path
            val snap = userRef.get().await()
            val existingVirtualId = snap.getString("virtualId")?.takeIf { it.isNotBlank() }
            val virtualId = existingVirtualId ?: VirtualIdGenerator.fromUid(uid)

            val virtualRef = db.collection("virtual_ids").document(virtualId)
            val virtualExists = virtualRef.get().await().exists()

            val userUpdates = mutableMapOf<String, Any>(
                "displayName" to cleanName,
                "nickname" to cleanNick,
                "gender" to cleanGender,
                "dob" to cleanDob,
                "photoBase64" to cleanPhoto,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (existingVirtualId == null) {
                userUpdates["virtualId"] = virtualId
            }

            val batch = db.batch()
            batch.update(userRef, userUpdates)
            if (virtualExists) {
                batch.update(
                    virtualRef,
                    mapOf(
                        "displayName" to cleanName,
                        "nickname" to cleanNick,
                        "gender" to cleanGender,
                        "photoBase64" to cleanPhoto,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    virtualRef,
                    mapOf(
                        "virtualId" to virtualId,
                        "userId" to uid,
                        "displayName" to cleanName,
                        "nickname" to cleanNick,
                        "gender" to cleanGender,
                        "photoBase64" to cleanPhoto,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()

            try {
                val request = UserProfileChangeRequest.Builder()
                    .setDisplayName(cleanName)
                    .build()
                auth.currentUser?.updateProfile(request)?.await()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Best-effort Firebase Auth display name sync
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun lookupPlayerByVirtualId(rawVirtualId: String): Result<VirtualPlayerLookup?> {
        val normalized = VirtualIdGenerator.normalize(rawVirtualId)
        if (!VirtualIdGenerator.isValid(normalized)) {
            return Result.failure(IllegalArgumentException("Enter a valid Virtual ID (e.g., VGD-ABC123)"))
        }
        val docPath = "virtual_ids/$normalized"
        return try {
            requireUserId()
            val snap = db.collection("virtual_ids").document(normalized).get().await()
            if (!snap.exists()) {
                Result.success(null)
            } else {
                val entry = snap.toObject(
                    VirtualIdDirectoryDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )?.toDomain()
                Result.success(entry)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docPath)
            Result.failure(e)
        }
    }

    override suspend fun openOrCreateDirectChat(peer: VirtualPlayerLookup): Result<ChatThread> {
        var docPath = "chats"
        return try {
            val uid = requireUserId()
            require(peer.userId.isNotBlank() && peer.userId != uid) {
                "Choose another player's Virtual ID to start a 1v1 chat"
            }

            val selfSnap = db.collection("users").document(uid).get().await()
            val selfVirtualId = selfSnap.getString("virtualId")?.takeIf { it.isNotBlank() }
                ?: VirtualIdGenerator.fromUid(uid)
            val selfName = (selfSnap.getString("displayName")?.takeIf { it.isNotBlank() }
                ?: auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
                ?: "Player").take(100)
            val selfPhoto = (selfSnap.getString("photoBase64") ?: "").take(150_000)

            val currentIsUser1 = uid < peer.userId
            val user1Id = if (currentIsUser1) uid else peer.userId
            val user1Vid = if (currentIsUser1) selfVirtualId else peer.virtualId
            val user1Name = (if (currentIsUser1) selfName else peer.displayName).ifBlank { "Player" }.take(100)
            val user1Photo = (if (currentIsUser1) selfPhoto else peer.photoBase64).take(150_000)

            val user2Id = if (currentIsUser1) peer.userId else uid
            val user2Vid = if (currentIsUser1) peer.virtualId else selfVirtualId
            val user2Name = (if (currentIsUser1) peer.displayName else selfName).ifBlank { "Player" }.take(100)
            val user2Photo = (if (currentIsUser1) peer.photoBase64 else selfPhoto).take(150_000)

            val chatId = "${user1Id}_${user2Id}"
            val chatRef = db.collection("chats").document(chatId)
            docPath = chatRef.path
            val existing = chatRef.get().await()

            if (existing.exists()) {
                val thread = existing.toObject(
                    ChatThreadDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )?.toDomain(uid) ?: ChatThread(
                    chatId = chatId,
                    participantIds = listOf(user1Id, user2Id),
                    peerUserId = peer.userId,
                    peerVirtualId = peer.virtualId,
                    peerName = peer.displayName,
                    peerPhotoBase64 = peer.photoBase64
                )
                return Result.success(thread)
            }

            val payload = mapOf(
                "participantIds" to listOf(user1Id, user2Id),
                "user1Id" to user1Id,
                "user1VirtualId" to user1Vid,
                "user1Name" to user1Name,
                "user1Photo" to user1Photo,
                "user2Id" to user2Id,
                "user2VirtualId" to user2Vid,
                "user2Name" to user2Name,
                "user2Photo" to user2Photo,
                "lastMessageText" to "",
                "lastSenderId" to "",
                "lastMessageAtMillis" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            chatRef.set(payload).await()

            Result.success(
                ChatThread(
                    chatId = chatId,
                    participantIds = listOf(user1Id, user2Id),
                    peerUserId = peer.userId,
                    peerVirtualId = peer.virtualId,
                    peerName = peer.displayName,
                    peerPhotoBase64 = peer.photoBase64
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun sendDirectMessage(
        chatId: String,
        peer: VirtualPlayerLookup?,
        text: String
    ): Result<Unit> {
        var docPath = "chats/$chatId"
        return try {
            val cleanText = text.trim().take(1000)
            require(cleanText.isNotBlank()) { "Message cannot be empty" }
            val uid = requireUserId()
            val chatRef = db.collection("chats").document(chatId)
            docPath = chatRef.path

            val selfSnap = db.collection("users").document(uid).get().await()
            val selfVirtualId = selfSnap.getString("virtualId")?.takeIf { it.isNotBlank() }
                ?: VirtualIdGenerator.fromUid(uid)
            val selfName = (selfSnap.getString("displayName")?.takeIf { it.isNotBlank() }
                ?: auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
                ?: "Player").take(100)
            val selfPhoto = (selfSnap.getString("photoBase64") ?: "").take(150_000)

            val chatSnap = chatRef.get().await()
            val nowMs = System.currentTimeMillis()
            val msgId = UUID.randomUUID().toString()
            val batch = db.batch()

            val participants: List<String>
            if (!chatSnap.exists()) {
                val targetPeer = peer ?: error("Peer details required to start chat")
                val currentIsUser1 = uid < targetPeer.userId
                val user1Id = if (currentIsUser1) uid else targetPeer.userId
                val user2Id = if (currentIsUser1) targetPeer.userId else uid
                participants = listOf(user1Id, user2Id)
                batch.set(
                    chatRef,
                    mapOf(
                        "participantIds" to participants,
                        "user1Id" to user1Id,
                        "user1VirtualId" to (if (currentIsUser1) selfVirtualId else targetPeer.virtualId),
                        "user1Name" to (if (currentIsUser1) selfName else targetPeer.displayName).ifBlank { "Player" }.take(100),
                        "user1Photo" to (if (currentIsUser1) selfPhoto else targetPeer.photoBase64).take(150_000),
                        "user2Id" to user2Id,
                        "user2VirtualId" to (if (currentIsUser1) targetPeer.virtualId else selfVirtualId),
                        "user2Name" to (if (currentIsUser1) targetPeer.displayName else selfName).ifBlank { "Player" }.take(100),
                        "user2Photo" to (if (currentIsUser1) targetPeer.photoBase64 else selfPhoto).take(150_000),
                        "lastMessageText" to cleanText,
                        "lastSenderId" to uid,
                        "lastMessageAtMillis" to nowMs,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                val doc = chatSnap.toObject(ChatThreadDoc::class.java)
                    ?: error("Invalid chat document")
                participants = doc.participantIds
                val currentIsUser1 = uid == doc.user1Id
                val updates = mutableMapOf<String, Any>(
                    "lastMessageText" to cleanText,
                    "lastSenderId" to uid,
                    "lastMessageAtMillis" to nowMs,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (currentIsUser1) {
                    updates["user1Name"] = selfName
                    updates["user1Photo"] = selfPhoto
                    updates["user1VirtualId"] = selfVirtualId
                } else {
                    updates["user2Name"] = selfName
                    updates["user2Photo"] = selfPhoto
                    updates["user2VirtualId"] = selfVirtualId
                }
                batch.update(chatRef, updates)
            }

            batch.set(
                chatRef.collection("messages").document(msgId),
                mapOf(
                    "chatId" to chatId,
                    "senderId" to uid,
                    "senderVirtualId" to selfVirtualId,
                    "senderName" to selfName,
                    "text" to cleanText,
                    "participantIds" to participants,
                    "timestampMillis" to nowMs,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun executeExchange(diamonds: Long): ExchangeExecutionResult {
        var docPath = "users/$currentUserId"
        return try {
            val uid = requireUserId()
            val user = auth.currentUser!!
            val configSnap = db.collection("app_config").document("platform").get().await()
            if (configSnap.exists()) {
                val exchangeEnabled = configSnap.getBoolean("exchangeEnabled") ?: true
                if (!exchangeEnabled && !isFounderSession) {
                    return ExchangeExecutionResult.Failure(ValidationError.BELOW_MINIMUM)
                }
            }

            val userRef = db.collection("users").document(uid)
            docPath = userRef.path
            val snap = userRef.get().await()
            val currentDiamonds = snap.getLong("diamonds") ?: ExchangeCalculator.INITIAL_DIAMONDS
            val currentCoins = snap.getLong("coins") ?: 0L
            val currentExCount = snap.getLong("totalExchangesCount") ?: 0L
            val currentExDiamonds = snap.getLong("totalDiamondsExchanged") ?: 0L
            val currentExCoins = snap.getLong("totalCoinsDistributed") ?: 0L
            val status = snap.getString("accountStatus") ?: FounderConfig.STATUS_ACTIVE
            if (status == FounderConfig.STATUS_SUSPENDED) {
                return ExchangeExecutionResult.Failure(ValidationError.INSUFFICIENT_BALANCE)
            }

            val breakdown = ExchangeCalculator.calculate(diamonds, currentDiamonds)
            if (!breakdown.isValid) {
                return ExchangeExecutionResult.Failure(breakdown.validationError)
            }

            val nowMs = System.currentTimeMillis()
            val exId = UUID.randomUUID().toString()
            val userEmail = (user.email ?: "").take(200)
            val userName = (user.displayName?.takeIf { it.isNotBlank() }
                ?: userEmail.substringBefore("@").ifBlank { "Player" }).take(100)

            val batch = db.batch()
            batch.update(
                userRef,
                mapOf(
                    "diamonds" to (currentDiamonds - breakdown.diamonds),
                    "coins" to (currentCoins + breakdown.netUserCoins),
                    "totalExchangesCount" to (currentExCount + 1L),
                    "totalDiamondsExchanged" to (currentExDiamonds + breakdown.diamonds),
                    "totalCoinsDistributed" to (currentExCoins + breakdown.netUserCoins),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                userRef.collection("exchanges").document(exId),
                mapOf(
                    "userId" to uid,
                    "diamondsExchanged" to breakdown.diamonds,
                    "grossCoins" to breakdown.grossCoins,
                    "netCoinsReceived" to breakdown.netUserCoins,
                    "adminFeeCoins" to breakdown.adminFeeCoins,
                    "timestampMillis" to nowMs,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("exchange_ledger").document(exId),
                mapOf(
                    "userId" to uid,
                    "userEmail" to userEmail,
                    "userName" to userName,
                    "diamondsExchanged" to breakdown.diamonds,
                    "grossCoins" to breakdown.grossCoins,
                    "netCoinsReceived" to breakdown.netUserCoins,
                    "adminFeeCoins" to breakdown.adminFeeCoins,
                    "timestampMillis" to nowMs,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()

            ExchangeExecutionResult.Success(
                diamondsExchanged = breakdown.diamonds,
                netCoinsReceived = breakdown.netUserCoins,
                adminFeeCoins = breakdown.adminFeeCoins
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            ExchangeExecutionResult.Failure(null)
        }
    }

    override suspend fun buyCoins(coinsAmount: Long): Result<Unit> {
        var docPath = "users/$currentUserId"
        return try {
            require(coinsAmount > 0L) { "Coin amount must be positive" }
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            docPath = userRef.path
            val snap = userRef.get().await()
            val currentCoins = snap.getLong("coins") ?: 0L
            val actId = UUID.randomUUID().toString()
            val nowMs = System.currentTimeMillis()
            val batch = db.batch()
            batch.update(
                userRef,
                mapOf(
                    "coins" to (currentCoins + coinsAmount).coerceAtMost(1_000_000_000L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                userRef.collection("activities").document(actId),
                activityPayload(
                    userId = uid,
                    type = "BUY_COINS",
                    title = "Purchased ${ExchangeCalculator.formatNumber(coinsAmount)} Coins",
                    subtitle = "Added to VGD Housie Wallet",
                    amountDeltaText = "+${ExchangeCalculator.formatNumber(coinsAmount)} Coins",
                    timestampMillis = nowMs
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun withdrawDiamonds(diamondsAmount: Long, destination: String): Result<Boolean> {
        var docPath = "users/$currentUserId"
        return try {
            if (diamondsAmount <= 0L) return Result.success(false)
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            docPath = userRef.path
            val snap = userRef.get().await()
            val currentDiamonds = snap.getLong("diamonds") ?: 0L
            if (diamondsAmount > currentDiamonds) return Result.success(false)
            val actId = UUID.randomUUID().toString()
            val nowMs = System.currentTimeMillis()
            val batch = db.batch()
            batch.update(
                userRef,
                mapOf(
                    "diamonds" to (currentDiamonds - diamondsAmount),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                userRef.collection("activities").document(actId),
                activityPayload(
                    userId = uid,
                    type = "WITHDRAW_DIAMOND",
                    title = "Withdrew ${ExchangeCalculator.formatNumber(diamondsAmount)} Diamond",
                    subtitle = "To ${destination.take(120)}",
                    amountDeltaText = "-${ExchangeCalculator.formatNumber(diamondsAmount)} Diamond",
                    timestampMillis = nowMs
                )
            )
            batch.commit().await()
            Result.success(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun addBonusDiamonds(diamondsAmount: Long): Result<Unit> {
        val uid = currentUserId
        return founderGenerateCurrency(
            targetUserId = uid,
            coinsToAdd = 0L,
            diamondsToAdd = diamondsAmount,
            reason = "Founder Quick Test Credit"
        )
    }

    override suspend fun joinGameRoom(roomName: String, entryCoins: Long): Result<Boolean> {
        var docPath = "users/$currentUserId"
        return try {
            if (entryCoins <= 0L) return Result.success(false)
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            docPath = userRef.path
            val snap = userRef.get().await()
            val currentCoins = snap.getLong("coins") ?: 0L
            val gamesPlayed = snap.getLong("gamesPlayed") ?: 0L
            val status = snap.getString("accountStatus") ?: FounderConfig.STATUS_ACTIVE
            if (status == FounderConfig.STATUS_SUSPENDED || currentCoins < entryCoins) {
                return Result.success(false)
            }
            val actId = UUID.randomUUID().toString()
            val nowMs = System.currentTimeMillis()
            val batch = db.batch()
            batch.update(
                userRef,
                mapOf(
                    "coins" to (currentCoins - entryCoins),
                    "gamesPlayed" to (gamesPlayed + 1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                userRef.collection("activities").document(actId),
                activityPayload(
                    userId = uid,
                    type = "ROOM_ENTRY",
                    title = "Joined ${roomName.take(80)}",
                    subtitle = "Housie Entry Fee",
                    amountDeltaText = "-${ExchangeCalculator.formatNumber(entryCoins)} Coins",
                    timestampMillis = nowMs
                )
            )
            batch.commit().await()
            Result.success(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun awardGamePrize(roomName: String, claimName: String, prizeDiamonds: Long): Result<Unit> {
        var docPath = "users/$currentUserId"
        return try {
            if (prizeDiamonds <= 0L) return Result.success(Unit)
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            docPath = userRef.path
            val snap = userRef.get().await()
            val currentDiamonds = snap.getLong("diamonds") ?: 0L
            val gamesWon = snap.getLong("gamesWon") ?: 0L
            val actId = UUID.randomUUID().toString()
            val nowMs = System.currentTimeMillis()
            val batch = db.batch()
            batch.update(
                userRef,
                mapOf(
                    "diamonds" to (currentDiamonds + prizeDiamonds).coerceAtMost(1_000_000_000L),
                    "gamesWon" to (gamesWon + 1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                userRef.collection("activities").document(actId),
                activityPayload(
                    userId = uid,
                    type = "GAME_WIN",
                    title = "Won ${claimName.take(40)} in ${roomName.take(60)}",
                    subtitle = "Housie Prize Credited",
                    amountDeltaText = "+${ExchangeCalculator.formatNumber(prizeDiamonds)} Diamond",
                    timestampMillis = nowMs
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun founderGenerateCurrency(
        targetUserId: String,
        coinsToAdd: Long,
        diamondsToAdd: Long,
        reason: String
    ): Result<Unit> {
        var docPath = "users/$targetUserId"
        return try {
            check(isFounderSession) { "Founder verification required" }
            val founderUid = requireUserId()
            val founderEmail = auth.currentUser?.email ?: FounderConfig.FOUNDER_EMAIL
            require(targetUserId.isNotBlank()) { "Target user ID required" }
            require(coinsToAdd >= 0L && diamondsToAdd >= 0L && (coinsToAdd > 0L || diamondsToAdd > 0L)) {
                "Specify positive Coins or Diamonds to generate"
            }

            val userRef = db.collection("users").document(targetUserId)
            docPath = userRef.path
            val snap = userRef.get().await()
            check(snap.exists()) { "Target user does not exist" }
            val currentCoins = snap.getLong("coins") ?: 0L
            val currentDiamonds = snap.getLong("diamonds") ?: 0L
            val targetEmail = snap.getString("email") ?: ""
            val nowMs = System.currentTimeMillis()
            val cleanReason = reason.trim().ifBlank { "Founder Mint Credit" }.take(120)

            val batch = db.batch()
            batch.update(
                userRef,
                mapOf(
                    "coins" to (currentCoins + coinsToAdd).coerceAtMost(1_000_000_000L),
                    "diamonds" to (currentDiamonds + diamondsToAdd).coerceAtMost(1_000_000_000L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            val deltaParts = buildList {
                if (coinsToAdd > 0L) add("+${ExchangeCalculator.formatNumber(coinsToAdd)} Coins")
                if (diamondsToAdd > 0L) add("+${ExchangeCalculator.formatNumber(diamondsToAdd)} Diamond")
            }
            val actId = UUID.randomUUID().toString()
            batch.set(
                userRef.collection("activities").document(actId),
                activityPayload(
                    userId = targetUserId,
                    type = "FOUNDER_GRANT",
                    title = "Founder Credited ${deltaParts.joinToString(" & ")}",
                    subtitle = cleanReason,
                    amountDeltaText = deltaParts.joinToString(" / "),
                    timestampMillis = nowMs
                )
            )

            val configRef = db.collection("app_config").document("platform")
            val configSnap = configRef.get().await()
            if (configSnap.exists()) {
                val prevCoinsGen = configSnap.getLong("totalCoinsGeneratedByFounder") ?: 0L
                val prevDiamondsGen = configSnap.getLong("totalDiamondsGeneratedByFounder") ?: 0L
                batch.update(
                    configRef,
                    mapOf(
                        "totalCoinsGeneratedByFounder" to (prevCoinsGen + coinsToAdd).coerceAtMost(1_000_000_000L),
                        "totalDiamondsGeneratedByFounder" to (prevDiamondsGen + diamondsToAdd).coerceAtMost(1_000_000_000L),
                        "updatedByEmail" to founderEmail.take(200),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            val logId = UUID.randomUUID().toString()
            batch.set(
                db.collection("founder_audit_logs").document(logId),
                mapOf(
                    "actorUid" to founderUid,
                    "actorEmail" to founderEmail.take(200),
                    "actionType" to if (coinsToAdd > 0L) "MINT_COINS" else "MINT_DIAMONDS",
                    "targetUserId" to targetUserId,
                    "targetEmail" to targetEmail.take(200),
                    "summary" to "Generated ${deltaParts.joinToString(" & ")} for ${targetEmail.ifBlank { targetUserId }} ($cleanReason)".take(300),
                    "timestampMillis" to nowMs,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun founderBroadcastCurrency(
        coinsToAdd: Long,
        diamondsToAdd: Long,
        reason: String
    ): Result<Int> {
        val path = "users"
        return try {
            check(isFounderSession) { "Founder verification required" }
            val founderUid = requireUserId()
            val founderEmail = auth.currentUser?.email ?: FounderConfig.FOUNDER_EMAIL
            require(coinsToAdd >= 0L && diamondsToAdd >= 0L && (coinsToAdd > 0L || diamondsToAdd > 0L)) {
                "Specify positive Coins or Diamonds to broadcast"
            }

            val usersSnap = db.collection("users").get().await()
            val activeDocs = usersSnap.documents.filter {
                (it.getString("accountStatus") ?: FounderConfig.STATUS_ACTIVE) == FounderConfig.STATUS_ACTIVE
            }
            val nowMs = System.currentTimeMillis()
            val cleanReason = reason.trim().ifBlank { "Founder Broadcast Bonus" }.take(120)
            val deltaParts = buildList {
                if (coinsToAdd > 0L) add("+${ExchangeCalculator.formatNumber(coinsToAdd)} Coins")
                if (diamondsToAdd > 0L) add("+${ExchangeCalculator.formatNumber(diamondsToAdd)} Diamond")
            }

            val batch = db.batch()
            activeDocs.forEach { doc ->
                val uid = doc.id
                val curCoins = doc.getLong("coins") ?: 0L
                val curDiamonds = doc.getLong("diamonds") ?: 0L
                batch.update(
                    doc.reference,
                    mapOf(
                        "coins" to (curCoins + coinsToAdd).coerceAtMost(1_000_000_000L),
                        "diamonds" to (curDiamonds + diamondsToAdd).coerceAtMost(1_000_000_000L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val actId = UUID.randomUUID().toString()
                batch.set(
                    doc.reference.collection("activities").document(actId),
                    activityPayload(
                        userId = uid,
                        type = "FOUNDER_GRANT",
                        title = "Founder Broadcast: ${deltaParts.joinToString(" & ")}",
                        subtitle = cleanReason,
                        amountDeltaText = deltaParts.joinToString(" / "),
                        timestampMillis = nowMs
                    )
                )
            }

            val configRef = db.collection("app_config").document("platform")
            val configSnap = configRef.get().await()
            if (configSnap.exists()) {
                val prevCoinsGen = configSnap.getLong("totalCoinsGeneratedByFounder") ?: 0L
                val prevDiamondsGen = configSnap.getLong("totalDiamondsGeneratedByFounder") ?: 0L
                batch.update(
                    configRef,
                    mapOf(
                        "totalCoinsGeneratedByFounder" to (prevCoinsGen + coinsToAdd * activeDocs.size).coerceAtMost(1_000_000_000L),
                        "totalDiamondsGeneratedByFounder" to (prevDiamondsGen + diamondsToAdd * activeDocs.size).coerceAtMost(1_000_000_000L),
                        "updatedByEmail" to founderEmail.take(200),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            val logId = UUID.randomUUID().toString()
            batch.set(
                db.collection("founder_audit_logs").document(logId),
                mapOf(
                    "actorUid" to founderUid,
                    "actorEmail" to founderEmail.take(200),
                    "actionType" to "BROADCAST_MINT",
                    "targetUserId" to "ALL_ACTIVE",
                    "targetEmail" to "${activeDocs.size} active users",
                    "summary" to "Broadcast ${deltaParts.joinToString(" & ")} to ${activeDocs.size} users ($cleanReason)".take(300),
                    "timestampMillis" to nowMs,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(activeDocs.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override suspend fun founderAdjustOrResetUser(
        targetUserId: String,
        resetToZero: Boolean,
        coinsToDeduct: Long,
        diamondsToDeduct: Long,
        reason: String
    ): Result<Unit> {
        var docPath = "users/$targetUserId"
        return try {
            check(isFounderSession) { "Founder verification required" }
            val founderUid = requireUserId()
            val founderEmail = auth.currentUser?.email ?: FounderConfig.FOUNDER_EMAIL
            val userRef = db.collection("users").document(targetUserId)
            docPath = userRef.path
            val snap = userRef.get().await()
            check(snap.exists()) { "User not found" }
            val curCoins = snap.getLong("coins") ?: 0L
            val curDiamonds = snap.getLong("diamonds") ?: 0L
            val targetEmail = snap.getString("email") ?: targetUserId

            val nextCoins = if (resetToZero) 0L else (curCoins - coinsToDeduct.coerceAtLeast(0L)).coerceAtLeast(0L)
            val nextDiamonds = if (resetToZero) 0L else (curDiamonds - diamondsToDeduct.coerceAtLeast(0L)).coerceAtLeast(0L)
            val nowMs = System.currentTimeMillis()
            val summaryText = if (resetToZero) {
                "Reset wallet to 0 Coins & 0 Diamond for $targetEmail"
            } else {
                "Adjusted wallet for $targetEmail (-${ExchangeCalculator.formatNumber(coinsToDeduct)} Coins, -${ExchangeCalculator.formatNumber(diamondsToDeduct)} Diamond)"
            }

            val batch = db.batch()
            batch.update(
                userRef,
                mapOf(
                    "coins" to nextCoins,
                    "diamonds" to nextDiamonds,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("founder_audit_logs").document(UUID.randomUUID().toString()),
                mapOf(
                    "actorUid" to founderUid,
                    "actorEmail" to founderEmail.take(200),
                    "actionType" to "WALLET_ADJUST",
                    "targetUserId" to targetUserId,
                    "targetEmail" to targetEmail.take(200),
                    "summary" to summaryText.take(300),
                    "timestampMillis" to nowMs,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun founderSetUserStatus(targetUserId: String, newStatus: String): Result<Unit> {
        var docPath = "users/$targetUserId"
        return try {
            check(isFounderSession) { "Founder verification required" }
            val founderUid = requireUserId()
            val founderEmail = auth.currentUser?.email ?: FounderConfig.FOUNDER_EMAIL
            require(newStatus == FounderConfig.STATUS_ACTIVE || newStatus == FounderConfig.STATUS_SUSPENDED)
            check(targetUserId != founderUid) { "Founder account cannot be suspended" }

            val userRef = db.collection("users").document(targetUserId)
            docPath = userRef.path
            val snap = userRef.get().await()
            val targetEmail = snap.getString("email") ?: targetUserId
            val nowMs = System.currentTimeMillis()

            val batch = db.batch()
            batch.update(
                userRef,
                mapOf(
                    "accountStatus" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("founder_audit_logs").document(UUID.randomUUID().toString()),
                mapOf(
                    "actorUid" to founderUid,
                    "actorEmail" to founderEmail.take(200),
                    "actionType" to "USER_STATUS",
                    "targetUserId" to targetUserId,
                    "targetEmail" to targetEmail.take(200),
                    "summary" to "Set account status to ${newStatus.uppercase()} for $targetEmail".take(300),
                    "timestampMillis" to nowMs,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun founderUpdatePlatformSettings(
        settings: PlatformSettings,
        actionType: String,
        summary: String
    ): Result<Unit> {
        val docPath = "app_config/platform"
        return try {
            check(isFounderSession) { "Founder verification required" }
            val founderUid = requireUserId()
            val founderEmail = auth.currentUser?.email ?: FounderConfig.FOUNDER_EMAIL
            val nowMs = System.currentTimeMillis()

            val batch = db.batch()
            batch.set(
                db.collection("app_config").document("platform"),
                platformConfigPayload(settings, founderEmail)
            )
            batch.set(
                db.collection("founder_audit_logs").document(UUID.randomUUID().toString()),
                mapOf(
                    "actorUid" to founderUid,
                    "actorEmail" to founderEmail.take(200),
                    "actionType" to actionType.take(50),
                    "targetUserId" to "PLATFORM",
                    "targetEmail" to "All App Users",
                    "summary" to summary.take(300),
                    "timestampMillis" to nowMs,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    private fun activityPayload(
        userId: String,
        type: String,
        title: String,
        subtitle: String,
        amountDeltaText: String,
        timestampMillis: Long
    ): Map<String, Any> = mapOf(
        "userId" to userId,
        "type" to type.take(40),
        "title" to title.take(160),
        "subtitle" to subtitle.take(200),
        "amountDeltaText" to amountDeltaText.take(80),
        "timestampMillis" to timestampMillis,
        "createdAt" to FieldValue.serverTimestamp()
    )

    private fun platformConfigPayload(
        settings: PlatformSettings,
        founderEmail: String
    ): Map<String, Any> = mapOf(
        "configId" to "platform",
        "exchangeEnabled" to settings.exchangeEnabled,
        "maintenanceMode" to settings.maintenanceMode,
        "announcementBanner" to settings.announcementBanner.take(300),
        "smallEntryCoins" to settings.smallEntryCoins.coerceIn(0L, 100_000_000L),
        "smallPrizeDiamonds" to settings.smallPrizeDiamonds.coerceIn(0L, 100_000_000L),
        "mediumEntryCoins" to settings.mediumEntryCoins.coerceIn(0L, 100_000_000L),
        "mediumPrizeDiamonds" to settings.mediumPrizeDiamonds.coerceIn(0L, 100_000_000L),
        "bigEntryCoins" to settings.bigEntryCoins.coerceIn(0L, 100_000_000L),
        "bigPrizeDiamonds" to settings.bigPrizeDiamonds.coerceIn(0L, 100_000_000L),
        "vipEntryCoins" to settings.vipEntryCoins.coerceIn(0L, 100_000_000L),
        "vipPrizeDiamonds" to settings.vipPrizeDiamonds.coerceIn(0L, 100_000_000L),
        "totalCoinsGeneratedByFounder" to settings.totalCoinsGeneratedByFounder.coerceIn(0L, 1_000_000_000L),
        "totalDiamondsGeneratedByFounder" to settings.totalDiamondsGeneratedByFounder.coerceIn(0L, 1_000_000_000L),
        "updatedByEmail" to founderEmail.take(200),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}
