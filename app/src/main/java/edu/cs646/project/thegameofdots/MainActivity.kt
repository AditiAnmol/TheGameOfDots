package edu.cs646.project.thegameofdots

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import edu.cs646.project.thegameofdots.DotsView.DotsGridListener
import java.util.Locale
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

const val SHAKE_THRESHOLD = 500

class MainActivity : AppCompatActivity(), SensorEventListener {

    private val dotsGame = DotsGame.getInstance()
    private lateinit var dotsView: DotsView
    private lateinit var movesRemainingTextView: TextView
    private lateinit var scoreTextView: TextView
    private lateinit var soundEffects: SoundEffects

    //sensor
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastAcceleration = SensorManager.GRAVITY_EARTH

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        movesRemainingTextView = findViewById(R.id.moves_remaining_text_view)
        scoreTextView = findViewById(R.id.score_text_view)
        dotsView = findViewById(R.id.dots_view)

        findViewById<Button>(R.id.new_game_button).setOnClickListener { newGameClick() }

        dotsView.setGridListener(gridListener)

        soundEffects = SoundEffects.getInstance(applicationContext)

        //sensor
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        startNewGame()

        // For testing in the emulator
        scoreTextView.setOnClickListener { startNewGame() }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundEffects.release()
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this, accelerometer)
    }

    override fun onSensorChanged(event: SensorEvent) {

        // Get accelerometer values
        val x: Float = event.values[0]
        val y: Float = event.values[1]
        val z: Float = event.values[2]

        // Find magnitude of acceleration
        val currentAcceleration: Float = x * x + y * y + z * z

        // Calculate difference between 2 readings
        val delta = currentAcceleration - lastAcceleration

        // Save for next time
        lastAcceleration = currentAcceleration

        // Detect shake
        if (abs(delta) > SHAKE_THRESHOLD) {
            startNewGame()
        }

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Nothing to do
    }

    private val gridListener = object : DotsGridListener {
        override fun onDotSelected(dot: Dot, status: DotSelectionStatus) {
            // Ignore selections when game is over
            if (dotsGame.isGameOver) return

            // Play first tone when first dot is selected
            if (status == DotSelectionStatus.First) {
                soundEffects.resetTones()
            }

            // Select the dot and play the right tone
            val addStatus = dotsGame.processDot(dot)
            if (addStatus == DotStatus.Added) {
                soundEffects.playTone(true)
            } else if (addStatus == DotStatus.Removed) {
                soundEffects.playTone(false)
            }

            // If done selecting dots then replace selected dots and display new moves and score
            if (status === DotSelectionStatus.Last) {
                if (dotsGame.selectedDots.size > 1) {
                    dotsView.animateDots()

                    // These methods must be called AFTER the animation completes
                    //dotsGame.finishMove()
                    //updateMovesAndScore()
                } else {
                    dotsGame.clearSelectedDots()
                }
            }

            // Display changes to the game
            dotsView.invalidate()
        }

        override fun onAnimationFinished() {
            dotsGame.finishMove()
            dotsView.invalidate()
            updateMovesAndScore()

            if (dotsGame.isGameOver) {
                soundEffects.playGameOver()
            }
        }

    }

    private fun newGameClick() {
        //startNewGame()

        // Animate down off screen
        val screenHeight = this.window.decorView.height.toFloat()
        val moveBoardOff = ObjectAnimator.ofFloat(
            dotsView, "translationY", screenHeight)
        moveBoardOff.duration = 700
        moveBoardOff.start()

        moveBoardOff.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                startNewGame()

                // Animate from above the screen down to default location
                val moveBoardOn = ObjectAnimator.ofFloat(
                    dotsView, "translationY", -screenHeight, 0f)
                moveBoardOn.duration = 700
                moveBoardOn.start()
            }
        })

    }

    private fun startNewGame() {
        dotsGame.newGame()
        dotsView.invalidate()
        updateMovesAndScore()
    }

    private fun updateMovesAndScore() {
        movesRemainingTextView.text = String.format(Locale.getDefault(), "%d", dotsGame.movesLeft)
        scoreTextView.text = String.format(Locale.getDefault(), "%d", dotsGame.score)
    }
}