package com.example.segundoapp
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object LocationPermissionHelper {
    private const val LOCATION_PERMISSION_REQUEST_CODE = 1

    fun checkLocationPermission(context: Context, onPermissionGranted: () -> Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permissão concedida, chama a função de retorno de chamada
            onPermissionGranted.invoke()
        } else {
            // Solicitar permissão ao usuário
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    fun handlePermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onPermissionGranted: () -> Unit,
        onPermissionDenied: () -> Unit
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão concedida, chama a função de retorno de chamada
                onPermissionGranted.invoke()
            } else {
                // Permissão negada, chama a função de retorno de chamada
                onPermissionDenied.invoke()
            }
        }
    }
}
