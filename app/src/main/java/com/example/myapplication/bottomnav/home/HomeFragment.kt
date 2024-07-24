package com.example.myapplication.bottomnav.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.myapplication.R
import com.example.myapplication.bottomnav.categories.CategoriesFragment
import com.example.myapplication.databinding.ActivityLoginBinding
import com.example.myapplication.databinding.FragmentHomeBinding
import com.shashank.sony.fancytoastlib.FancyToast

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: ImageSliderAdapter
    private val imageUrls = listOf(
        "https://images.fooby.ru/1/71/54/3526882",
        "https://images.fooby.ru/1/77/15/3807223",
        "https://upload.wikimedia.org/wikipedia/commons/0/04/Korpus_810.jpg",
        "https://tgkb5.ru/images/people_terapia/pdo_vhod1.jpg",
        "https://tgkb5.ru/images/people_terapia/810_viv.jpg",
        "https://tgkb5.ru/images/people_detstvo/det_corp.jpg",
        "https://across.ru/sites/default/files/styles/full_post/public/gkb_5_tolyatti.png?itok=w7KbXbiW"
    )
    private var currentPage = 0

    private val autoScrollRunnable = object : Runnable {
        override fun run() {
            if (adapter.itemCount > 0) {
                currentPage = (currentPage + 1) % adapter.itemCount
                viewPager.setCurrentItem(currentPage, true)
                handler.postDelayed(this, 3000)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPager = binding.viewPager
        adapter = ImageSliderAdapter(imageUrls)
        viewPager.adapter = adapter

        // Начинаем автоматическое прокручивание
        handler.postDelayed(autoScrollRunnable, 3000)

        // Настройка навигации к CalendarFragment
        binding.tvNavigateToCalendar.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, CategoriesFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(autoScrollRunnable)  // Останавливаем авто-прокручивание при уничтожении представления
        _binding = null
    }
}