# 📅 Campos de Disponibilidade - Explicação

## 🎯 Diferença entre os Campos

### `date` ✅ **CAMPO PRINCIPAL**
- **O que é**: Data da consulta
- **Uso**: Determinar se a consulta está no futuro ou passado
- **Exemplo**: `18 de novembro de 2025 às 23:13:00 UTC`
- **Importância**: **CRÍTICO** - Este é o campo usado para filtrar slots disponíveis

### `startTime` ⏰
- **O que é**: Horário específico de início da consulta
- **Uso**: Mostrar horário exato para o paciente
- **Exemplo**: `16 de novembro de 2025 às 23:19:55 UTC`
- **Nota**: Pode ser diferente de `date` por questões de criação

### `endTime` ⏰
- **O que é**: Horário específico de término da consulta
- **Uso**: Calcular duração da consulta
- **Exemplo**: `17 de novembro de 2025 às 00:19:55 UTC`

### `createdAt` 📝
- **O que é**: Quando o registro foi criado no Firestore
- **Uso**: Auditoria e histórico
- **Exemplo**: `16 de novembro de 2025 às 23:20:00 UTC`
- **Importância**: Apenas informativo, **NÃO** usar para filtrar

### `updatedAt` 📝
- **O que é**: Última atualização do registro
- **Uso**: Auditoria e sincronização
- **Exemplo**: `16 de novembro de 2025 às 23:20:00 UTC`

## 🔍 Exemplo Real

```javascript
{
  // ✅ DATA DA CONSULTA (usar para filtrar)
  "date": "18 de novembro de 2025 às 23:13:00 UTC",
  
  // ⏰ HORÁRIO DE INÍCIO (mostrar na UI)
  "startTime": "16 de novembro de 2025 às 23:19:55 UTC",
  
  // ⏰ HORÁRIO DE FIM
  "endTime": "17 de novembro de 2025 às 00:19:55 UTC",
  
  // 📝 QUANDO FOI CRIADO (não usar para filtrar)
  "createdAt": "16 de novembro de 2025 às 23:20:00 UTC",
  
  // 📝 ÚLTIMA ATUALIZAÇÃO
  "updatedAt": "16 de novembro de 2025 às 23:20:00 UTC",
  
  // Outros campos
  "doctorId": "hhGIsn7aNHdOf0Elp2g5ivT59Ge2",
  "isBooked": false,
  "isAvailable": true,
  "patientId": null,
  "patientName": null,
  "appointmentId": null
}
```

## ⚠️ Problema Anterior

### Antes (ERRADO ❌)
```kotlin
// Filtrava usando startTime
if (slot.startTime!!.compareTo(now) > 0) {
    // startTime = 16/11 (passado)
    // Rejeitava o slot mesmo com date = 18/11 (futuro)
}
```

### Agora (CORRETO ✅)
```kotlin
// Filtra usando date
val dateToCheck = slot.date ?: slot.startTime
if (dateToCheck.compareTo(now) > 0) {
    // date = 18/11 (futuro)
    // Aceita o slot corretamente
}
```

## 📊 Lógica de Filtragem

```kotlin
// Prioridade na verificação:
val dateToCheck = slot.date ?: slot.startTime

// 1. Usa 'date' se existir (prioritário)
// 2. Fallback para 'startTime' se date não existir
// 3. Compara com tempo atual

if (dateToCheck > now) {
    // Consulta no futuro ✅
} else {
    // Consulta no passado ❌
}
```

## 🧪 Como Testar

### 1. Ver no Logcat

Após a correção, você verá:
```
📄 Processing slot 7D2CF859-848D-4CC2-8CDC-27D6BA2B5F26
   Slot: isBooked=false, isAvailable=true
   Date (consulta): Tue Nov 18 23:13:00 GMT 2025  ✅ FUTURO
   StartTime (horário): Sun Nov 16 23:19:55 GMT 2025
   ⏰ Date comparison: 1 (>0 = future)
   ⏰ Difference: 172800 seconds (48.00 hours)
   ✅ Added to list (48.00 hours in future)
```

### 2. Criar Novo Slot

Ao criar um slot no `ManageAvailabilityActivity`:
```kotlin
val newSlot = hashMapOf(
    "date" to dateTimestamp,        // ✅ Data da consulta
    "startTime" to startTimestamp,  // Horário específico
    "endTime" to endTimestamp,      // Horário fim
    "createdAt" to now,             // Quando foi criado
    "updatedAt" to now              // Última atualização
)
```

## 📋 Checklist

Ao criar/verificar slots:

- [ ] Campo `date` existe
- [ ] Campo `date` está no futuro
- [ ] Campo `startTime` pode ser diferente de `date`
- [ ] Não confundir `date` com `createdAt`
- [ ] `isBooked` = false
- [ ] `isAvailable` = true

## ✅ Resumo

| Campo | Uso | Filtrar? |
|-------|-----|----------|
| `date` | Data da consulta | ✅ SIM |
| `startTime` | Horário início | ❌ NÃO |
| `endTime` | Horário fim | ❌ NÃO |
| `createdAt` | Criação registro | ❌ NÃO |
| `updatedAt` | Última atualização | ❌ NÃO |

## 🎯 Importante

**SEMPRE use o campo `date` para:**
- Verificar se consulta está no futuro
- Filtrar disponibilidades
- Ordenar slots

**NÃO use `createdAt` ou `startTime` para filtrar!**

---

**Correção aplicada em:** 16/11/2025  
**Commit:** 7ae2b52

