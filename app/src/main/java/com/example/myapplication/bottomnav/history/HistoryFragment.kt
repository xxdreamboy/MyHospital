package com.example.myapplication.bottomnav.history

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentHistoryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.shashank.sony.fancytoastlib.FancyToast

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        fetchCurrentUserAndRecords()
        return binding.root
    }

    private fun fetchCurrentUserAndRecords() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e("HistoryFragment", "User not authenticated")
            return
        }

        val userId = currentUser.uid
        val userDatabaseReference = FirebaseDatabase.getInstance().getReference("Users").child(userId)

        userDatabaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.child("username").getValue(String::class.java)
                if (username != null) {
                    fetchRecords(username)
                } else {
                    Log.e("HistoryFragment", "Username not found")
                    FancyToast.makeText(requireContext(), "Пользователь не найден", FancyToast.LENGTH_SHORT, FancyToast.WARNING, false).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HistoryFragment", "Database error: ${error.message}")
                FancyToast.makeText(requireContext(), "Ошибка при получении данных пользователя: ${error.message}", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false)
            }
        })
    }

    private fun fetchRecords(username: String) {
        val recordsDatabaseReference = FirebaseDatabase.getInstance().getReference("Records")

        recordsDatabaseReference.orderByChild("userFullName").equalTo(username)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (dataSnapshot in snapshot.children) {
                            val record = dataSnapshot.getValue(Record::class.java)
                            if (record != null) {
                                addRecordToView(record)
                            } else {
                                Log.e("HistoryFragment", "Record is null")
                                FancyToast.makeText(requireContext(), "Записи не найдены", FancyToast.LENGTH_SHORT, FancyToast.WARNING, false).show()
                            }
                        }
                    } else {
                        Log.e("HistoryFragment", "No records found")
                        FancyToast.makeText(requireContext(), "Записи не найдены", FancyToast.LENGTH_SHORT, FancyToast.WARNING, false).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("HistoryFragment", "Database error: ${error.message}")
                    FancyToast.makeText(requireContext(), "Ошибка при получении данных пользователя: ${error.message}", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show()
                }
            })
    }

    @SuppressLint("SetTextI18n")
    private fun addRecordToView(record: Record) {
        val recordView = LayoutInflater.from(context).inflate(R.layout.item_record, binding.recordsContainer, false)

        val doctorNameTextView: TextView = recordView.findViewById(R.id.doctorNameTextView)
        val doctorSpecializationTextView: TextView = recordView.findViewById(R.id.doctorSpecializationTextView)
        val appointmentDateTimeTextView: TextView = recordView.findViewById(R.id.appointmentDateTimeTextView)

        doctorNameTextView.text = record.doctorName
        doctorSpecializationTextView.text = record.doctorSpecialization
        appointmentDateTimeTextView.text = "${record.appointmentDate} ${record.appointmentTime}"

        recordView.setOnClickListener {
            showRecordDetailsDialog(record)
        }

        binding.recordsContainer.addView(recordView)
    }

    private fun showRecordDetailsDialog(record: Record) {
        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setTitle("Регистрационный талон")
        dialogBuilder.setMessage(
            "Врач: ${record.doctorName}\n" +
                    "Специализация: ${record.doctorSpecialization}\n" +
                    "Дата и время: ${record.appointmentDate} ${record.appointmentTime}(0)\n" +
                    "Адрес: ${record.doctorAdress}\n" +
                    "Кабинет: ${record.doctorOffice}\n" +
                    "Пациент: ${record.userFullName}\n" +
                    "ОМС: ${record.userOms}\n" +
                    "Паспорт: ${record.userPassport}\n" +
                    "Телефон: ${record.userPhoneNumber}\n" +
                    "СНИЛС: ${record.userSnils}"
        )
        dialogBuilder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        dialogBuilder.create().show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}