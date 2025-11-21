package com.example.psyconnectandroid

import java.util.Date

/**
 * Modelo de dados para estatísticas do doutor
 * Segue a mesma estrutura do iOS para manter consistência
 */
data class DoctorStats(
    val totalConsultations: Int = 0,              // Total de consultas
    val completedConsultations: Int = 0,          // Consultas concluídas
    val pendingConsultations: Int = 0,            // Consultas pendentes
    val cancelledConsultations: Int = 0,          // Consultas canceladas
    val totalEarnings: Double = 0.0,              // Ganhos totais (EUR)
    val monthlyEarnings: Double = 0.0,            // Ganhos do mês atual (EUR)
    val averageRating: Double = 0.0,              // Avaliação média
    val totalPatients: Int = 0,                   // Pacientes únicos
    val newPatientsThisMonth: Int = 0,           // Novos pacientes este mês
    val consultationTrend: List<ConsultationTrend> = emptyList(),  // Tendência últimos 30 dias
    val monthlyStats: List<MonthlyStat> = emptyList(),            // Estatísticas mensais (12 meses)
    val recentActivity: List<ActivityItem> = emptyList()           // Atividades recentes
)

/**
 * Tendência de consultas por dia (últimos 30 dias)
 */
data class ConsultationTrend(
    val date: Date,
    val consultations: Int,
    val earnings: Double
)

/**
 * Estatísticas mensais
 */
data class MonthlyStat(
    val month: String,      // "Jan 2024"
    val year: Int,
    val consultations: Int,
    val earnings: Double,
    val patients: Int       // Pacientes únicos do mês
)

/**
 * Item de atividade recente
 */
data class ActivityItem(
    val type: ActivityType,
    val description: String,
    val timestamp: Date,
    val patientName: String?,
    val amount: Double?
)

/**
 * Tipo de atividade
 */
enum class ActivityType {
    CONSULTATION,   // Consulta
    PAYMENT,        // Pagamento
    CANCELLATION    // Cancelamento
}

/**
 * Dados de uma consulta processada
 */
data class AppointmentData(
    val id: String,
    val doctorId: String,
    val patientId: String,
    val patientName: String,
    val status: String,
    val createdAt: Date,
    val date: Date?,
    val amount: Int,  // Em centavos
    val paymentStatus: String?
)



