package com.ivan.asistente

import android.animation.ValueAnimator
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class VoiceConversationActivity : AppCompatActivity() {

    companion object {
        private var instance: VoiceConversationActivity? = null

        fun setState(state: String) {
            instance?.runOnUiThread {
                when (state) {
                    "listening" -> instance?.setListening()
                    "processing" -> instance?.setProcessing()
                    "speaking" -> instance?.setSpeaking()
                }
            }
        }
    }

    private lateinit var orb: View
    private lateinit var glow: View
    private lateinit var state: TextView
    private var animation: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_voice_conversation)

        instance = this
        orb = findViewById(R.id.voice_orb)
        glow = findViewById(R.id.voice_orb_glow)
        state = findViewById(R.id.voice_state)

        findViewById<ImageButton>(R.id.voice_close).setOnClickListener {
            closeConversation()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                closeConversation()
            }
        })

        playCallStartSound()
        setListening()
    }

    private fun closeConversation() {
        animation?.cancel()
        setResult(RESULT_OK)
        finish()
    }

    private fun playCallStartSound() {
        val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 45)
        tone.startTone(ToneGenerator.TONE_PROP_ACK, 125)
        tone.release()
    }

    private fun setListening() {
        state.text = "Escuchando…"
        startStateAnimation(1.035f, 0.72f, 1700L)
    }

    private fun setProcessing() {
        state.text = "Procesando…"
        startStateAnimation(1.045f, 0.90f, 1150L)
    }

    private fun setSpeaking() {
        state.text = "Daniela hablando…"
        startSpeakingAnimation()
    }

    private fun startStateAnimation(scale: Float, alpha: Float, duration: Long) {
        animation?.cancel()
        glow.alpha = alpha

        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val phase = it.animatedFraction
                val pulse = 1f + (scale - 1f) * kotlin.math.sin(phase * Math.PI).toFloat()
                orb.scaleX = pulse
                orb.scaleY = pulse
                glow.scaleX = pulse
                glow.scaleY = pulse
            }
        }
        animation = animator
        animator.start()
    }

    private fun startSpeakingAnimation() {
        animation?.cancel()

        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 720L
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val phase = it.animatedFraction
                val wave = kotlin.math.sin(phase * Math.PI * 2.0)
                val pulse = 1f + 0.075f * wave.toFloat()
                val glowPulse = 1f + 0.10f * wave.toFloat()
                orb.scaleX = pulse
                orb.scaleY = pulse
                glow.scaleX = glowPulse
                glow.scaleY = glowPulse
                val alphaWave = ((wave + 1.0) / 2.0).toFloat()
                glow.alpha = 0.72f + 0.24f * alphaWave
            }
        }
        animation = animator
        animator.start()
    }

    override fun onDestroy() {
        animation?.cancel()
        animation = null
        if (instance === this) instance = null
        super.onDestroy()
    }
}
