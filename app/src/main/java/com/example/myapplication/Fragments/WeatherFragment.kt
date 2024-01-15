package com.example.myapplication.Fragments


import android.content.Context
import android.graphics.Rect
import android.location.Geocoder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.graphics.drawable.DrawableCompat
import com.example.myapplication.API.ApiInterface
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentWeatherBinding
import com.example.myapplication.models.Weather
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class WeatherFragment : Fragment() {
    lateinit var binding: FragmentWeatherBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentWeatherBinding.inflate(layoutInflater, container, false)

        // Inflate the layout for this fragment
        val view = binding.root

        binding.apply {
            fetachWeatherData("Cần Thơ")
            searchCity()
        }

        return view
    }

    private fun searchCity() {
        val list = resources.getStringArray(R.array.Province)
        val adt = ArrayAdapter(requireContext(),android.R.layout.simple_list_item_1,list)
        binding.searchView.setAdapter(adt)
        // Sự kiện khi chọn một item từ AutoCompleteTextView
        binding.searchView.setOnItemClickListener { parent, view, position, id ->
            val selectedProvince = parent.getItemAtPosition(position).toString()
            fetachWeatherData(selectedProvince)
            binding.searchView.setText("")
            binding.searchView.clearFocus()
        }
        // Sự kiện khi nhấn Enter trên bàn phím
        binding.searchView.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                val enteredText = (v as AutoCompleteTextView).text.toString()
                fetachWeatherData(enteredText)
                binding.searchView.setText("")
                binding.searchView.clearFocus()
                return@setOnKeyListener true
            }
            false
        }
        binding.searchView.setOnFocusChangeListener { v, hasFocus -> if(hasFocus) binding.searchView.showDropDown()}
    }

    private fun fetachWeatherData(cityName: String) {
        binding.progressBar.visibility = View.VISIBLE
        //Gọi api openweather
        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .build().create(ApiInterface::class.java)

        val response = retrofit.getWeatherData("$cityName, VN", "60eec28a3259753f5b12b76ce3f4f461", "metric")
        response.enqueue(object : Callback<Weather> {
            override fun onResponse(call: Call<Weather>, response: Response<Weather>) {
                val responseBody = response.body()
                if (response.isSuccessful && responseBody != null) {
                    val humid = responseBody.main.humidity
                    val winSpeed = responseBody.wind.speed
                    val condition = responseBody.weather.firstOrNull()?.main ?: "Unknown"
                    val vietnameseCondition = convertConditionToVietnamese(condition)
                    val sunRise = responseBody.sys.sunrise.toLong()
                    val sunSet = responseBody.sys.sunset.toLong()
                    val maxTemp = responseBody.main.temp_max
                    val minTemp = responseBody.main.temp_min
                    val temperature = responseBody.main.temp
                    binding.textView.text = "$temperature °C"
                    binding.textView3.text = "$vietnameseCondition"
                    binding.tvMax.text = "Nhiệt độ lớn nhất: $maxTemp°C"
                    binding.tvMin.text = "Nhiệt độ nhỏ nhất: $minTemp°C"
                    binding.tvApihumid.text = "$humid %"
                    binding.tvApiWinspeed.text = "$winSpeed m/s"
                    binding.tvApiSunrise.text = "${time(sunRise)} am"
                    binding.tvApiSunset.text = "${time(sunSet)} pm"
                    binding.location.text = "$cityName"

                    val calendar = Calendar.getInstance()
                    val dayOfWeek = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.ALL_STYLES, Locale.getDefault())
                    val formattedDate = SimpleDateFormat("d/M/yyyy", Locale.getDefault()).format(calendar.time)

                    binding.tvDay.text = dayOfWeek
                    binding.tvDate.text = formattedDate


                    Handler(Looper.getMainLooper()).postDelayed({
                        binding.progressBar.visibility = View.GONE
                        binding.content.visibility = View.VISIBLE
                        changeImg(condition)
                    }, 1000)

                }
                else{
                    val errorMessage = when {
                        response.code() == 404 -> {
                            "Tên bạn nhập không tìm thấy ở Việt Nam"
                        }
                        else -> "Lỗi truy xuất, vui lòng thử lại !!"
                    }

                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                }
            }

            override fun onFailure(call: Call<Weather>, t: Throwable) {
                val errorMessage = when (t) {
                    is IOException -> "Vui lòng kiểm tra kết nối"
                    else -> "Lỗi truy xuất, vui lòng thử lại !!"
                }

                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
            }
        })
    }
    private fun time(timestamp: Long): String{
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format((Date(timestamp*1000)))
    }

    private fun changeImg(condition: String) {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when(condition){
            "Clear Sky", "Sunny", "Clear"->{
                //Kiểm tra điều kiện để đổi thành ảnh nền ban đêm
                if (currentHour >= 18 || currentHour <= 6){
                    binding.root.setBackgroundResource(R.drawable.night)
                    binding.lottieAnimationView.setAnimation(R.raw.moon)
                    changeTextColor()
                }else{
                    binding.root.setBackgroundResource(R.drawable.sunny)
                    binding.lottieAnimationView.setAnimation(R.raw.sunny)
                }
            }

            "Haze", "Mist", "Partly Clouds", "Overcast", "Foggy", "Clouds" ->{
                if (currentHour >= 18 || currentHour <= 6){
                    binding.root.setBackgroundResource(R.drawable.night)
                    binding.lottieAnimationView.setAnimation(R.raw.nigthclouds)
                    changeTextColor()
                }else {
                    binding.root.setBackgroundResource(R.drawable.cloud_weather)
                    binding.lottieAnimationView.setAnimation(R.raw.clouds)
                }
            }

            "Light Rain", "Drizzle", "Moderate Rain", "Showers", "Heavy Rain", "Rain" ->{
                if (currentHour >= 18 || currentHour <= 6){
                    binding.root.setBackgroundResource(R.drawable.night)
                    binding.lottieAnimationView.setAnimation(R.raw.nightrain)
                    changeTextColor()
                }else {
                    binding.root.setBackgroundResource(R.drawable.rain_weather)
                    binding.lottieAnimationView.setAnimation(R.raw.rain)
                    changeTextColor()
                }
            }

            "Light Snow", "Moderate Snow", "Heavy Snow", "Blizzard", "Snow" ->{
                if (currentHour >= 18 || currentHour <= 6){
                    binding.root.setBackgroundResource(R.drawable.night)
                    binding.lottieAnimationView.setAnimation(R.raw.nigthsnow)
                    changeTextColor()
                }else {
                    binding.root.setBackgroundResource(R.drawable.snow_weather)
                    binding.lottieAnimationView.setAnimation(R.raw.snow)
                }
            }
        }
        binding.lottieAnimationView.playAnimation()
    }

    private fun changeTextColor(){
        val textViewList = listOf(
            binding.textView, binding.textView3, binding.tvMax, binding.tvMin,
            binding.tvApihumid, binding.tvApiWinspeed, binding.tvApiSunrise, binding.tvApiSunset,
            binding.location, binding.tvDay, binding.tvDate, binding.tvHumid, binding.tvWindspeed,
            binding.tvSunSet,binding.tvSunRise,binding.tvToday
        )
        val locationIcon = ContextCompat.getDrawable(requireContext(), R.drawable.baseline_location_on_24)
        DrawableCompat.setTint(locationIcon!!, ContextCompat.getColor(requireContext(), android.R.color.white))
        binding.location.setCompoundDrawablesWithIntrinsicBounds(locationIcon, null, null, null)
        for (textView in textViewList) {
            textView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        }
    }

    private fun convertConditionToVietnamese(condition: String): Any {
        return when (condition.toLowerCase()) {
            "clear" -> "Trời quang đãng"
            "clouds","Haze", "Partly Clouds", "Clouds", "Overcast", "Foggy" -> "Nhiều mây"
            "rain","Light Rain", "Drizzle", "Moderate Rain", "Showers", "Heavy Rain" -> "Mưa"
            "snow","Light Snow", "Moderate Snow", "Heavy Snow", "Blizzard" -> "Tuyết"
            "thunderstorm" -> "Dông"
            "mist" -> "Sương mù"
            else -> condition // Giữ nguyên nếu không có ánh xạ
        }
    }
}
