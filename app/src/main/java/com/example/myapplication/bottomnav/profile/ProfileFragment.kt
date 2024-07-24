package com.example.myapplication.bottomnav.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.myapplication.MainActivity
import com.example.myapplication.databinding.FragmentProfileBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.shashank.sony.fancytoastlib.FancyToast
import java.io.IOException

class ProfileFragment:Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    var filePath: Uri = Uri.EMPTY



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        loadUserInfo()

        binding.profileImageView.setOnClickListener(){
            selectImage()
        }



        binding.logoutBtn.setOnClickListener(){
            val loginIntent = Intent(activity, MainActivity::class.java)
            FirebaseAuth.getInstance().signOut()
            startActivity(loginIntent)
        }
        return binding.root




    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.saveUserInfoBtn.setOnClickListener() {
            val phoneNumber = binding.numberEt.text.toString()
            val passport = binding.passportEt.text.toString()
            val snils = binding.snilsEt.text.toString()
            val oms = binding.omsEt.text.toString()

            val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
            if(currentUserUid != null) {
                val userRef = FirebaseDatabase.getInstance().reference.child("Users").child(currentUserUid)

                val userInfo = HashMap<String, Any>()
                userInfo["phoneNumber"] = phoneNumber
                userInfo["passport"] = passport
                userInfo["snils"] = snils
                userInfo["oms"] = oms

                userRef.updateChildren(userInfo)
                    .addOnSuccessListener {
                        FancyToast.makeText(requireContext(), "Данные успешно сохранены", FancyToast.LENGTH_LONG, FancyToast.SUCCESS, false).show()
                    }
                    .addOnFailureListener{ e ->
                        FancyToast.makeText(requireContext(), "Ошибка при сохранении данных: ${e.message}", FancyToast.LENGTH_LONG, FancyToast.ERROR, false).show()
                    }
            } else {
                FancyToast.makeText(requireContext(), "Пользователь не аутентифицирован", FancyToast.LENGTH_LONG, FancyToast.WARNING, false).show()
            }
        }
    }

    var pickActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null && result.data!!.data != null) {
            filePath = result.data!!.data!!
        }

        try {
            val bitmap: Bitmap = MediaStore.Images.Media
                .getBitmap(
                    requireContext().contentResolver, filePath
                )
            binding.profileImageView.setImageBitmap(bitmap)
        }catch (e: IOException) {
            e.printStackTrace()
        }

        uploadImage()
    }


    private fun loadUserInfo() {
        FirebaseDatabase.getInstance().reference.child("Users").child(FirebaseAuth.getInstance().currentUser!!.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var userName:String = snapshot.child("username").value.toString()
                    var emailUser:String = snapshot.child("email").value.toString()
                    var profileImage:String = snapshot.child("profileImage").value.toString()
                    var phoneNumber: String = snapshot.child("phoneNumber").value.toString()
                    var oms: String = snapshot.child("oms").value.toString()
                    var passport: String = snapshot.child("passport").value.toString()
                    var snils: String = snapshot.child("snils").value.toString()


                    binding.usernameTv.text = userName
                    binding.emailEt.setText(emailUser)
                    binding.numberEt.setText(phoneNumber)
                    binding.passportEt.setText(passport)
                    binding.snilsEt.setText(snils)
                    binding.omsEt.setText(oms)

                    if(!profileImage.isEmpty()){
                        Glide.with(this@ProfileFragment).load(profileImage).into(binding.profileImageView)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    var errorMessage = "Ошибка базы данных: ${error.message}"
                    FancyToast.makeText(context as Context,errorMessage, FancyToast.LENGTH_LONG, FancyToast.ERROR, false).show()
                }
            })
    }

//    private fun uploadUserInfo(){
//            var uid: String = FirebaseAuth.getInstance().currentUser!!.uid
//            var userInfo = HashMap<String, String>()
//            userInfo.put("email", binding.emailEt.text.toString())
//
//    }

    private fun selectImage(){
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        pickActivityResultLauncher.launch(intent)
    }

    private fun uploadImage() {
        if (filePath!= null){
            var uid:String = FirebaseAuth.getInstance().currentUser!!.uid
            FirebaseStorage.getInstance().reference.child("images/"+uid)
                .putFile(filePath).addOnSuccessListener {
                    FancyToast.makeText( context as Context, "Фото загружено успешно", FancyToast.LENGTH_LONG,FancyToast.SUCCESS, false).show()

                    FirebaseStorage.getInstance().reference.child("images/" + uid).downloadUrl
                        .addOnSuccessListener {
                            FirebaseDatabase.getInstance().reference.child("Users").child(FirebaseAuth.getInstance().currentUser!!.uid)
                                .child("profileImage").setValue(it.toString())
                        }
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}