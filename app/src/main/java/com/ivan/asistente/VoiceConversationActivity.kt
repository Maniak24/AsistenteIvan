package com.ivan.asistente

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
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
        setContentView(R.layout.activity_voice_conversation)

        instance = this

        orb = findViewById(R.id.voice_orb)
        glow = findViewById(R.id.voice_orb_glow)
        state = findViewById(R.id.voice_state)

        findViewById<ImageButton>(R.id.voice_close).setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }

        startListeningAnimation()
    }

    fun setListening() {
        state.text = "Escuchando…"
        startListeningAnimation()
    }

    fun setProcessing() {
        state.text = "Analizando…"
        startProcessingAnimation()
    }

    fun setSpeaking() {
        state.text = "Daniela hablando…"
        startSpeakingAnimation()
    }

    private fun startListeningAnimation() {
        animation?.cancel()

        val scaleX = ObjectAnimator.ofFloat(orb, View.SCALE_X, 1f, 1.06f, 1f)
        val scaleY = ObjectAnimator.ofFloat(orb, View.SCALE_Y, 1f, 1.06f, 1f)

        animation = AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 1300
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    private fun startProcessingAnimation() {
        animation?.cancel()

        val rotate = ObjectAnimator.ofFloat(orb, View.ROTATION, 0f, 360f)

        val scaleX = ObjectAnimator.ofFloat(
            orb,
            View.SCALE_X,
            1f, 1.05f, 1f
        )

        val scaleY = ObjectAnimator.ofFloat(
            orb,
            View.SCALE_Y,
            1f, 1.05f, 1f
        )

        animation = AnimatorSet().apply {
            playTogether(rotate, scaleX, scaleY)
            duration = 1600
            start()
        }

        animation?.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                if (state.text == "Analizando…") {
                    startProcessingAnimation()
                }
            }
        })
    }

    private fun startSpeakingAnimation() {
        animation?.cancel()

        val orbX = ObjectAnimator.ofFloat(
            orb,
            View.SCALE_X,
            1f, 1.12f, 0.96f, 1.08f, 1f
        )

        val orbY = ObjectAnimator.ofFloat(
            orb,
            View.SCALE_Y,
            1f, 1.12f, 0.96f, 1.08f, 1f
        )

        val glowX = ObjectAnimator.ofFloat(
            glow,
            View.SCALE_X,
            1f, 1.08f, 0.98f, 1.06f, 1f
        )

        val glowY = ObjectAnimator.ofFloat(
            glow,
            View.SCALE_Y,
            1f, 1.08f, 0.98f, 1.06f, 1f
        )

        val glowAlpha = ObjectAnimator.ofFloat(
            glow,
            View.ALPHA,
            0.65f, 1f, 0.75f, 1f
        )

        animation = AnimatorSet().apply {
            playTogether(orbX, orbY, glowX, glowY, glowAlpha)
            duration = 850
            start()
        }

        animation?.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                if (state.text == "Daniela hablando…") {
                    startSpeakingAnimation()
                }
            }
        })
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        setResult(RESULT_OK)
        finish()
    }

    override fun onDestroy() {
        animation?.cancel()

        if (instance === this) {
            instance = null
        }

        super.onDestroy()
    }
}
