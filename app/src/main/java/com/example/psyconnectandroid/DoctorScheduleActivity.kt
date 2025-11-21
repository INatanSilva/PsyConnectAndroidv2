package com.example.psyconnectandroid

import android.os.Bundle
import android.widget.Button
import android.widget.CalendarView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

/**
 * Agenda do doutor - gerenciamento de disponibilidade
 */
class DoctorScheduleActivity : AppCompatActivity() {
    
    // Views
    private lateinit var calendarView: CalendarView
    private lateinit var rvSchedule: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var btnAddSlot: Button
    private lateinit var tvSelectedDate: TextView
    
    // Services
    private val availabilityService = DoctorAvailabilityService()
    private val auth = FirebaseAuth.getInstance()
    
    // Adapter
    private lateinit var adapter: ScheduleSlotAdapter
    
    // Data
    private var selectedDate: Date = Date()
    private val slots = mutableListOf<DoctorAvailability>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_schedule)
        
        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        setupSwipeRefresh()
        loadSchedule()
    }
    
    private fun initializeViews() {
        calendarView = findViewById(R.id.calendarView)
        rvSchedule = findViewById(R.id.rvSchedule)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        btnAddSlot = findViewById(R.id.btnAddSlot)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        
        // Set today as selected date
        calendarView.date = System.currentTimeMillis()
        updateSelectedDateText()
    }
    
    private fun setupRecyclerView() {
        adapter = ScheduleSlotAdapter(slots) { slot ->
            // Toggle availability or cancel appointment
            if (slot.isBooked) {
                cancelAppointment(slot)
            } else {
                toggleAvailability(slot)
            }
        }
        rvSchedule.layoutManager = LinearLayoutManager(this)
        rvSchedule.adapter = adapter
    }
    
    private fun setupClickListeners() {
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            selectedDate = calendar.time
            updateSelectedDateText()
            loadSchedule()
        }
        
        btnAddSlot.setOnClickListener {
            // TODO: Implementar adicionar slot
            Toast.makeText(this, "Adicionar slot será implementado", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            loadSchedule()
        }
    }
    
    private fun updateSelectedDateText() {
        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        tvSelectedDate.text = "Agenda de ${dateFormat.format(selectedDate)}"
    }
    
    private fun loadSchedule() {
        val doctorId = auth.currentUser?.uid
        if (doctorId == null) {
            Toast.makeText(this, "Erro: Usuário não autenticado", Toast.LENGTH_SHORT).show()
            return
        }
        
        swipeRefreshLayout.isRefreshing = true
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val schedule = availabilityService.getDoctorSchedule(doctorId, selectedDate)
                slots.clear()
                slots.addAll(schedule)
                adapter.notifyDataSetChanged()
            } catch (e: Exception) {
                android.util.Log.e("DoctorSchedule", "❌ Erro ao carregar agenda", e)
                Toast.makeText(this@DoctorScheduleActivity, "Erro ao carregar agenda", Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }
    
    private fun toggleAvailability(slot: DoctorAvailability) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val success = availabilityService.updateAvailability(
                    slot.id,
                    !slot.isAvailable
                )
                if (success) {
                    loadSchedule()
                } else {
                    Toast.makeText(this@DoctorScheduleActivity, "Erro ao atualizar disponibilidade", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("DoctorSchedule", "❌ Erro ao atualizar disponibilidade", e)
                Toast.makeText(this@DoctorScheduleActivity, "Erro ao atualizar disponibilidade", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun cancelAppointment(slot: DoctorAvailability) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val success = availabilityService.cancelAppointment(slot.id)
                if (success) {
                    Toast.makeText(this@DoctorScheduleActivity, "Agendamento cancelado", Toast.LENGTH_SHORT).show()
                    loadSchedule()
                } else {
                    Toast.makeText(this@DoctorScheduleActivity, "Erro ao cancelar agendamento", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("DoctorSchedule", "❌ Erro ao cancelar agendamento", e)
                Toast.makeText(this@DoctorScheduleActivity, "Erro ao cancelar agendamento", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        loadSchedule()
    }
}
