# 🔍 Debug de Disponibilidades dos Doutores

## 📋 Estrutura de Dados no Firestore

### Coleção: `doctorAvailability`

Cada documento contém os seguintes campos:

```javascript
{
  doctorId: "hhGisn7aNHdOlOElpZg5iVT59Ge2",    // ID do doutor
  startTime: Timestamp,                        // Horário de início
  endTime: Timestamp,                          // Horário de fim
  date: Timestamp,                             // Data do slot
  isBooked: false,                             // Se já foi reservado
  isAvailable: true,                           // Se está disponível
  patientId: null,                             // ID do paciente (null se não reservado)
  patientName: null,                           // Nome do paciente (null se não reservado)
  appointmentId: null,                         // ID da consulta (null se não reservado)
  createdAt: Timestamp,                        // Data de criação
  updatedAt: Timestamp                         // Última atualização
}
```

## 🔄 Fluxo de Carregamento

### 1. DoctorProfileActivity.kt

```kotlin
loadAvailability() {
    // 1. Busca no Firestore por doctorId
    firestore.collection("doctorAvailability")
        .whereEqualTo("doctorId", doctorId)
        .get()
    
    // 2. Processa cada documento
    for (document in querySnapshot) {
        val slot = AvailabilitySlot.fromMap(data, document.id)
        
        // 3. Filtra:
        //    - isBooked = false
        //    - isAvailable = true
        //    - startTime > now (futuro)
        
        if (!slot.isBooked && slot.isAvailable && slot.startTime > now) {
            availabilitySlots.add(slot)
        }
    }
    
    // 4. Ordena por startTime (mais próximos primeiro)
    availabilitySlots.sort()
    
    // 5. Limita a 10 slots
    availabilitySlots.take(10)
    
    // 6. Atualiza UI
    availabilityAdapter.notifyDataSetChanged()
}
```

## 📊 Classe AvailabilitySlot

### Campos Principais

```kotlin
data class AvailabilitySlot(
    val doctorId: String,           // ✅ Novo campo
    val startTime: Timestamp?,      // Horário início
    val endTime: Timestamp?,        // Horário fim
    val date: Timestamp?,           // ✅ Novo campo
    val isBooked: Boolean,          // Status reserva
    val isAvailable: Boolean,       // ✅ Novo campo
    val patientId: String?,         // ✅ Novo campo
    val patientName: String?,       // ✅ Novo campo
    val appointmentId: String?,     // ✅ Novo campo
    val createdAt: Timestamp?,      // ✅ Novo campo
    val updatedAt: Timestamp?       // ✅ Novo campo
)
```

### Método fromMap

```kotlin
companion object {
    fun fromMap(map: Map<String, Any>, id: String): AvailabilitySlot {
        return AvailabilitySlot(
            doctorId = map["doctorId"] as? String ?: "",
            startTime = map["startTime"] as? Timestamp,
            endTime = map["endTime"] as? Timestamp,
            date = map["date"] as? Timestamp,
            isBooked = map["isBooked"] as? Boolean ?: false,
            isAvailable = map["isAvailable"] as? Boolean ?: true,
            patientId = map["patientId"] as? String,
            patientName = map["patientName"] as? String,
            appointmentId = map["appointmentId"] as? String,
            createdAt = map["createdAt"] as? Timestamp,
            updatedAt = map["updatedAt"] as? Timestamp
        ).apply {
            this.id = id
        }
    }
}
```

## 🔍 Logs de Debug

### Para verificar se as disponibilidades estão sendo carregadas:

#### No Logcat, filtrar por tag: `DoctorProfileActivity`

```
🔍 Loading availability for doctor: {doctorId}
✅ Found X total availability slots
   📄 Processing slot {slotId}
      Fields: [doctorId, startTime, endTime, ...]
      Slot: isBooked=false, isAvailable=true
      StartTime: Sun Nov 17 00:07:20 GMT 2025
      ✅ Added to list
✅ Final availability count: X
```

### Logs possíveis:

1. **Sucesso completo:**
   ```
   🔍 Loading availability for doctor: abc123
   ✅ Found 4 total availability slots
   📄 Processing slot 1
      ✅ Added to list
   ✅ Final availability count: 3
   ```

2. **Nenhum slot disponível:**
   ```
   🔍 Loading availability for doctor: abc123
   ✅ Found 0 total availability slots
   ⚠️ No available slots found for this doctor
   ```

3. **Slots no passado:**
   ```
   🔍 Loading availability for doctor: abc123
   ✅ Found 2 total availability slots
   📄 Processing slot 1
      ⏭️ Skipped - past date
   ✅ Final availability count: 0
   ```

4. **Erro ao carregar:**
   ```
   ❌ Error loading availability from Firestore
   ```

## 🧪 Como Testar

### 1. Verificar Firestore

1. Abrir Firebase Console
2. Ir para Firestore Database
3. Navegar até `doctorAvailability`
4. Verificar se existem documentos
5. Confirmar campos:
   - `doctorId` existe e está correto
   - `isBooked` = false
   - `isAvailable` = true
   - `startTime` está no futuro

### 2. Testar no App

1. Login como paciente
2. Navegar até perfil do doutor
3. Verificar Logcat para logs de debug
4. Confirmar se slots aparecem na tela

### 3. Verificar Filtros

Os slots são filtrados por:
- ✅ `doctorId` corresponde ao doutor visualizado
- ✅ `isBooked` = false (não reservado)
- ✅ `isAvailable` = true (disponível)
- ✅ `startTime` > agora (futuro)

## 🔧 Problemas Comuns

### Problema 1: Nenhum slot aparece

**Possíveis causas:**
1. `doctorId` no documento não corresponde ao ID do doutor
2. Todos os slots têm `isBooked` = true
3. Todos os slots estão no passado
4. Campo `isAvailable` = false

**Solução:**
- Verificar logs de debug no Logcat
- Confirmar dados no Firestore
- Criar novos slots no futuro

### Problema 2: Slots do passado aparecem

**Causa:**
- Filtro de data não está funcionando

**Solução:**
- Verificar se `startTime` está sendo lido corretamente
- Confirmar tipo Timestamp no Firestore

### Problema 3: Erro ao carregar

**Possíveis causas:**
1. Permissões do Firestore
2. Conexão com internet
3. Estrutura de dados incorreta

**Solução:**
- Verificar regras do Firestore
- Testar conexão
- Validar estrutura dos documentos

## 📝 Exemplo de Documento Válido

```javascript
{
  "doctorId": "hhGisn7aNHdOlOElpZg5iVT59Ge2",
  "startTime": {
    "seconds": 1731800840,
    "nanoseconds": 0
  },
  "endTime": {
    "seconds": 1731804440,
    "nanoseconds": 0
  },
  "date": {
    "seconds": 1731800820,
    "nanoseconds": 0
  },
  "isBooked": false,
  "isAvailable": true,
  "patientId": null,
  "patientName": null,
  "appointmentId": null,
  "createdAt": {
    "seconds": 1731798760,
    "nanoseconds": 481649000
  },
  "updatedAt": {
    "seconds": 1731798762,
    "nanoseconds": 0
  }
}
```

## ✅ Checklist de Verificação

Ao debugar disponibilidades, verificar:

- [ ] Coleção `doctorAvailability` existe no Firestore
- [ ] Documentos têm campo `doctorId` correto
- [ ] Campo `isBooked` = false
- [ ] Campo `isAvailable` = true  
- [ ] Campo `startTime` está no futuro
- [ ] Todos os campos obrigatórios existem
- [ ] Logs aparecem no Logcat
- [ ] Adapter está recebendo os dados
- [ ] UI está sendo atualizada (notifyDataSetChanged)
- [ ] Layout `item_availability_slot` existe

## 🎯 Melhorias Implementadas

### V2.0 - Novembro 2025

- ✅ Adicionados todos os campos do Firestore
- ✅ Filtro por `isAvailable`
- ✅ Logs detalhados de debug
- ✅ Tratamento de erros melhorado
- ✅ Filtragem local ao invés de query composta
- ✅ Documentação completa

## 📞 Suporte

Se os problemas persistirem:
1. Verificar logs completos no Logcat
2. Confirmar estrutura de dados no Firestore
3. Validar permissões do Firestore Rules
4. Testar com diferentes doutores

