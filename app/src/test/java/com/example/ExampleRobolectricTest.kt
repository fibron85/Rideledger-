package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.SessionManager
import com.example.data.repository.RideLedgerRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: RideLedgerRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RideLedgerRepository(
            driverDao = database.driverDao(),
            settingsDao = database.settingsDao(),
            rideDao = database.rideDao(),
            sessionManager = SessionManager(context)
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `read app name from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("RideLedger", appName)
    }

    @Test
    fun `local driver registration profile setup and local password reset succeed`() = runTest {
        val registerResult = repository.registerDriver("1234567", "112233")
        assertTrue(registerResult.isSuccess)

        val profileResult = repository.updateProfile("1234567", "Tariq Ali", "0509876543")
        assertTrue(profileResult.isSuccess)

        // Wrong mobile should fail password reset
        val wrongReset = repository.resetPassword("1234567", "0500000000", "998877")
        assertTrue(wrongReset.isFailure)

        // Correct mobile resets password locally
        val validReset = repository.resetPassword("1234567", "0509876543", "998877")
        assertTrue(validReset.isSuccess)

        // Old password fails, new password succeeds
        assertTrue(repository.loginDriver("1234567", "112233").isFailure)
        assertTrue(repository.loginDriver("1234567", "998877").isSuccess)
    }
}
