package org.techtown.trashcan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.GroundOverlayOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import org.json.JSONArray
import org.json.JSONObject
import androidx.cardview.widget.CardView
import com.google.android.gms.location.*

import com.google.android.gms.maps.model.Marker
import org.json.JSONException

//data class Trash(val add : String, val add_type : String, val trash_type : String,
//                 val lat : Double, val lng : Double, val distance : Double)


@Suppress("deprecation")
class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null

    lateinit var mLastLocation: Location
    internal lateinit var mLocationRequest: LocationRequest
    private val REQUEST_PERMISSION_LOCATION = 10

    lateinit var gMap: GoogleMap
    lateinit var mapFrag: MapFragment
    lateinit var videoMark: GroundOverlayOptions

    var lats = Array<Double>(100, {0.0})
    var lngs = Array<Double>(100, {0.0})

    var lat: Double =0.0
    var lng: Double = 0.0
    var dis: Double = 0.0
    var dataLength: Int = 0
    lateinit var button: Button
    lateinit var button2: Button
//    lateinit var text1: TextView
//    lateinit var text2: TextView
//    lateinit var text3: TextView
    lateinit var lv: ListView
    lateinit var card_view : CardView
    lateinit var card_view2 : CardView
    var nearby_bin = mutableListOf<TrashModel?>()
    lateinit var nearBinRVAdapter: NearBinListLVAdapter

    private lateinit var binding:MainActivity


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.setIcon(R.drawable.bin)
        title = "  쓰레기를 찾아서"

        card_view = findViewById<CardView>(R.id.card_view)
        card_view2 = findViewById<CardView>(R.id.nearbyBin_card_view)
        card_view.visibility = View.GONE
        card_view2.visibility = View.GONE

        mapFrag = fragmentManager.findFragmentById(R.id.map) as MapFragment
        mapFrag.getMapAsync(this)

        //
        button = findViewById<Button>(R.id.button)
        button2 = findViewById<Button>(R.id.button2)
//        text1 = findViewById<TextView>(R.id.text1)
//        text2 = findViewById<TextView>(R.id.text2)
//        text3 = findViewById<TextView>(R.id.JSONtext)
        lv = findViewById<ListView>(R.id.nearbyBinListView)

        nearBinRVAdapter = NearBinListLVAdapter(nearby_bin)
        lv.adapter = nearBinRVAdapter

        mLocationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        button.setOnClickListener {
            card_view2.visibility = View.GONE

            if  (checkPermissionForLocation(this)) {
                startLocationUpdates()
                jsonLoader()
                gMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(this.lat, this.lng)))
            }
        }

        button2.setOnClickListener {
            if  (checkPermissionForLocation(this)) {
                startLocationUpdates()
                jsonLoader()
                showNearbyBin()
            }
        }



    }


    override fun onMapReady(map: GoogleMap) {

        // 구글 맵 객체를 불러온다.
        val json = assets.open("data.json").reader().readText()
        val data = JSONObject(json).getJSONArray("trash")


        gMap = map
        gMap.uiSettings.isZoomControlsEnabled = true
        gMap.setOnMapClickListener { point ->
            videoMark = GroundOverlayOptions().image(
                BitmapDescriptorFactory.fromResource(R.drawable.presence_video_busy))
                .position(point, 100f, 100f)
            gMap.addGroundOverlay(videoMark)
        }


        // 카메라를 한국성서대 위치로 옮긴다.
//        gMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(37.649279918895, 127.064085591)))


        val markerOptions = MarkerOptions()
        var marker : Marker
        //마커 생성
        for(index in 0 until data.length()){
            val iObj = data.getJSONObject(index)

            var add = iObj.getString("add")
            var trashType = iObj.getString("trash_tpye")
            var add_type = iObj.getString("add_type")

            if(!iObj.isNull("lat") && !iObj.isNull("lng")){
                //lat와 lng값이 있는 경우
                var lat = iObj.getString("lat").toDouble()
                var lng = iObj.getString("lng").toDouble()

                markerOptions
                    .position(LatLng(lat, lng))
                    .title(add.toString() + " (" + add_type.toString() + ")")
                    .snippet(trashType.toString())
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.trash_bin))



                marker = gMap.addMarker(markerOptions)
                marker.tag = add + "/" +trashType

                Log.d("마커", markerOptions.title.toString())
            }

            //Log.d("JSON", iObj.toString())

        }

        //마커 클릭 리스너-마커 클릭하면 카드뷰 띄움
        gMap.setOnMarkerClickListener(object : GoogleMap.OnMarkerClickListener {
            override fun onMarkerClick(marker: Marker): Boolean {
                card_view.visibility = View.VISIBLE
                card_view2.visibility = View.GONE

                var trashcan_loc = findViewById<TextView>(R.id.trashcan_loc)
                var trashcan_type = findViewById<TextView>(R.id.trashcan_type)

                trashcan_loc.text = marker.title
                trashcan_type.text = marker.snippet

                return false
            }
        })

        //맵 클릭 리스너 - 맵을 클릭하면 카드뷰가 없어진다.
        gMap.setOnMapClickListener(object : GoogleMap.OnMapClickListener {
            override fun onMapClick(latLng: LatLng) {
                card_view.visibility = View.GONE
                card_view2.visibility = View.GONE
            }
        })

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menu.add(0, 1, 0, "위성 지도")
        menu.add(0, 2, 0, "일반 지도")

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            1 -> {
                gMap.mapType = GoogleMap.MAP_TYPE_HYBRID
                return true
            }
            2 -> {
                gMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                return true
            }

        }
        return false
    }

    private fun startLocationUpdates() {
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        mFusedLocationProviderClient!!.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())
    }

    private val mLocationCallback = object: LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation
            locationResult.lastLocation?.let { onLocationChanged(it) }
        }
    }

    fun onLocationChanged(location: Location) {
        mLastLocation = location
        this.lat = mLastLocation.latitude
        this.lng = mLastLocation.longitude
//        text2.text = "위도 : " + this.lat
//        text1.text = "경도 : " + this.lng

        Log.d("테스트", this.lat.toString() + " and " + this.lng.toString())

    }

    private fun checkPermissionForLocation(context: Context): Boolean {
        // Android 6.0 Marshmallow 이상에서는 위치 권한에 추가 런타임 권한이 필요
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                true
            } else {
                // 권한이 없으므로 권한 요청 알림 보내기
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_PERMISSION_LOCATION)
                false
            }
        } else {
            true
        }
    }


    private fun jsonLoader(){
        var i = 0
        var lat = Array(100, {0})
        var lng = Array(100, {0})
        val dists: Array<Double> = emptyArray()
        var tempStr = ""

        val jsonStr = assets.open("data.json").reader().readText()
        Log.d("jsonString", jsonStr)

        val locationJson = JSONObject(jsonStr)

        val jsonArray: JSONArray = locationJson.optJSONArray("trash")

//        this.dataLength = jsonArray.length()
        Log.i("JsonArray", jsonArray.toString())

        nearby_bin.clear() //근처 쓰레기통 목록 초기화

        for (i in 0..(jsonArray.length() -1)) {
            try {

                val latStr = jsonArray.getJSONObject(i).getString("lat")
                val lngStr = jsonArray.getJSONObject(i).getString("lng")
                val addStr = jsonArray.getJSONObject(i).getString("add")
                val addTypeStr = jsonArray.getJSONObject(i).getString("add_type")
                val trashTypeStr = jsonArray.getJSONObject(i).getString("trash_tpye")


//                this.lats.set(i, latStr.toDouble())
//                this.lngs.set(i, lngStr.toDouble())

                val lat = latStr.toDouble()
                val lng = lngStr.toDouble()

                Haversine(this.lat, this.lng, lat, lng)
                tempStr += "거리 ${this.dis.toString()}\n"
                dists.plus(this.dis)

                val trash_data = TrashModel(addStr, addTypeStr, trashTypeStr, lat, lng, this.dis.toDouble())
                nearby_bin.add(trash_data)

//                Log.d("로그", "왜안되는거지?")
//                Log.d("로그", nearby_bin.toString())
//                Log.d("로그", trash_data.toString())



            } catch (e: JSONException){
                Log.d("로그","err")
//                Log.d("로그", e.toString())
            }
//            text3.text = tempStr

        }

    }



    private fun Haversine(x1: Double, y1: Double, x2: Double, y2: Double) {
        var distance = 0.0
        var radius = 6371.0
        var toRadian = Math.PI / 180

        val deltaLatitude = Math.abs(x1 - x1) * toRadian
        val deltaLongitude = Math.abs(y1 -y2) * toRadian

        val sinDeltaLat = Math.sin(deltaLatitude /2)
        val sinDeltaLng = Math.sin(deltaLongitude /2)
        val squarRoot = Math.sqrt(
            sinDeltaLat * sinDeltaLat +
                    Math.cos(x1 * toRadian) * Math.cos(x2 * toRadian) * sinDeltaLng * sinDeltaLng
        )

        this.dis = 2 * radius * Math.asin(squarRoot)
        //Log.d("dis", this.dis.toString())
        //text3.text = this.dis.toString()
    }


    private fun showNearbyBin () {

        try{
            nearBinRVAdapter.notifyDataSetChanged()

            nearby_bin.sortBy { it?.distance }
            card_view2.visibility = View.VISIBLE

            lv.setOnItemClickListener{parent, view, position, id ->
                gMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(nearby_bin[position]!!.lat, nearby_bin[position]!!.lng)))
            }

        } catch (e: java.lang.Exception){
            Log.d("결과", e.toString())
        }


    }

}
