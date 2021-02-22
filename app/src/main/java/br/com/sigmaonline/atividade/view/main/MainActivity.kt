package br.com.sigmaonline.atividade.view.main

import android.Manifest
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import br.com.sigmaonline.atividade.BuildConfig
import br.com.sigmaonline.atividade.R
import br.com.sigmaonline.atividade.logger.LogFragment
import com.google.android.gms.location.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var activityTrackingEnabled = false
    private var mPendingIntent2: PendingIntent? = null

    private lateinit var mLogFragment: LogFragment
    private lateinit var mBtnRequestUpdate: Button
    private lateinit var mPendingIntent1: PendingIntent
    private lateinit var mTransitionsReceiver: TransitionsReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mLogFragment = (supportFragmentManager.findFragmentById(R.id.log_fragment) as LogFragment?)!!
        mBtnRequestUpdate = findViewById(R.id.bt_request_update)
        mBtnRequestUpdate.isEnabled = false

        mPendingIntent1 = PendingIntent.getBroadcast(this@MainActivity, 0, Intent(TRANSITIONS_RECEIVER_ACTION), 0)

        mTransitionsReceiver = TransitionsReceiver()

        printToScreen("App inicializado")
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(mTransitionsReceiver, IntentFilter(TRANSITIONS_RECEIVER_ACTION))
    }

    override fun onStop() {
        unregisterReceiver(mTransitionsReceiver)
        super.onStop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Inicia activity recognition se a permissão foi concedida.
        if (activityRecognitionPermissionApproved() && !activityTrackingEnabled) {
            enableActivityTransitions()
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    fun onClickEnableOrDisableActivityRecognition(view: View) {
        mBtnRequestUpdate.isEnabled = (view as Button).text == "Liga"
        if (view.text == "Liga") {
            view.text = getString(R.string.desliga)
            enableActivityTransitions()
        } else {
            view.text = getString(R.string.liga)
            disableActivityTransitions()
            if (mPendingIntent2 != null) {
                onClickRequestUpdate(mBtnRequestUpdate)
            }
        }
    }

    fun onClickRequestUpdate(view: View) {
        if ((view as Button).text == getString(R.string.request_update_on)) {
            view.text = getString(R.string.request_update_off)
            mPendingIntent2 = PendingIntent.getBroadcast(this@MainActivity, 0, Intent(TRANSITIONS_RECEIVER_ACTION), 0)
            ActivityRecognition.getClient(this).requestActivityUpdates(Long.MIN_VALUE, mPendingIntent2!!)
        } else {
            view.text = getString(R.string.request_update_on)
            ActivityRecognition.getClient(this).removeActivityUpdates(mPendingIntent2!!)
            mPendingIntent2 = null
        }
    }

    fun onClickClearLog(@Suppress("UNUSED_PARAMETER") view: View) {
        val alertDialog: AlertDialog? = this.let {
            val builder = AlertDialog.Builder(it)
            builder.apply {
                setPositiveButton(R.string.sim) { _, _ ->
                    mLogFragment.getLogView().clearLog()
                }
                setNegativeButton(R.string.nao) { _, _ ->
                }
            }
            builder.setTitle(R.string.confirma)
            // Create the AlertDialog
            builder.create()
        }
        alertDialog?.show()
    }

    private fun enableActivityTransitions() {
        val task = ActivityRecognition.getClient(this).requestActivityTransitionUpdates(buildTransitionRequest(), mPendingIntent1)
        task.addOnSuccessListener {
            activityTrackingEnabled = true
            printToScreen(SimpleDateFormat("HH:mm:ss", Locale.US).format(Date()) + " Transitions Api was successfully registered.")
        }

        task.addOnFailureListener {
            printToScreen("Transitions Api could NOT be registered: $it")
        }
    }

    private fun disableActivityTransitions() {
        ActivityRecognition.getClient(this).removeActivityTransitionUpdates(mPendingIntent1)
            .addOnSuccessListener {
                activityTrackingEnabled = false
                mPendingIntent1.cancel()
                printToScreen(SimpleDateFormat("HH:mm:ss", Locale.US).format(Date()) + " Transitions successfully unregistered.")
            }
            .addOnFailureListener {
                printToScreen("Transitions could not be unregistered: $it")
            }
    }

    // region inner class - classe que escuta as respostas do SO sobre a mudança de estado do usuário
    inner class TransitionsReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            printToScreen(SimpleDateFormat("HH:mm:ss", Locale.US).format(Date()) + " onReceive called")

            if (!TextUtils.equals(TRANSITIONS_RECEIVER_ACTION, intent.action)) {
                printToScreen(SimpleDateFormat("HH:mm:ss", Locale.US).format(Date()) + " Received an unsupported action in TransitionsReceiver: action = " + intent.action)
                return
            }

            if (ActivityTransitionResult.hasResult(intent)) {
                val result = ActivityTransitionResult.extractResult(intent)
                if (result != null) {
                    for (event in result.transitionEvents) {
                        val info = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date()) +
                                " Transition: " + toActivityString(event.activityType) + " (" + toTransitionType(event.transitionType) + ")"

                        printToScreen(info)
                    }
                }
            }
        }
    }
    // endregion

    // region Verifica permissões ACTIVITY_RECOGNITION
    private fun activityRecognitionPermissionApproved(): Boolean {
        return if (runningQOrLater) {
            PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            true
        }
    }
    // endregion

    // region Métodos auxiliares
    private fun printToScreen(message: String) {
        mLogFragment.getLogView().println(message)
    }

    private fun buildTransitionRequest(): ActivityTransitionRequest {
        val transitions: MutableList<ActivityTransition> = ArrayList()
        transitions.add(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()
        )
        transitions.add(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        )
        transitions.add(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()
        )
        transitions.add(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        )
        transitions.add(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()
        )
        transitions.add(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        )
        return ActivityTransitionRequest(transitions)
    }

    private fun toActivityString(activity: Int): String {
        return when (activity) {
            DetectedActivity.STILL -> "STILL"
            DetectedActivity.WALKING -> "WALKING"
            DetectedActivity.IN_VEHICLE -> "IN_VEHICLE"
            else -> "UNKNOWN"
        }
    }

    private fun toTransitionType(transitionType: Int): String {
        return when (transitionType) {
            ActivityTransition.ACTIVITY_TRANSITION_ENTER -> "ENTER"
            ActivityTransition.ACTIVITY_TRANSITION_EXIT -> "EXIT"
            else -> "UNKNOWN"
        }
    }

    companion object {
        const val TRANSITIONS_RECEIVER_ACTION = BuildConfig.APPLICATION_ID + "TRANSITIONS_RECEIVER_ACTION"
        private val runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }
    // endregion

}