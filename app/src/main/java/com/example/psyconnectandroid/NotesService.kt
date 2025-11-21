package com.example.psyconnectandroid

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Serviço para gerenciar anotações do paciente
 */
class NotesService {
    private val TAG = "NotesService"
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    /**
     * Cria ou atualiza uma anotação
     */
    suspend fun saveNote(note: PatientNote): Boolean = withContext(Dispatchers.IO) {
        try {
            val doctorId = auth.currentUser?.uid
            if (doctorId == null) {
                Log.e(TAG, "❌ Usuário não autenticado")
                return@withContext false
            }
            
            val noteId = if (note.id.isNotEmpty()) {
                note.id
            } else {
                "${doctorId}_${note.patientId}"
            }
            
            val noteData = mapOf(
                "doctorId" to doctorId,
                "patientId" to note.patientId,
                "updatedAt" to Timestamp.now(),
                "shareWithPatient" to note.shareWithPatient,
                "feelingsAndSymptoms" to note.feelingsAndSymptoms,
                "patientNeeds" to note.patientNeeds,
                "referralNeeded" to note.referralNeeded,
                "referralDetails" to note.referralDetails,
                "diagnosticHypotheses" to note.diagnosticHypotheses,
                "therapeuticGoals" to note.therapeuticGoals,
                "interventions" to note.interventions,
                "observations" to note.observations,
                "carePlan" to note.carePlan,
                "riskLevel" to note.riskLevel
            )
            
            firestore.collection("patientNotes")
                .document(noteId)
                .set(noteData)
                .await()
            
            Log.d(TAG, "✅ Anotação salva: $noteId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao salvar anotação", e)
            false
        }
    }
    
    /**
     * Busca uma anotação específica
     */
    suspend fun getNote(doctorId: String, patientId: String): PatientNote? = withContext(Dispatchers.IO) {
        try {
            val noteId = "${doctorId}_${patientId}"
            val document = firestore.collection("patientNotes")
                .document(noteId)
                .get()
                .await()
            
            if (document.exists()) {
                PatientNote.fromMap(document.data!!, document.id)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar anotação", e)
            null
        }
    }
    
    /**
     * Lista todas as anotações do doutor
     */
    suspend fun getAllNotes(doctorId: String): List<PatientNote> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("patientNotes")
                .whereEqualTo("doctorId", doctorId)
                .get()
                .await()
            
            val notes = snapshot.documents.mapNotNull { document ->
                try {
                    PatientNote.fromMap(document.data ?: return@mapNotNull null, document.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao processar anotação ${document.id}", e)
                    null
                }
            }
            
            // Ordenar por updatedAt desc
            notes.sortedByDescending { it.updatedAt?.toDate() ?: java.util.Date(0) }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao listar anotações", e)
            emptyList()
        }
    }
    
    /**
     * Deleta uma anotação
     */
    suspend fun deleteNote(noteId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            firestore.collection("patientNotes")
                .document(noteId)
                .delete()
                .await()
            
            Log.d(TAG, "✅ Anotação deletada: $noteId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao deletar anotação", e)
            false
        }
    }
}


