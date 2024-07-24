package com.example.myapplication


import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.myapplication.bottomnav.categories.CategoriesFragment
import com.example.myapplication.bottomnav.history.HistoryFragment
import com.example.myapplication.bottomnav.home.HomeFragment
import com.example.myapplication.bottomnav.profile.ProfileFragment
import com.example.myapplication.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import androidx.core.content.ContextCompat


@Suppress("UNUSED_EXPRESSION")
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding




    private val fragment1 = HomeFragment()
    private val fragment2 = ProfileFragment()
    private val fragment3 = HistoryFragment()
    private val fragment4 = CategoriesFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        window.navigationBarColor = ContextCompat.getColor(this, R.color.main)

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intent = Intent(this, LoginActivity::class.java)

        if(FirebaseAuth.getInstance().currentUser == null) {
            startActivity(intent)
        }



        binding.bottombar.setOnItemSelectedListener { menuItem ->
            when(menuItem.itemId){

                    R.id.home -> {
                        switchFragment(fragment1)
                        true
                    }
                    R.id.categories -> {
                        switchFragment(fragment4)
                        true
                    }
                    R.id.history -> {
                        switchFragment(fragment3)
                        true
                    }
                    R.id.profile -> {
                        switchFragment(fragment2)
                        true
                    }
                    else -> false

            }
         }

        switchFragment(fragment1)
    }

    private fun switchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(binding.fragmentContainer.id, fragment).commit()

    }

}