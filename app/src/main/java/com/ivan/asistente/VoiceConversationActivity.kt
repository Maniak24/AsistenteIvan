package com.ivan.asistente

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageButton
import android.widget.TextView
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
    private var animation: AnimatorSet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_voice_conversation)

        instance = this

        orb = findViewById(R.id.voice_orb)
        glow = findViewById(R.id.voice_orb_glow)
        state = findViewById(R.id.voice_state)

        findViewById<ImageButton>(R.id.voice_close).setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }

        playCallStartSound()
        setListening()
    }

    private fun playCallStartSound() {
        val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 45)
        tone.startTone(ToneGenerator.TONE_PROP_ACK, 125)
        tone.release()
    }

    private fun setListening() {
        state.text = "Escuchando…"
        startListeningAnimation()
    }

    private fun setProcessing() {
        state.text = "Procesando…"
        startProcessingAnimation()
    }

    private fun setSpeaking() {
        state.text = "Daniela hablando…"
        startSpeakingAnimation()
    }

    private fun cancelAnimation() {
        animation?.removeAllListeners()
        animation?.cancel()
        animation = null
        orb.animate().cancel()
        glow.animate().cancel()
    }

    private fun startListeningAnimation() {
        cancelAnimation()
        orb.scaleX = 1f
        orb.scaleY = 1f
        glow.scaleX = 1f
        glow.scaleY = 1f
        glow.alpha = 0.72f

        val orbX = ObjectAnimator.ofFloat(orb, View.SCALE_X, 1f, 1.035f, 1f)
        val orbY = ObjectAnimator.ofFloat(orb, View.SCALE_Y, 1f, 1.035f, 1f)
        val glowX = ObjectAnimator.ofFloat(glow, View.SCALE_X, 1f, 1.035f, 1f)
        val glowY = ObjectAnimator.ofFloat(glow, View.SCALE_Y, 1f, 1.035f, 1f)

        animation = AnimatorSet().apply {
            playTogether(orbX, orbY, glowX, glowY)
            duration = 1700
            interpolator = AccelerateDecelerateInterpolator()
            addListener(loopListener("listening"))
            start()
        }
    }

    private fun startProcessingAnimation() {
        cancelAnimation()
        val orbX = ObjectAnimator.ofFloat(orb, View.SCALE_X, 1f, 1.045f, 1f)
        val orbY = ObjectAnimator.ofFloat(orb, View.SCALE_Y, 1f, 1.045f, 1f)
        val glowAlpha = ObjectAnimator.ofFloat(glow, View.ALPHA, 0.52f, 0.9f, 0.52f)

        animation = AnimatorSet().apply {
            playTogether(orbX, orbY, glowAlpha)
            duration = 1150
            interpolator = AccelerateDecelerateInterpolator()
            addListener(loopListener("processing"))
            start()
        }
    }

    private fun startSpeakingAnimation() {
        cancelAnimation()
        val orbX = ObjectAnimator.ofFloat(orb, View.SCALE_X, 1f, 1.075f, 0.985f, 1.055f, 1f)
        val orbY = ObjectAnimator.ofFloat(orb, View.SCALE_Y, 1f, 1.075f, 0.985f, 1.055f, 1f)
        val glowX = ObjectAnimator.ofFloat(glow, View.SCALE_X, 1f, 1.10f, 0.98f, 1.07f, 1f)
        val glowY = ObjectAnimator.ofFloat(glow, View.SCALE_Y, 1f, 1.10f, 0.98f, 1.07f, 1f)
        val glowAlpha = ObjectAnimator.ofFloat(glow, View.ALPHA, 0.62f, 1f, 0.68f, 0.95f, 0.62f)

        animation = AnimatorSet().apply {
            playTogether(orbX, orbY, glowX, glowY, glowAlpha)
            duration = 900
            interpolator = AccelerateDecelerateInterpolator()
            addListener(loopListener("speaking"))
            start()
        }
    }

    private fun loopListener(expected: String) = object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animator: Animator) {
            if (state.text == when (expected) {
                    "listening" -> "Escuchando…"
                    "processing" -> "Procesando…"
                    else -> "Daniela hablando…"
                }) {
                when (expected) {
                    "listening" -> startListeningAnimation()
                    "processing" -> startProcessingAnimation()
                    "speaking" -> startSpeakingAnimation()
                }
            }
        }
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        setResult(RESULT_OK)
        finish()
    }

    override fun onDestroy() {
        cancelAnimation()
        if (instance === this) instance = null
        super.onDestroy()
    }
}
