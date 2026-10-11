package com.bugreel.pro.core.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// firestore.rules / storage.rules (CLAUDE.md Section 18 Launch Readiness Gate) only allow a
// signed-in user to touch their own data. BugReel has no login screen, so the first Firebase
// call signs in anonymously; the uid persists across app restarts. Requires the Anonymous
// sign-in provider to be enabled in the Firebase console.
@Singleton
class FirebaseUserSession @Inject constructor(
    private val auth: FirebaseAuth
) {
    private val signInLock = Mutex()

    suspend fun requireUserId(): String = auth.currentUser?.uid ?: signInLock.withLock {
        auth.currentUser?.uid
            ?: checkNotNull(auth.signInAnonymously().await().user) { "Anonymous sign-in returned no user" }.uid
    }
}
