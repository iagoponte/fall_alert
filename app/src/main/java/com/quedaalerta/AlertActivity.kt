package com.quedaalerta

import android.os.Bundle
import android.os.CountDownTimer
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.quedaalerta.databinding.ActivityAlertBinding

class AlertActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertBinding
    private var countDownTimer: CountDownTimer? = null

    private val COUNTDOWN_MILLIS = 30_000L
    private val COUNTDOWN_INTERVAL = 1_000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlertBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Garante que a tela acende mesmo com o dispositivo bloqueado
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        val lat       = intent.getDoubleExtra(FallDetectionService.EXTRA_LAT, 0.0)
        val lon       = intent.getDoubleExtra(FallDetectionService.EXTRA_LON, 0.0)
        val magnitude = intent.getFloatExtra("magnitude", 0f)

        binding.tvMagnitude.text = String.format("Magnitude detectada: %.1f m/s²", magnitude)

        if (lat != 0.0 && lon != 0.0) {
            binding.tvLocation.text = String.format("Localização: %.4f° / %.4f°", lat, lon)
        } else {
            binding.tvLocation.text = "Localização: não disponível"
        }

        binding.btnCancel.setOnClickListener {
            cancelAlert()
        }

        startCountdown(lat, lon)
    }

    private fun startCountdown(lat: Double, lon: Double) {
        countDownTimer = object : CountDownTimer(COUNTDOWN_MILLIS, COUNTDOWN_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                binding.tvCountdown.text = seconds.toString()
                binding.progressCountdown.progress = ((seconds.toFloat() / 30f) * 100).toInt()
            }

            override fun onFinish() {
                sendEmergencyAlert(lat, lon)
            }
        }.start()
    }

    private fun sendEmergencyAlert(lat: Double, lon: Double) {
        // Chamada correta para o método estático no companion object
        FallDetectionService.sendEmergencySms(this, lat, lon)

        binding.tvCountdown.text = "✓"
        binding.tvStatus.text = "SMS enviado para todos os contatos!"
        binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.green_700))
        binding.btnCancel.isEnabled = false

        // Fecha após 3 segundos
        binding.root.postDelayed({ finish() }, 3000)
    }

    private fun cancelAlert() {
        countDownTimer?.cancel()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    // Impede que o botão Voltar cancele sem interação explícita
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // intencional: não permite fechar com back — usuário deve tocar no botão
    }
}
