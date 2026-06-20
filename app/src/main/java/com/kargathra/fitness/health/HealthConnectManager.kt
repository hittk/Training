package com.kargathra.fitness.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Length
import java.time.Instant
import java.time.ZoneOffset

/** Avg / max heart rate and calories for a session window, read back from
 *  whatever supplies them to Health Connect (here, the user's Fitbit). */
data class SessionVitals(
    val avgBpm: Long?,
    val maxBpm: Long?,
    val activeKcal: Double?
)

/**
 * Single entry point for everything Health Connect. All data is written to the
 * on-device Health Connect store; nothing leaves the phone.
 *
 * Heart rate and active calories are READ, not written: the user's Fitbit
 * already feeds these into Health Connect, so it stays the single source of
 * truth for vitals. Kargathra writes the one thing the watch can't — a
 * structured strength session with sets and reps — and reads the Fitbit's HR
 * back over the same window to enrich it.
 */
class HealthConnectManager(private val context: Context) {

    private val client: HealthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    val permissions: Set<String> = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(DistanceRecord::class),
        // Read-only: sourced from the Fitbit, never written by this app.
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    )

    fun requestPermissionContract() =
        PermissionController.createRequestPermissionResultContract()

    companion object {
        fun sdkStatus(context: Context): Int = HealthConnectClient.getSdkStatus(context)
        const val AVAILABLE = HealthConnectClient.SDK_AVAILABLE
        const val UPDATE_REQUIRED = HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED
        const val UNAVAILABLE = HealthConnectClient.SDK_UNAVAILABLE
    }

    suspend fun hasAllPermissions(): Boolean =
        client.permissionController.getGrantedPermissions().containsAll(permissions)

    /** True only when the SDK is present AND every needed permission is granted. */
    fun isSdkAvailable(): Boolean = sdkStatus(context) == AVAILABLE
    suspend fun isReady(): Boolean = isSdkAvailable() && hasAllPermissions()

    /**
     * Writes a completed strength-training session. Per-set rep detail attaches
     * as ExerciseSegments once the logging layer lands; for now this records the
     * session envelope. Calories/HR are intentionally NOT written — they come
     * from the Fitbit and are read back via [readSessionVitals].
     */
    suspend fun writeStrengthSession(
        start: Instant,
        end: Instant,
        title: String,
        zone: ZoneOffset = ZoneOffset.UTC
    ) {
        val records = buildList<Record> {
            add(
                ExerciseSessionRecord(
                    startTime = start, startZoneOffset = zone,
                    endTime = end, endZoneOffset = zone,
                    exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
                    title = title,
                    metadata = Metadata.manualEntry()
                )
            )
        }
        client.insertRecords(records)
    }

    /**
     * Writes a cardio session (spin bike / incline treadmill) with optional
     * distance. Note: if the Fitbit also auto-logs the activity, Health Connect
     * will hold both entries attributed to their source apps — a per-session
     * "let the watch log this" toggle is planned to avoid duplicates.
     */
    suspend fun writeCardioSession(
        start: Instant,
        end: Instant,
        title: String,
        exerciseType: Int,
        distanceMeters: Double? = null,
        zone: ZoneOffset = ZoneOffset.UTC
    ) {
        val records = buildList<Record> {
            add(
                ExerciseSessionRecord(
                    startTime = start, startZoneOffset = zone,
                    endTime = end, endZoneOffset = zone,
                    exerciseType = exerciseType,
                    title = title,
                    metadata = Metadata.manualEntry()
                )
            )
            distanceMeters?.let {
                add(
                    DistanceRecord(
                        startTime = start, startZoneOffset = zone,
                        endTime = end, endZoneOffset = zone,
                        distance = Length.meters(it),
                        metadata = Metadata.manualEntry()
                    )
                )
            }
        }
        client.insertRecords(records)
    }

    /**
     * Reads avg/max heart rate and total active calories over a window —
     * typically the Fitbit's data for a just-completed strength session.
     */
    suspend fun readSessionVitals(start: Instant, end: Instant): SessionVitals {
        val result = client.aggregate(
            AggregateRequest(
                metrics = setOf(
                    HeartRateRecord.BPM_AVG,
                    HeartRateRecord.BPM_MAX,
                    ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL
                ),
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        return SessionVitals(
            avgBpm = result[HeartRateRecord.BPM_AVG],
            maxBpm = result[HeartRateRecord.BPM_MAX],
            activeKcal = result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories
        )
    }
}
