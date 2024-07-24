package com.example.myapplication.bottomnav.categories

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.R
import com.bumptech.glide.request.RequestOptions
import com.example.myapplication.databinding.FragmentCategoriesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.shashank.sony.fancytoastlib.FancyToast
import java.util.Calendar

class CategoriesFragment : Fragment() {

    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseDatabase: FirebaseDatabase

    private var selectedCategory: String? = null
    private var selectedDoctor: Doctor? = null
    private var selectedDate: String? = null
    private var selectedTime: String? = null

    private val doctorsMap = mutableMapOf<String, Doctor>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance()

        loadCategories()

        binding.spinnerCategories.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                selectedCategory = parent.getItemAtPosition(position) as String
                selectedDoctor = null
                binding.tvDoctorInfo.text = ""
                loadDoctors(selectedCategory!!)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.spinnerDoctors.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val doctorName = parent.getItemAtPosition(position) as String
                selectedDoctor = doctorsMap[doctorName]
                displayDoctorInfo(selectedDoctor!!)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.btnSelectDatetime.setOnClickListener {
            showDateTimePicker()
        }

        binding.btnBookAppointment.setOnClickListener {
            bookAppointment()
        }
    }

    private fun loadCategories() {
        val categoriesRef = firebaseDatabase.getReference("Categories")
        categoriesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val categories = mutableListOf<String>()
                for (categorySnapshot in snapshot.children) {
                    val category = categorySnapshot.child("categoryName").getValue(String::class.java)
                    if (category != null) {
                        categories.add(category)
                    }
                }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerCategories.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                FancyToast.makeText(requireContext(), "Ошибка базы данных: $error", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show()
            }
        })
    }

    private fun loadDoctors(category: String) {
        val doctorsRef = firebaseDatabase.getReference("Doctors")
        doctorsRef.orderByChild("categoryName").equalTo(category).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                doctorsMap.clear()
                val doctorNames = mutableListOf<String>()
                for (doctorSnapshot in snapshot.children) {
                    val doctor = doctorSnapshot.getValue(Doctor::class.java)
                    if (doctor != null) {
                        doctorsMap[doctor.name] = doctor
                        doctorNames.add(doctor.name)
                    } else {
                        Log.e("loadDoctors", "Не удалось преобразовать: ${doctorSnapshot.value}")
                    }
                }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, doctorNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerDoctors.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                FancyToast.makeText(requireContext(), "Ошибка при загрузке данных врачей: ${error.message}", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show()
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun displayDoctorInfo(doctor: Doctor) {
        binding.tvDoctorInfo.text = """
        Имя: ${doctor.name}
        Адрес: ${doctor.adress}
        Кабинет: ${doctor.office}
    """.trimIndent()

        if (doctor.photoUrl.isNotEmpty()) {
            _binding?.let {
                Glide.with(this)
                    .load(doctor.photoUrl)
                    .apply(RequestOptions.placeholderOf(R.drawable.tooltip_frame_dark).error(R.drawable.notification_bg))
                    .into(it.ivDoctorPhoto)
            }
        } else {
            binding.ivDoctorPhoto.setImageResource(R.drawable.tooltip_frame_dark)
        }
    }

    private fun showDateTimePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
            val timePickerDialog = TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                selectedDate = "$dayOfMonth/${month + 1}/$year"
                selectedTime = "$hourOfDay:$minute"
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)
            timePickerDialog.show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        datePickerDialog.show()
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
    }

    private fun bookAppointment() {
        if (selectedCategory == null || selectedDoctor == null || selectedDate == null || selectedTime == null) {
            FancyToast.makeText(requireContext(), "Пожалуйста, заполните все поля", FancyToast.LENGTH_SHORT, FancyToast.WARNING, false).show()
            return
        }

        showLoading()

        val user = firebaseAuth.currentUser ?: return
        val usersRef = firebaseDatabase.getReference("Users").child(user.uid)
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userData = snapshot.getValue(User::class.java) ?: return
                val recordsRef = firebaseDatabase.getReference("Records")
                val appointmentDateTime = "$selectedDate $selectedTime ${selectedDoctor!!.name}"

                recordsRef.orderByChild("appointmentDateTime").equalTo(appointmentDateTime)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            hideLoading()
                            if (dataSnapshot.exists()) {
                                FancyToast.makeText(requireContext(), "На данное время и дату уже есть запись", FancyToast.LENGTH_SHORT, FancyToast.WARNING, false).show()
                            } else {
                                val record = Record(
                                    userFullName = userData.username,
                                    userPassport = userData.passport,
                                    userSnils = userData.snils,
                                    userOms = userData.oms,
                                    userPhoneNumber = userData.phoneNumber,
                                    doctorName = selectedDoctor!!.name,
                                    doctorSpecialization = selectedDoctor!!.specialization,
                                    doctorAdress = selectedDoctor!!.adress,
                                    doctorOffice = selectedDoctor!!.office,
                                    appointmentDate = selectedDate!!,
                                    appointmentTime = selectedTime!!,
                                    appointmentDateTime = appointmentDateTime
                                )

                                recordsRef.push().setValue(record).addOnCompleteListener {
                                    if (it.isSuccessful) {
                                        FancyToast.makeText(requireContext(), "Запись на прием успешно создана", FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, false).show()
                                        sendEmail(userData.email, record)
                                    } else {
                                        FancyToast.makeText(requireContext(), "Ошибка при создании записи", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show()
                                    }
                                }
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            hideLoading()
                            FancyToast.makeText(requireContext(), "Ошибка при проверке записи", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show()
                        }
                    })
            }

            override fun onCancelled(error: DatabaseError) {
                hideLoading()
                FancyToast.makeText(requireContext(), "Ошибка при получении данных пользователя", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show()
            }
        })
    }

    private fun sendEmail(email: String, record: Record) {
        val subject = "Запись на прием к врачу"
        val message = """
        Уважаемый(ая) ${record.userFullName},

        Вы успешно записаны на прием к врачу ${record.doctorName} (${record.doctorSpecialization}) в кабинет №${record.doctorOffice}.
        Дата и время приема: ${record.appointmentDate} в ${record.appointmentTime}0.
        Адрес: ${record.doctorAdress}.

        Спасибо за использование нашего сервиса.

        С уважением,
        Ваша Тольяттинская Клиническая Больница №5
    """.trimIndent()

        // Используем Intent для отправки email
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, message)
        }
        try {
            startActivity(Intent.createChooser(intent, "Отправка email..."))
        } catch (e: Exception) {
            FancyToast.makeText(requireContext(), "Не удалось отправить email", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class Doctor(
        val name: String = "",
        val adress: String = "",
        val experience: String = "",
        val rating: String = "",
        val office: String = "",
        val specialization: String = "",
        val photoUrl: String = ""
    )

    data class User(
        val username: String = "",
        val passport: String = "",
        val snils: String = "",
        val oms: String = "",
        val phoneNumber: String = "",
        val email: String = "",
        val categoryName: String = ""
    )

    data class Record(
        val userFullName: String,
        val userPassport: String,
        val userSnils: String,
        val userOms: String,
        val userPhoneNumber: String,
        val doctorName: String,
        val doctorSpecialization: String,
        val doctorAdress: String,
        val doctorOffice: String,
        val appointmentDate: String,
        val appointmentTime: String,
        val appointmentDateTime: String
    )
}


