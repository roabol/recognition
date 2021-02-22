package br.com.sigmaonline.atividade.view.permission

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import br.com.sigmaonline.atividade.R
import java.util.*

class PermissionRationalActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If permissions granted, we start the main activity (shut this activity down).
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            finish()
        }

        setContentView(R.layout.activity_permission_rational)
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    fun onClickApprovePermissionRequest(view: View?) {
        Log.d(TAG, "onClickApprovePermissionRequest()")

        // TODO: Review permission request for activity recognition.
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), PermissionRationalActivity.PERMISSION_REQUEST_ACTIVITY_RECOGNITION)
    }

    fun onClickDenyPermissionRequest(view: View?) {
        Log.d(TAG, "onClickDenyPermissionRequest()")
        finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        val permissionResult = "Request code: " + requestCode + ", Permissions: " + Arrays.toString(permissions) + ", Results: " + Arrays.toString(grantResults)

        Log.d(TAG, "onRequestPermissionsResult(): $permissionResult")

        if (requestCode == PERMISSION_REQUEST_ACTIVITY_RECOGNITION) {
            // Close activity regardless of user's decision (decision picked up in main activity).
            finish()
        }
    }

    companion object {
        private const val TAG = "UnidadeEmpresaRepository"
        private const val PERMISSION_REQUEST_ACTIVITY_RECOGNITION: Int = 45
    }
}