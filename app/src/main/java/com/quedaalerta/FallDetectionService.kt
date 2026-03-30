package com.quedaalerta

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlin.math.sqrt

class FallDetectionService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Estado de debounce: evita múltiplas detecções seguidas
    private var lastFallTime = 0L
    private val FALL_COOLDOWN_MS = 60_000L      // 1 minuto entre alertas
    private val FALL_THRESHOLD   = 25.0f         // m/s² — valor empiricamente validado

    companion object {
        const val CHANNEL_ID   = "quedaalerta_channel"
        const val NOTIF_ID     = 1
        const val ACTION_ALERT = "com.quedaalerta.FALL_DETECTED"
        const val EXTRA_LAT    = "latitude"
        const val EXTRA_LON    = "longitude"

        /**
         * Envia SMS de emergência para todos os contatos cadastrados.
         * Movido para o companion object para ser acessível sem instanciar o Service.
         */
        fun sendEmergencySms(context: Context, lat: Double, lon: Double) {
            val contacts = ContactsManager.getContacts(context)
            if (contacts.isEmpty()) return

            val mapsLink = if (lat != 0.0 && lon != 0.0)
                "https://maps.google.com/?q=$lat,$lon"
            else
                "(localização indisponível)"

            val message = "⚠ QUEDA DETECTADA\n" +
                    "QuedaAlerta identificou uma possível queda.\n" +
                    "Localização: $mapsLink"

            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            contacts.forEach { contact ->
                try {
                    smsManager?.sendTextMessage(contact.phone, null, message, null, null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        sensorManager        = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer        = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        fusedLocationClient  = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildForegroundNotification())
        accelerometer?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ─── Detecção de queda ────────────────────────────────────────────────────

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt(x * x + y * y + z * z)

        val now = System.currentTimeMillis()
        if (magnitude > FALL_THRESHOLD && now - lastFallTime > FALL_COOLDOWN_MS) {
            lastFallTime = now
            onFallDetected(magnitude)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // ─── Lógica de alerta ─────────────────────────────────────────────────────

    private fun onFallDetected(magnitude: Float) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                val lat = location?.latitude  ?: 0.0
                val lon = location?.longitude ?: 0.0
                launchAlertActivity(lat, lon, magnitude)
            }.addOnFailureListener {
                launchAlertActivity(0.0, 0.0, magnitude)
            }
        } catch (e: SecurityException) {
            launchAlertActivity(0.0, 0.0, magnitude)
        }
    }

    private fun launchAlertActivity(lat: Double, lon: Double, magnitude: Float) {
        val intent = Intent(this, AlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_LAT, lat)
            putExtra(EXTRA_LON, lon)
            putExtra("magnitude", magnitude)
        }
        startActivity(intent)
    }

    // ─── Notificação persistente ──────────────────────────────────────────────

    private fun buildForegroundNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("QuedaAlerta ativo")
            .setContentText("Monitorando acelerômetro em segundo plano")
            .setSmallIcon(R.drawable.ic_shield)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Monitoramento QuedaAlerta",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notificação de serviço em segundo plano"
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }
}
