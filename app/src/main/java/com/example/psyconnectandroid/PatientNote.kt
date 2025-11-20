package com.example.psyconnectandroid

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

/**
 * Modelo de anotação do paciente
 * Estrutura do Firebase: patientNotes collection
 * ID do documento: {doctorId}_{patientId}
 */
data class PatientNote(
    @get:Exclude var id: String = "",
    val patientId: String = "",
    val doctorId: String = "",
    val doctorName: String = "",
    val doctorPhotoUrl: String = "",
    val updatedAt: Timestamp? = null,
    val shareWithPatient: Boolean = false,
    val feelingsAndSymptoms: String = "",
    val patientNeeds: String = "",
    val referralNeeded: Boolean = false,
    val referralDetails: String = "",
    val diagnosticHypotheses: String = "",
    val therapeuticGoals: String = "",
    val interventions: String = "",
    val observations: String = "",
    val carePlan: String = "",
    val riskLevel: String = ""
) {
    companion object {
        fun fromMap(map: Map<String, Any>, documentId: String): PatientNote {
            return PatientNote(
                id = documentId,
                patientId = map["patientId"] as? String ?: "",
                doctorId = map["doctorId"] as? String ?: "",
                doctorName = map["doctorName"] as? String ?: "",
                doctorPhotoUrl = map["doctorPhotoUrl"] as? String ?: "",
                updatedAt = map["updatedAt"] as? Timestamp,
                shareWithPatient = (map["shareWithPatient"] as? Boolean) ?: false,
                feelingsAndSymptoms = map["feelingsAndSymptoms"] as? String ?: "",
                patientNeeds = map["patientNeeds"] as? String ?: "",
                referralNeeded = (map["referralNeeded"] as? Boolean) ?: false,
                referralDetails = map["referralDetails"] as? String ?: "",
                diagnosticHypotheses = map["diagnosticHypotheses"] as? String ?: "",
                therapeuticGoals = map["therapeuticGoals"] as? String ?: "",
                interventions = map["interventions"] as? String ?: "",
                observations = map["observations"] as? String ?: "",
                carePlan = map["carePlan"] as? String ?: "",
                riskLevel = map["riskLevel"] as? String ?: ""
            )
        }
    }
    
    /**
     * Retorna um preview do conteúdo (therapeuticGoals ou feelingsAndSymptoms)
     */
    fun getPreview(): String {
        return when {
            therapeuticGoals.isNotEmpty() -> therapeuticGoals
            feelingsAndSymptoms.isNotEmpty() -> feelingsAndSymptoms
            patientNeeds.isNotEmpty() -> patientNeeds
            observations.isNotEmpty() -> observations
            else -> ""
        }
    }
}

