package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.SessionManager
import com.example.data.local.dao.DriverDao
import com.example.data.local.dao.RideDao
import com.example.data.local.dao.SettingsDao
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.DriverAccountEntity
import com.example.data.local.entity.PickupPointEntity
import com.example.data.local.entity.PlatformEntity
import com.example.data.local.entity.RideEntity
import com.example.domain.BillingCycle
import com.example.domain.RideCalculator
import com.example.domain.SecurityUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class ImportResult(
    val importedCount: Int,
    val skippedDuplicates: Int
)

class RideLedgerRepository(
    private val driverDao: DriverDao,
    private val settingsDao: SettingsDao,
    private val rideDao: RideDao,
    private val sessionManager: SessionManager
) {
    val activeDriverId: Flow<String?> = sessionManager.activeDriverId

    val biometricDriverId: Flow<String?> = sessionManager.biometricDriverId

    val storageSetupCompleted: Flow<Boolean> = sessionManager.storageSetupCompleted

    val grantedFolderUri: Flow<String?> = sessionManager.grantedFolderUri

    val platforms: Flow<List<PlatformEntity>> = settingsDao.observePlatforms()

    val pickupPoints: Flow<List<PickupPointEntity>> = settingsDao.observePickupPoints()

    val appSettings: Flow<AppSettingsEntity> = settingsDao.observeAppSettings().map { settings ->
        settings ?: AppSettingsEntity()
    }

    fun observeDriver(driverId: String): Flow<DriverAccountEntity?> =
        driverDao.observeDriver(driverId)

    fun observeAllRides(driverId: String): Flow<List<RideEntity>> =
        rideDao.observeAllRidesForDriver(driverId)

    fun observeRidesForDate(driverId: String, dateIso: String): Flow<List<RideEntity>> =
        rideDao.observeRidesForDate(driverId, dateIso)

    fun observeRidesForMonth(driverId: String, monthPrefix: String): Flow<List<RideEntity>> =
        rideDao.observeRidesForMonth(driverId, monthPrefix)

    suspend fun ensureDefaultsPopulated() {
        AppDatabase.seedInitialData(settingsDao)
    }

    suspend fun registerDriver(driverId: String, password: String): Result<DriverAccountEntity> {
        val cleanId = driverId.trim()
        val cleanPass = password.trim()
        if (!SecurityUtils.isValidDriverId(cleanId)) {
            return Result.failure(IllegalArgumentException("Driver ID must be exactly 7 digits."))
        }
        if (!SecurityUtils.isValidPassword(cleanPass)) {
            return Result.failure(IllegalArgumentException("Password must be exactly 6 digits."))
        }
        val existing = driverDao.getDriverById(cleanId)
        if (existing != null) {
            return Result.failure(IllegalStateException("Driver ID $cleanId already exists on this device."))
        }
        val account = DriverAccountEntity(
            driverId = cleanId,
            passwordHash = SecurityUtils.hashPassword(cleanPass)
        )
        driverDao.insertDriver(account)
        sessionManager.setActiveDriverId(cleanId)
        ensureDefaultsPopulated()
        return Result.success(account)
    }

    suspend fun loginDriver(driverId: String, password: String): Result<DriverAccountEntity> {
        val cleanId = driverId.trim()
        val cleanPass = password.trim()
        if (!SecurityUtils.isValidDriverId(cleanId)) {
            return Result.failure(IllegalArgumentException("Enter a valid 7-digit Driver ID."))
        }
        if (!SecurityUtils.isValidPassword(cleanPass)) {
            return Result.failure(IllegalArgumentException("Enter a valid 6-digit password."))
        }
        val driver = driverDao.getDriverById(cleanId)
            ?: return Result.failure(IllegalStateException("No local account found for Driver ID $cleanId."))
        if (driver.passwordHash != SecurityUtils.hashPassword(cleanPass)) {
            return Result.failure(IllegalArgumentException("Incorrect 6-digit password."))
        }
        sessionManager.setActiveDriverId(cleanId)
        ensureDefaultsPopulated()
        return Result.success(driver)
    }

    suspend fun loginWithBiometric(driverId: String): Result<DriverAccountEntity> {
        val cleanId = driverId.trim()
        val driver = driverDao.getDriverById(cleanId)
            ?: run {
                sessionManager.setBiometricDriverId(null)
                return Result.failure(IllegalStateException("Enrolled local driver account no longer exists."))
            }
        sessionManager.setActiveDriverId(driver.driverId)
        ensureDefaultsPopulated()
        return Result.success(driver)
    }

    suspend fun setBiometricEnabled(driverId: String, enabled: Boolean) {
        if (enabled) {
            sessionManager.setBiometricDriverId(driverId.trim())
        } else {
            sessionManager.setBiometricDriverId(null)
        }
    }

    suspend fun setStorageSetupCompleted(completed: Boolean) {
        sessionManager.setStorageSetupCompleted(completed)
    }

    suspend fun setGrantedFolderUri(uriString: String?) {
        sessionManager.setGrantedFolderUri(uriString)
    }

    suspend fun resetPassword(
        driverId: String,
        mobileNumber: String,
        newPassword: String
    ): Result<Unit> {
        val cleanId = driverId.trim()
        val normalizedMobile = SecurityUtils.normalizeMobile(mobileNumber)
        val cleanNewPass = newPassword.trim()

        if (!SecurityUtils.isValidDriverId(cleanId)) {
            return Result.failure(IllegalArgumentException("Enter a valid 7-digit Driver ID."))
        }
        if (normalizedMobile.length < 7) {
            return Result.failure(IllegalArgumentException("Enter your registered mobile number."))
        }
        if (!SecurityUtils.isValidPassword(cleanNewPass)) {
            return Result.failure(IllegalArgumentException("New password must be exactly 6 digits."))
        }

        val driver = driverDao.getDriverById(cleanId)
            ?: return Result.failure(IllegalStateException("No account found for Driver ID $cleanId."))

        val savedMobile = SecurityUtils.normalizeMobile(driver.mobileNumber)
        if (savedMobile.isEmpty()) {
            return Result.failure(
                IllegalStateException("No mobile number is saved on this account. Cannot verify reset.")
            )
        }
        if (savedMobile != normalizedMobile) {
            return Result.failure(IllegalArgumentException("Mobile number does not match this Driver ID."))
        }

        driverDao.updatePassword(cleanId, SecurityUtils.hashPassword(cleanNewPass))
        return Result.success(Unit)
    }

    suspend fun updateProfile(
        driverId: String,
        fullName: String,
        mobileNumber: String
    ): Result<Unit> {
        val cleanName = fullName.trim()
        val cleanMobile = mobileNumber.trim()
        if (cleanName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Full name cannot be empty."))
        }
        if (!SecurityUtils.isValidMobile(cleanMobile)) {
            return Result.failure(IllegalArgumentException("Enter a valid mobile number (at least 7 digits)."))
        }
        driverDao.updateProfile(driverId, cleanName, cleanMobile)
        return Result.success(Unit)
    }

    suspend fun changePassword(
        driverId: String,
        currentPassword: String,
        newPassword: String
    ): Result<Unit> {
        val driver = driverDao.getDriverById(driverId)
            ?: return Result.failure(IllegalStateException("Driver account not found."))
        if (driver.passwordHash != SecurityUtils.hashPassword(currentPassword.trim())) {
            return Result.failure(IllegalArgumentException("Current password is incorrect."))
        }
        if (!SecurityUtils.isValidPassword(newPassword.trim())) {
            return Result.failure(IllegalArgumentException("New password must be exactly 6 digits."))
        }
        driverDao.updatePassword(driverId, SecurityUtils.hashPassword(newPassword.trim()))
        return Result.success(Unit)
    }

    suspend fun logout() {
        sessionManager.clearSession()
    }

    suspend fun addPlatform(name: String): Result<Unit> {
        val clean = name.trim()
        if (clean.isEmpty()) {
            return Result.failure(IllegalArgumentException("Platform name cannot be empty."))
        }
        val insertedId = settingsDao.insertPlatform(PlatformEntity(name = clean))
        if (insertedId == -1L) {
            return Result.failure(IllegalStateException("Platform \"$clean\" already exists."))
        }
        return Result.success(Unit)
    }

    suspend fun deletePlatform(platform: PlatformEntity) {
        settingsDao.deletePlatform(platform)
    }

    suspend fun addPickupPoint(
        name: String,
        boltBookingFee: Double,
        dtcBookingFee: Double
    ): Result<Unit> {
        val clean = name.trim()
        if (clean.isEmpty()) {
            return Result.failure(IllegalArgumentException("Pickup point name cannot be empty."))
        }
        val existing = settingsDao.getPickupPointByName(clean)
        if (existing != null) {
            return Result.failure(IllegalStateException("Pickup point \"$clean\" already exists."))
        }
        settingsDao.insertPickupPoint(
            PickupPointEntity(
                name = clean,
                boltBookingFee = RideCalculator.round2(boltBookingFee.coerceAtLeast(0.0)),
                dtcBookingFee = RideCalculator.round2(dtcBookingFee.coerceAtLeast(0.0))
            )
        )
        return Result.success(Unit)
    }

    suspend fun updatePickupPoint(pickupPoint: PickupPointEntity): Result<Unit> {
        val clean = pickupPoint.name.trim()
        if (clean.isEmpty()) {
            return Result.failure(IllegalArgumentException("Pickup point name cannot be empty."))
        }
        settingsDao.updatePickupPoint(
            pickupPoint.copy(
                name = clean,
                boltBookingFee = RideCalculator.round2(pickupPoint.boltBookingFee.coerceAtLeast(0.0)),
                dtcBookingFee = RideCalculator.round2(pickupPoint.dtcBookingFee.coerceAtLeast(0.0))
            )
        )
        return Result.success(Unit)
    }

    suspend fun deletePickupPoint(pickupPoint: PickupPointEntity) {
        settingsDao.deletePickupPoint(pickupPoint)
    }

    suspend fun saveBookingFeeForPickupPoint(
        pickupPointName: String,
        platformName: String,
        bookingFee: Double
    ) {
        val pickup = settingsDao.getPickupPointByName(pickupPointName) ?: return
        val cleanFee = RideCalculator.round2(bookingFee.coerceAtLeast(0.0))
        val updated = when {
            RideCalculator.isBoltPlatform(platformName) -> pickup.copy(boltBookingFee = cleanFee)
            RideCalculator.isDtcPlatform(platformName) -> pickup.copy(dtcBookingFee = cleanFee)
            else -> return
        }
        settingsDao.updatePickupPoint(updated)
    }

    suspend fun updateFuelAndCurrency(fuelCostPerKm: Double, currencyCode: String): Result<Unit> {
        if (fuelCostPerKm < 0.0) {
            return Result.failure(IllegalArgumentException("Fuel cost per km cannot be negative."))
        }
        val cleanCurrency = currencyCode.trim().uppercase().ifEmpty { "AED" }
        val current = settingsDao.getAppSettings() ?: AppSettingsEntity()
        settingsDao.upsertAppSettings(
            current.copy(
                id = 1,
                fuelCostPerKm = RideCalculator.round2(fuelCostPerKm),
                currencyCode = cleanCurrency
            )
        )
        return Result.success(Unit)
    }

    suspend fun updateShiftTiming(
        shiftStartHour: Int,
        shiftStartMinute: Int,
        shiftEndHour: Int,
        shiftEndMinute: Int
    ): Result<Unit> {
        val current = settingsDao.getAppSettings() ?: AppSettingsEntity()
        settingsDao.upsertAppSettings(
            current.copy(
                id = 1,
                shiftStartHour = shiftStartHour.coerceIn(0, 23),
                shiftStartMinute = shiftStartMinute.coerceIn(0, 59),
                shiftEndHour = shiftEndHour.coerceIn(0, 23),
                shiftEndMinute = shiftEndMinute.coerceIn(0, 59)
            )
        )
        return Result.success(Unit)
    }

    suspend fun saveRide(ride: RideEntity): Long = rideDao.insertRide(ride)

    suspend fun deleteRide(ride: RideEntity) = rideDao.deleteRide(ride)

    suspend fun getRideById(id: Long): RideEntity? = rideDao.getRideById(id)

    suspend fun submitRidesToFinance(rideIds: List<Long>) {
        if (rideIds.isEmpty()) return
        rideDao.markRidesSubmittedToFinance(
            rideIds = rideIds.distinct(),
            submittedAt = System.currentTimeMillis()
        )
    }

    suspend fun deleteRidesForCycle(
        driverId: String,
        cycle: BillingCycle,
        settings: AppSettingsEntity
    ): Int {
        val allRides = rideDao.getAllRidesForDriverOnce(driverId)
        val targetIds = allRides
            .filter { ride ->
                val shiftIso = RideCalculator.computeShiftDateIso(ride.rideDateMillis, settings)
                shiftIso >= cycle.startDateIso && shiftIso <= cycle.endDateIso
            }
            .map { it.id }

        if (targetIds.isEmpty()) return 0
        targetIds.chunked(500).forEach { batch ->
            rideDao.deleteRidesByIds(batch)
        }
        return targetIds.size
    }

    suspend fun importBackupRides(
        driverId: String,
        importedRides: List<RideEntity>
    ): ImportResult {
        if (importedRides.isEmpty()) return ImportResult(0, 0)
        val existingRides = rideDao.getAllRidesForDriverOnce(driverId)
        val existingKeys = existingRides.map { rideDedupeKey(it) }.toMutableSet()

        val toInsert = mutableListOf<RideEntity>()
        var skipped = 0
        for (ride in importedRides) {
            val normalized = ride.copy(id = 0L, driverId = driverId)
            val key = rideDedupeKey(normalized)
            if (existingKeys.add(key)) {
                toInsert.add(normalized)
            } else {
                skipped++
            }
        }
        if (toInsert.isNotEmpty()) {
            rideDao.insertRides(toInsert)
        }
        return ImportResult(importedCount = toInsert.size, skippedDuplicates = skipped)
    }

    private fun rideDedupeKey(ride: RideEntity): String {
        val dateStr = RideCalculator.toIsoDate(ride.rideDateMillis)
        val timeStr = RideCalculator.toTimeLabel(ride.rideDateMillis)
        return "$dateStr|$timeStr|${ride.platformName.trim().lowercase()}|${ride.paymentMethod.trim().lowercase()}|${RideCalculator.round2(ride.grossFare)}|${RideCalculator.round2(ride.netIncome)}"
    }
}
