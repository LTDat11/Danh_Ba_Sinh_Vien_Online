package com.example.myapplication.Fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import com.example.myapplication.API.ApiInterface
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentWeatherBinding
import com.example.myapplication.models.Weather
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Calendar
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
        val searchView = binding.searchView
        searchView.setOnQueryTextListener(object :SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    fetachWeatherData(query)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }

        })
    }

    private fun fetachWeatherData(cityName: String) {
        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .build().create(ApiInterface::class.java)

        val response = retrofit.getWeatherData("$cityName", "60eec28a3259753f5b12b76ce3f4f461", "metric")
        response.enqueue(object : Callback<Weather> {
            override fun onResponse(call: Call<Weather>, response: Response<Weather>) {
                val responseBody = response.body()
                if (response.isSuccessful && responseBody != null) {
                    val humid = responseBody.main.humidity
                    val winSpeed = responseBody.wind.speed
                    val condition = responseBody.weather.firstOrNull()?.main ?: "Unknown"
                    val vietnameseCondition = convertConditionToVietnamese(condition)
                    val maxTemp = responseBody.main.temp_max
                    val minTemp = responseBody.main.temp_min
                    val temperature = responseBody.main.temp
                    binding.textView.text = "$temperature °C"
                    binding.textView3.text = "$vietnameseCondition"
                    binding.tvMax.text = "Nhiệt độ lớn nhất: $maxTemp°C"
                    binding.tvMin.text = "Nhiệt độ nhỏ nhất: $minTemp°C"
                    binding.tvApihumid.text = "$humid %"
                    binding.tvApiWinspeed.text = "$winSpeed m/s"
                    binding.location.text = "$cityName"

                    val calendar = Calendar.getInstance()
                    val dayOfWeek = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.ALL_STYLES, Locale.getDefault())
                    val formattedDate = SimpleDateFormat("d/M/yyyy", Locale.getDefault()).format(calendar.time)

                    binding.tvDay.text = dayOfWeek
                    binding.tvDate.text = formattedDate

                    changeImg(condition)
                }
            }

            override fun onFailure(call: Call<Weather>, t: Throwable) {
                // Handle failure if needed
            }
        })
    }

    private fun changeImg(condition: String) {
        when(condition){
            "Clear Sky", "Sunny", "Clear", "Clouds" ->{
                binding.root.setBackgroundResource(R.drawable.sunny)
                binding.lottieAnimationView.setAnimation(R.raw.sunny)
            }

            "Haze", "Mist", "Partly Clouds", "Overcast", "Foggy" ->{
                binding.root.setBackgroundResource(R.drawable.cloud_weather)
                binding.lottieAnimationView.setAnimation(R.raw.cloud)
            }

            "Light Rain", "Drizzle", "Moderate Rain", "Showers", "Heavy Rain", "Rain" ->{
                binding.root.setBackgroundResource(R.drawable.rain_weather)
                binding.lottieAnimationView.setAnimation(R.raw.rain)
            }

            "Light Snow", "Moderate Snow", "Heavy Snow", "Blizzard", "Snow" ->{
                binding.root.setBackgroundResource(R.drawable.snow_weather)
                binding.lottieAnimationView.setAnimation(R.raw.snow)
            }

        }
        binding.lottieAnimationView.playAnimation()
    }

    private fun convertConditionToVietnamese(condition: String): Any {
        return when (condition.toLowerCase()) {
            "clear" -> "Trời quang đãng"
            "clouds","Haze", "Mist", "Partly Clouds", "Clouds", "Overcast", "Foggy" -> "Nhiều mây"
            "rain","Light Rain", "Drizzle", "Moderate Rain", "Showers", "Heavy Rain" -> "Mưa"
            "snow","Light Snow", "Moderate Snow", "Heavy Snow", "Blizzard" -> "Tuyết"
            "thunderstorm" -> "Dông"
            "mist" -> "Sương mù"
            else -> condition // Giữ nguyên nếu không có ánh xạ
        }
    }
}
