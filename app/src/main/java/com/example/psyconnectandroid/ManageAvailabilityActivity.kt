package com.example.psyconnectandroid

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.CalendarView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ManageAvailabilityActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var calendarView: CalendarView
    private lateinit var tvSelectedDate: TextView
    private lateinit var rvSlots: RecyclerView
    private lateinit var btnAddSlot: Button

    private lateinit var adapter: EditableAvailabilityAdapter
    private val slots = mutableListOf<AvailabilitySlot>()
    private var selectedDate: Calendar = Calendar.getInstance()

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val doctorId by lazy { auth.currentUser?.uid }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_availability)

        initializeViews()
        setupToolbar()
        setupCalendar()
        setupRecyclerView()
        setupListeners()

        updateDateLabel()
        loadSlotsForSelectedDate()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbarManageAvailability)
        calendarView = findViewById(R.id.calendarView)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        rvSlots = findViewById(R.id.rvAvailabilitySlots)
        btnAddSlot = findViewById(R.id.btnAddSlot)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupCalendar() {
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate.set(year, month, dayOfMonth)
            updateDateLabel()
            loadSlotsForSelectedDate()
        }
    }

    private fun setupRecyclerView() {
        adapter = EditableAvailabilityAdapter(slots) { slot ->
            removeSlot(slot)
        }
        rvSlots.layoutManager = LinearLayoutManager(this)
        rvSlots.adapter = adapter
    }
    
    private fun setupListeners() {
        btnAddSlot.setOnClickListener {
            showTimePickerDialog()
        }
    }

    private fun updateDateLabel() {
        val format = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR"))
        tvSelectedDate.text = "Horários para ${format.format(selectedDate.time)}"
    }

    private fun loadSlotsForSelectedDate() {
        if (doctorId == null) return

        val startOfDay = selectedDate.clone() as Calendar
        startOfDay.set(Calendar.HOUR_OF_DAY, 0)
        
        val endOfDay = selectedDate.clone() as Calendar
        endOfDay.set(Calendar.HOUR_OF_DAY, 23)

        firestore.collection("doctorAvailability")
            .whereEqualTo("doctorId", doctorId)
            .whereGreaterThanOrEqualTo("startTime", Timestamp(startOfDay.time))
            .whereLessThanOrEqualTo("startTime", Timestamp(endOfDay.time))
            .orderBy("startTime")
            .get()
            .addOnSuccessListener { documents ->
                slots.clear()
                for (doc in documents) {
                    slots.add(AvailabilitySlot.fromMap(doc.data, doc.id))
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun showTimePickerDialog() {
        val currentTime = Calendar.getInstance()
        val hour = currentTime.get(Calendar.HOUR_OF_DAY)
        val minute = currentTime.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val startTime = selectedDate.clone() as Calendar
            startTime.set(Calendar.HOUR_OF_DAY, selectedHour)
            startTime.set(Calendar.MINUTE, selectedMinute)

            val endTime = startTime.clone() as Calendar
            endTime.add(Calendar.HOUR, 1) // Assume 1-hour slots

            addSlot(startTime.time, endTime.time)
        }, hour, minute, true).show()
    }

    private fun addSlot(startTime: Date, endTime: Date) {
        if (doctorId == null) return

        val newSlot = hashMapOf(
            "doctorId" to doctorId,
            "startTime" to Timestamp(startTime),
            "endTime" to Timestamp(endTime),
            "isBooked" to false
        )

        firestore.collection("doctorAvailability").add(newSlot)
            .addOnSuccessListener {
                Toast.makeText(this, "Horário adicionado!", Toast.LENGTH_SHORT).show()
                loadSlotsForSelectedDate() // Refresh the list
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao adicionar horário.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeSlot(slot: AvailabilitySlot) {
        if (slot.id.isEmpty()) return

        firestore.collection("doctorAvailability").document(slot.id).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Horário removido!", Toast.LENGTH_SHORT).show()
                loadSlotsForSelectedDate() // Refresh the list
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao remover horário.", Toast.LENGTH_SHORT).show()
            }
    }
}
