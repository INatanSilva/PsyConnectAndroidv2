package com.example.psyconnectandroid

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Serviço para calcular estatísticas do doutor
 * Segue a mesma lógica do iOS para manter consistência
 */
class DoctorStatsService {
    private val TAG = "DoctorStatsService"
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    var stats: DoctorStats? = null
        private set
    
    /**
     * Carrega todas as estatísticas do doutor
     */
    suspend fun loadStats(): DoctorStats? = withContext(Dispatchers.IO) {
        try {
            val doctorId = auth.currentUser?.uid
            if (doctorId == null) {
                Log.e(TAG, "❌ Usuário não autenticado")
                return@withContext null
            }
            
            Log.d(TAG, "🔍 Carregando estatísticas para doutor: $doctorId")
            val calculatedStats = calculateDoctorStats(doctorId)
            stats = calculatedStats
            Log.d(TAG, "✅ Estatísticas carregadas: ${calculatedStats.totalConsultations} consultas, ${calculatedStats.totalPatients} pacientes")
            calculatedStats
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao carregar estatísticas", e)
            null
        }
    }
    
    /**
     * Calcula todas as estatísticas do doutor
     */
    private suspend fun calculateDoctorStats(doctorId: String): DoctorStats {
        // 1. Buscar todas as consultas do doutor
        val appointmentsSnapshot = firestore.collection("appointments")
            .whereEqualTo("doctorId", doctorId)
            .get()
            .await()
        
        Log.d(TAG, "✅ Encontradas ${appointmentsSnapshot.size()} consultas")
        
        // 2. Coletar patientIds únicos e processar dados
        val patientIds = mutableSetOf<String>()
        val documentsData = mutableListOf<Pair<String, Map<String, Any>>>()
        
        for (document in appointmentsSnapshot.documents) {
            val data = document.data
            if (data != null) {
                val patientId = data["patientId"] as? String
                if (patientId != null) {
                    patientIds.add(patientId)
                    documentsData.add(Pair(document.id, data))
                }
            }
        }
        
        // 3. Buscar nomes dos pacientes em paralelo (se necessário)
        val patientNamesCache = fetchPatientNames(documentsData)
        
        // 4. Criar AppointmentData com nomes corretos
        val appointments = documentsData.mapNotNull { (docId, data) ->
            try {
                val patientId = data["patientId"] as? String ?: return@mapNotNull null
                val storedName = data["patientName"] as? String
                val patientName = storedName ?: patientNamesCache[patientId] ?: "Paciente"
                
                val createdAt = (data["createdAt"] as? Timestamp)?.toDate() ?: Date()
                val date = (data["date"] as? Timestamp)?.toDate()
                val payment = data["payment"] as? Map<*, *>
                val amount = (payment?.get("amountEurCents") as? Number)?.toInt() 
                    ?: (data["priceEurCents"] as? Number)?.toInt() 
                    ?: 0
                val paymentStatus = payment?.get("status") as? String
                
                AppointmentData(
                    id = docId,
                    doctorId = doctorId,
                    patientId = patientId,
                    patientName = patientName,
                    status = data["status"] as? String ?: "Agendada",
                    createdAt = createdAt,
                    date = date,
                    amount = amount,
                    paymentStatus = paymentStatus
                )
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao processar consulta $docId", e)
                null
            }
        }
        
        // 5. Calcular métricas
        val totalConsultations = appointments.size
        val completedConsultations = appointments.count { it.status == "Concluída" }
        val pendingConsultations = appointments.count { 
            it.status == "Agendada" || it.status == "Pago" || it.status == "Aguardando Confirmação"
        }
        val cancelledConsultations = appointments.count { it.status == "Cancelada" }
        
        // Ganhos totais (apenas consultas concluídas)
        val totalEarnings = appointments
            .filter { it.status == "Concluída" }
            .sumOf { it.amount / 100.0 }
        
        // Ganhos do mês atual
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        val monthlyEarnings = appointments
            .filter { appointment ->
                appointment.status == "Concluída" && appointment.date != null
            }
            .filter { appointment ->
                val appointmentCalendar = Calendar.getInstance().apply {
                    time = appointment.date!!
                }
                appointmentCalendar.get(Calendar.MONTH) == currentMonth &&
                appointmentCalendar.get(Calendar.YEAR) == currentYear
            }
            .sumOf { it.amount / 100.0 }
        
        // Pacientes únicos
        val uniquePatientIds = appointments.map { it.patientId }.distinct()
        val totalPatients = uniquePatientIds.size
        
        // Novos pacientes este mês
        val newPatientsThisMonth = appointments
            .filter { appointment ->
                appointment.date != null
            }
            .filter { appointment ->
                val appointmentCalendar = Calendar.getInstance().apply {
                    time = appointment.date!!
                }
                appointmentCalendar.get(Calendar.MONTH) == currentMonth &&
                appointmentCalendar.get(Calendar.YEAR) == currentYear
            }
            .map { it.patientId }
            .distinct()
            .size
        
        // Buscar avaliação média
        val averageRating = fetchAverageRating(doctorId)
        
        // Gerar dados calculados
        val consultationTrend = generateConsultationTrend(appointments)
        val monthlyStats = generateMonthlyStats(appointments)
        val recentActivity = generateRecentActivity(appointments)
        
        return DoctorStats(
            totalConsultations = totalConsultations,
            completedConsultations = completedConsultations,
            pendingConsultations = pendingConsultations,
            cancelledConsultations = cancelledConsultations,
            totalEarnings = totalEarnings,
            monthlyEarnings = monthlyEarnings,
            averageRating = averageRating,
            totalPatients = totalPatients,
            newPatientsThisMonth = newPatientsThisMonth,
            consultationTrend = consultationTrend,
            monthlyStats = monthlyStats,
            recentActivity = recentActivity
        )
    }
    
    /**
     * Busca nomes dos pacientes em paralelo
     */
    private suspend fun fetchPatientNames(
        documentsData: List<Pair<String, Map<String, Any>>>
    ): Map<String, String> = withContext(Dispatchers.IO) {
        val patientNamesCache = mutableMapOf<String, String>()
        val idsToFetch = mutableSetOf<String>()
        
        // Verificar quais nomes já estão armazenados
        for ((_, data) in documentsData) {
            val patientId = data["patientId"] as? String ?: continue
            val storedName = data["patientName"] as? String
            
            if (storedName != null && storedName.isNotEmpty() && storedName != "Patient") {
                patientNamesCache[patientId] = storedName
            } else {
                idsToFetch.add(patientId)
            }
        }
        
        // Buscar nomes em paralelo
        val fetchedNames = idsToFetch.map { patientId ->
            async {
                Pair(patientId, fetchPatientName(patientId))
            }
        }.awaitAll()
        
        fetchedNames.forEach { (patientId, name) ->
            if (name != null) {
                patientNamesCache[patientId] = name
            }
        }
        
        patientNamesCache
    }
    
    /**
     * Busca nome de um paciente específico
     */
    private suspend fun fetchPatientName(patientId: String): String? = withContext(Dispatchers.IO) {
        try {
            // Tentar "users" primeiro
            val usersDoc = firestore.collection("users").document(patientId).get().await()
            if (usersDoc.exists()) {
                val name = usersDoc.getString("name") 
                    ?: usersDoc.getString("fullName")
                if (name != null) return@withContext name
            }
            
            // Fallback: "pacientes"
            val pacientesDoc = firestore.collection("pacientes").document(patientId).get().await()
            if (pacientesDoc.exists()) {
                val name = pacientesDoc.getString("name")
                    ?: pacientesDoc.getString("fullName")
                if (name != null) return@withContext name
            }
            
            // Último fallback: "patients"
            val patientsDoc = firestore.collection("patients").document(patientId).get().await()
            if (patientsDoc.exists()) {
                val name = patientsDoc.getString("name")
                    ?: patientsDoc.getString("fullName")
                if (name != null) return@withContext name
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar nome do paciente $patientId", e)
            null
        }
    }
    
    /**
     * Busca avaliação média do doutor
     */
    private suspend fun fetchAverageRating(doctorId: String): Double = withContext(Dispatchers.IO) {
        try {
            val reviewsSnapshot = firestore.collection("avaliacoes")
                .whereEqualTo("doctorId", doctorId)
                .get()
                .await()
            
            if (reviewsSnapshot.isEmpty) return@withContext 0.0
            
            val ratings = reviewsSnapshot.documents.mapNotNull { doc ->
                (doc.get("rating") as? Number)?.toDouble()
            }
            
            if (ratings.isEmpty()) return@withContext 0.0
            ratings.average()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar avaliações", e)
            0.0
        }
    }
    
    /**
     * Gera tendência de consultas (últimos 30 dias)
     */
    private fun generateConsultationTrend(appointments: List<AppointmentData>): List<ConsultationTrend> {
        val calendar = Calendar.getInstance()
        val today = Date()
        val trends = mutableListOf<ConsultationTrend>()
        
        for (i in 0 until 30) {
            val date = calendar.apply {
                time = today
                add(Calendar.DAY_OF_YEAR, -i)
            }.time
            
            val dayStart = calendar.apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            
            val dayEnd = calendar.apply {
                time = dayStart
                add(Calendar.DAY_OF_YEAR, 1)
            }.time
            
            val dayAppointments = appointments.filter { appointment ->
                appointment.createdAt >= dayStart && appointment.createdAt < dayEnd
            }
            
            val consultations = dayAppointments.size
            val earnings = dayAppointments
                .filter { it.status == "Concluída" }
                .sumOf { it.amount / 100.0 }
            
            trends.add(ConsultationTrend(
                date = date,
                consultations = consultations,
                earnings = earnings
            ))
        }
        
        return trends.reversed() // Mais antigo primeiro
    }
    
    /**
     * Gera estatísticas mensais (últimos 12 meses)
     */
    private fun generateMonthlyStats(appointments: List<AppointmentData>): List<MonthlyStat> {
        val calendar = Calendar.getInstance()
        val monthFormatter = SimpleDateFormat("MMM", Locale("pt", "BR"))
        val yearFormatter = SimpleDateFormat("yyyy", Locale.getDefault())
        
        val monthlyData = mutableMapOf<String, Triple<Int, Double, MutableSet<String>>>()
        
        for (appointment in appointments) {
            val date = appointment.date ?: appointment.createdAt
            val monthKey = monthFormatter.format(date)
            val year = yearFormatter.format(date).toInt()
            val fullKey = "$monthKey $year"
            
            val current = monthlyData[fullKey] ?: Triple(0, 0.0, mutableSetOf())
            val consultations = current.first + 1
            val earnings = if (appointment.status == "Concluída") {
                current.second + (appointment.amount / 100.0)
            } else {
                current.second
            }
            val patients = current.third.apply { add(appointment.patientId) }
            
            monthlyData[fullKey] = Triple(consultations, earnings, patients)
        }
        
        return monthlyData.map { (key, value) ->
            val parts = key.split(" ")
            val month = parts[0]
            val year = parts[1].toInt()
            
            MonthlyStat(
                month = key,
                year = year,
                consultations = value.first,
                earnings = value.second,
                patients = value.third.size
            )
        }.sortedBy { "${it.year}-${it.month}" }
    }
    
    /**
     * Gera atividades recentes (últimas 10)
     */
    private fun generateRecentActivity(appointments: List<AppointmentData>): List<ActivityItem> {
        val recentAppointments = appointments
            .sortedByDescending { it.createdAt }
            .take(10)
        
        return recentAppointments.map { appointment ->
            val activityType: ActivityType
            val description: String
            
            when (appointment.status) {
                "Concluída" -> {
                    activityType = ActivityType.CONSULTATION
                    description = "Consulta concluída com ${appointment.patientName}"
                }
                "Pago" -> {
                    activityType = ActivityType.PAYMENT
                    description = "Pagamento recebido de ${appointment.patientName}"
                }
                "Cancelada" -> {
                    activityType = ActivityType.CANCELLATION
                    description = "Consulta cancelada com ${appointment.patientName}"
                }
                else -> {
                    activityType = ActivityType.CONSULTATION
                    description = "Nova consulta agendada com ${appointment.patientName}"
                }
            }
            
            ActivityItem(
                type = activityType,
                description = description,
                timestamp = appointment.createdAt,
                patientName = appointment.patientName,
                amount = if (appointment.status == "Pago" || appointment.status == "Concluída") {
                    appointment.amount / 100.0
                } else null
            )
        }
    }
}


