package com.example.segundoapp

import com.example.segundoapp.LocationPermissionHelper
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.Manifest
import android.content.ContentValues.TAG
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.segundoapp.databinding.ActivityMainBinding
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.maps.android.heatmaps.HeatmapTileProvider
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build





class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private const val REQUEST_CODE = 123 // Valor
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var database: DatabaseReference
    private lateinit var googleMap: GoogleMap

    val db = FirebaseFirestore.getInstance()
    var handler = Handler(Looper.getMainLooper())
    var nivelRuido: Float = 0.0f
    val NIVEL_RUIDO_ALERTA: Float = 65.00f
    var userLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clearCache(applicationContext)
        setContentView(R.layout.activity_main)

        LocationPermissionHelper.checkLocationPermission(this) {

        }
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager


        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapViewContainer) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
        if (mapFragment == null) {
            val newMapFragment = SupportMapFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .add(R.id.mapViewContainer, newMapFragment)
                .commit()
            newMapFragment.getMapAsync(this)
        } else {
            mapFragment.getMapAsync(this)
        }

        val runnable = Runnable {
            // Código a ser executado na thread principal
            consulta()
            getCurrentLocation()
        }
        // Executa o código na thread principal após um atraso de 10 segundos para nao lotar o firebase de requisições e estourar o limite
        handler.postDelayed(runnable, 10000)
    }


    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        val zoomlevel = 15.0f
        val PUCC = LatLng(-22.834788, -47.049731)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(PUCC, zoomlevel))
    }

    fun consulta() {
        val collectionRef = db.collection("IntegrasData")
        val dadosList = mutableListOf<IntegrasData>()
        val latLngList = mutableListOf<LatLng>()

        collectionRef.get()
            .addOnSuccessListener { querySnapshot ->
                for (documentSnapshot in querySnapshot) {
                    val nivelRuido = documentSnapshot.getDouble("SPL")?.toFloat()
                    val dataFirestore = documentSnapshot.getDate("data")
                    val latitude = documentSnapshot.getDouble("Latitude")
                    val longitude = documentSnapshot.getDouble("Longitude")
                    // Acesso aos dados de cada documento retornado
                    val data = documentSnapshot.data
                    // Lógica para manipular os dados
                    if (nivelRuido != null && latitude != null && longitude != null) {
                        val dados = IntegrasData(nivelRuido, latitude, longitude)
                        val latLng = LatLng(latitude, longitude)
                        latLngList.add(latLng)
                        dadosList.add(dados)
                        verificarNivelRuidoEPosicao(latitude, longitude)
                    }

                }
                for (dados in dadosList) {
                    Log.d(
                        TAG, "Nivel de ruido: ${dados.nivelRuido}\n" +
                                "Latitude: ${dados.latitude}\n" +
                                "Longitude: ${dados.longitude}"
                    )
                }
                val heatMap = HeatmapTileProvider.Builder()
                    .data(latLngList)
                    .build()

                googleMap.addTileOverlay(TileOverlayOptions().tileProvider(heatMap))

            }

            .addOnFailureListener { exception ->
                "Erro ao tentar consultar o banco de dados na coleção ${collectionRef}: ${exception}"
                // Lógica para lidar com erros
            }

    }

    fun clearCache(context: Context) {
        context.cacheDir?.let { cacheDir ->
            if (cacheDir.exists()) {
                val files = cacheDir.listFiles()
                if (files != null) {
                    for (file in files) {
                        file.delete()
                    }
                }
            }
        }
    }


    private fun getCurrentLocation() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permissão de localização concedida

            // Obter a localização atual
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0,
                0f,
                object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        // Aqui está a posição atual do usuário
                        val latitude = location.latitude
                        val longitude = location.longitude
                        userLocation = location
                        // Faça o que for necessário com a posição atual
                        // ...

                        // Lembre-se de parar as atualizações de localização quando não precisar mais delas
                        locationManager.removeUpdates(this)
                    }

                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                }
            )
        } else {
            // Permissão de localização não concedida, solicite ao usuário
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_CODE
            )
        }
    }


    private fun verificarNivelRuidoEPosicao(latitude: Double, longitude: Double) {
        // Verifique os níveis de ruído e posição do usuário e emita alertas conforme necessário

        if (userLocation != null && userLocation!!.latitude == latitude && userLocation!!.longitude == longitude && nivelRuido >= NIVEL_RUIDO_ALERTA) {
            emitirAlertaRuido()
        }
    }

    fun emitirAlertaRuido(){
        val notificationId = 1

        // Crie um objeto NotificationCompat.Builder para construir a notificação
        val builder = NotificationCompat.Builder(this, "channel_id")
            .setSmallIcon(R.drawable.sound)
            .setContentTitle("Alerta de ruído")
            .setContentText("Nível de ruído ultrapassado!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // Crie um objeto PendingIntent para abrir a atividade desejada ao clicar na notificação
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setContentIntent(pendingIntent)

        // Obtenha uma referência ao NotificationManager
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Verifique se o dispositivo está executando o Android 8.0 (Oreo) ou posterior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Crie um canal de notificação para dispositivos Android 8.0 e posterior
            val channel = NotificationChannel(
                "channel_id",
                "Nome do Canal",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
        // Construa e exiba a notificação usando o NotificationManager
        notificationManager.notify(notificationId, builder.build())

    }

}


