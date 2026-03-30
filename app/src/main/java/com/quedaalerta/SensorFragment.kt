package com.quedaalerta

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.quedaalerta.databinding.FragmentSensorBinding
import kotlin.math.sqrt

class SensorFragment : Fragment(), SensorEventListener {

    private var _binding: FragmentSensorBinding? = null
    private val binding get() = _binding!!

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        const val FALL_THRESHOLD = 25.0f
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSensorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        if (accelerometer == null) {
            binding.tvSensorStatus.text = "Acelerômetro não disponível neste dispositivo"
            binding.cardSensorData.visibility = View.GONE
        }

        updateLocation()
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt(x * x + y * y + z * z)

        _binding?.let { b ->
            b.tvAxisX.text    = String.format("%.2f m/s²", x)
            b.tvAxisY.text    = String.format("%.2f m/s²", y)
            b.tvAxisZ.text    = String.format("%.2f m/s²", z)
            b.tvMagnitude.text = String.format("%.2f m/s²", magnitude)

            val pct = ((magnitude / FALL_THRESHOLD) * 100).coerceAtMost(100f).toInt()
            b.progressMagnitude.progress = pct

            if (magnitude > FALL_THRESHOLD) {
                b.tvSensorStatus.text = "⚠ THRESHOLD EXCEDIDO"
                b.tvSensorStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.error)
                )
                b.progressMagnitude.setIndicatorColor(
                    ContextCompat.getColor(requireContext(), R.color.error)
                )
            } else {
                b.tvSensorStatus.text = "● Monitorando"
                b.tvSensorStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.green_700)
                )
                b.progressMagnitude.setIndicatorColor(
                    ContextCompat.getColor(requireContext(), R.color.primary)
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun updateLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                _binding?.let { b ->
                    if (location != null) {
                        b.tvGps.text = String.format(
                            "%.4f° / %.4f° (%.0fm acc.)",
                            location.latitude,
                            location.longitude,
                            location.accuracy
                        )
                    } else {
                        b.tvGps.text = "Aguardando sinal GPS..."
                    }
                }
            }
        } catch (e: SecurityException) {
            _binding?.tvGps?.text = "Permissão de localização necessária"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
