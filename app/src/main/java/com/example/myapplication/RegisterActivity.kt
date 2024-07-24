package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.shashank.sony.fancytoastlib.FancyToast

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val mainActivityIntent = Intent(this, MainActivity::class.java)
        val LoginActivityIntent = Intent(this, LoginActivity::class.java)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backBtn.setOnClickListener {
            startActivity(LoginActivityIntent)
        }

        binding.registerBtn.setOnClickListener {
            val email = binding.emailEt.text.toString().trim()
            val password = binding.passwordEt.text.toString().trim()
            val username = binding.usernameEt.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
                FancyToast.makeText(
                    this,
                    "Поля не могут быть пустыми",
                    FancyToast.LENGTH_SHORT,
                    FancyToast.WARNING,
                    false
                ).show()
            } else {
                // Проверка существования пользователя с указанной почтой
                FirebaseAuth.getInstance().fetchSignInMethodsForEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val signInMethods = task.result?.signInMethods ?: emptyList()
                            if (signInMethods.isNotEmpty()) {
                                // Пользователь с такой почтой уже зарегистрирован
                                FancyToast.makeText(
                                    this,
                                    "Пользователь с этой почтой уже зарегистрирован",
                                    FancyToast.LENGTH_SHORT,
                                    FancyToast.ERROR,
                                    false
                                ).show()
                            } else {
                                // Регистрация нового пользователя
                                FirebaseAuth.getInstance()
                                    .createUserWithEmailAndPassword(email, password)
                                    .addOnCompleteListener { registrationTask ->
                                        if (registrationTask.isSuccessful) {
                                            val userInfo = hashMapOf(
                                                "email" to email,
                                                "username" to username,
                                                "profileImage" to ""
                                            )
                                            FirebaseDatabase.getInstance().reference
                                                .child("Users")
                                                .child(FirebaseAuth.getInstance().currentUser!!.uid)
                                                .setValue(userInfo)
                                                .addOnSuccessListener {
                                                    startActivity(mainActivityIntent)
                                                    FancyToast.makeText(
                                                        this,
                                                        "Вы успешно зарегистрировались!",
                                                        FancyToast.LENGTH_LONG,
                                                        FancyToast.SUCCESS,
                                                        false
                                                    ).show()
                                                }
                                        } else {
                                            FancyToast.makeText(
                                                this,
                                                "Пользователь с такой почтой уже зарегестрирован!",
                                                FancyToast.LENGTH_SHORT,
                                                FancyToast.ERROR,
                                                false
                                            ).show()
                                        }
                                    }
                            }
                        } else {
                            FancyToast.makeText(
                                this,
                                "Ошибка при проверке почты: ${task.exception?.message}",
                                FancyToast.LENGTH_SHORT,
                                FancyToast.ERROR,
                                false
                            ).show()
                        }
                    }
            }
        }
    }
}