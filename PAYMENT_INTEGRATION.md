# Integração de Pagamento Stripe - PsyConnect Android

## 📋 Visão Geral

Sistema completo de gateway de pagamento integrado com Stripe para agendamento de consultas psicológicas.

## 🏗️ Arquitetura

### Componentes Principais

1. **StripeService.kt** - Serviço de integração com API Stripe
2. **PaymentActivity.kt** - Activity que processa pagamentos via WebView
3. **BookingConfirmationActivity.kt** - Modificada para iniciar fluxo de pagamento

## 🔄 Fluxo de Pagamento

```
1. Paciente visualiza perfil do doutor (DoctorProfileActivity)
   ↓
2. Seleciona horário disponível
   ↓
3. Confirma agendamento (BookingConfirmationActivity)
   ↓
4. Inicia pagamento (PaymentActivity)
   ↓
5. Stripe Checkout Session (WebView)
   ↓
6. Pagamento processado
   ↓
7. Consulta criada no Firestore
   ↓
8. Slot marcado como ocupado
   ↓
9. Retorna ao Dashboard
```

## 🔌 API Endpoints Utilizados

### Base URL
```
https://stripe-backend-psyconnect.onrender.com
```

### Endpoints

#### 1. Create Payment Intent
```http
POST /createPaymentIntent
Content-Type: application/json

{
  "amount": 5000,           // Amount in cents (50.00 EUR)
  "currency": "eur",
  "doctorId": "doctor123",
  "patientId": "patient456",
  "appointmentId": "apt789" // Optional
}

Response:
{
  "clientSecret": "pi_xxx_secret_yyy",
  "paymentIntentId": "pi_xxx"
}
```

#### 2. Create Checkout Session (Usado no app)
```http
POST /createCheckoutSession
Content-Type: application/json

{
  "amount": 5000,
  "currency": "eur",
  "doctorId": "doctor123",
  "patientId": "patient456",
  "successUrl": "psyconnect://payment/success",
  "cancelUrl": "psyconnect://payment/cancel"
}

Response:
{
  "url": "https://checkout.stripe.com/c/pay/cs_xxx",
  "sessionId": "cs_xxx"
}
```

#### 3. Check Payment Status
```http
GET /checkPaymentStatus?paymentIntentId=pi_xxx

Response:
{
  "status": "succeeded",
  "amount": 5000,
  "currency": "eur"
}
```

#### 4. Create Onboarding Link (Para doutores)
```http
POST /createOnboardingLink
Content-Type: application/json

{
  "doctorId": "doctor123",
  "email": "doctor@example.com",
  "refreshUrl": "https://app.psyconnect.com/refresh",
  "returnUrl": "https://app.psyconnect.com/return"
}

Response:
{
  "url": "https://connect.stripe.com/setup/xxx"
}
```

## 💰 Cálculo de Valores

### Modelo de Negócio
A plataforma retém **10% do valor da consulta** do pagamento ao doutor.

### Preço Total
```kotlin
val priceInCents = doctor.priceEurCents  // Ex: 5000 (50.00 EUR)
val totalAmountInCents = priceInCents    // Paciente paga o valor da consulta
val platformFee = (priceInCents * 0.10).toInt()  // 10% fica com a plataforma
val doctorReceives = priceInCents - platformFee  // 90% vai para o doutor
```

### Exemplos de Cálculo
- Consulta de €50,00:
  - Paciente paga: **€50,00**
  - Plataforma recebe: **€5,00** (10%)
  - Doutor recebe: **€45,00** (90%)
  
- Consulta de €80,00:
  - Paciente paga: **€80,00**
  - Plataforma recebe: **€8,00** (10%)
  - Doutor recebe: **€72,00** (90%)
  
- Consulta de €100,00:
  - Paciente paga: **€100,00**
  - Plataforma recebe: **€10,00** (10%)
  - Doutor recebe: **€90,00** (90%)

## 📱 Deep Links

O app está configurado para responder aos seguintes deep links:

- `psyconnect://payment/success` - Pagamento bem-sucedido
- `psyconnect://payment/cancel` - Pagamento cancelado

### Configuração no AndroidManifest.xml
```xml
<activity android:name=".PaymentActivity">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="psyconnect" android:host="payment" />
    </intent-filter>
</activity>
```

## 🔐 Segurança

1. **HTTPS Only** - Todas as chamadas à API usam HTTPS
2. **Client Secret** - Usado para autenticação de pagamento
3. **Webhook Verification** - Backend verifica assinaturas do Stripe
4. **Payment Status Check** - Verificação do status antes de criar consulta

## 📊 Estados de Pagamento

### Status no Firestore
```kotlin
paymentStatus: "paid" | "pending" | "failed" | "refunded"
```

### Status da Consulta
```kotlin
status: "confirmed" | "completed" | "cancelled"
```

## 🗄️ Estrutura de Dados

### Appointment Document (Firestore)
```javascript
{
  patientId: "uid_patient",
  doctorId: "uid_doctor",
  startTime: Timestamp,
  endTime: Timestamp,
  status: "confirmed",
  paymentStatus: "paid",
  paymentAmount: 5250,
  sessionId: "cs_xxx",
  createdAt: Timestamp,
  doctorName: "Dr. João Silva",
  patientName: "Maria Santos"
}
```

## 🔄 Webhooks Suportados (Backend)

O backend processa os seguintes webhooks do Stripe:

1. **payment_intent.succeeded** - Atualiza status da consulta
2. **payment_intent.payment_failed** - Processa falhas
3. **account.updated** - Atualiza status de onboarding do doutor
4. **checkout.session.completed** - Processa conclusão do checkout
5. **charge.dispute.created** - Registra disputas

## 🧪 Teste do Fluxo

### Cartões de Teste Stripe

```
Sucesso:
4242 4242 4242 4242
Exp: Qualquer data futura
CVC: Qualquer 3 dígitos
CEP: Qualquer

Falha (Card Declined):
4000 0000 0000 0002
```

## 📝 Logs de Debug

### StripeService
```kotlin
Log.d("StripeService", "Creating Payment Intent: $jsonBody")
Log.d("StripeService", "Payment Intent Created: $jsonResponse")
```

### PaymentActivity
```kotlin
Log.d("PaymentActivity", "✅ Payment successful!")
Log.d("PaymentActivity", "✅ Appointment created: ${documentReference.id}")
```

### BookingConfirmationActivity
```kotlin
Log.d("BookingConfirmation", "Starting payment flow - Amount: $totalAmountInCents cents")
```

## 🚀 Próximos Passos

1. ✅ Integração básica completa
2. ⏳ Adicionar suporte para cupons de desconto
3. ⏳ Implementar reembolsos
4. ⏳ Dashboard de pagamentos para doutores
5. ⏳ Histórico de pagamentos para pacientes
6. ⏳ Integração com Stripe Connect para doutores

## 🐛 Troubleshooting

### Erro: "HTTP Error 400"
- Verifique se todos os parâmetros obrigatórios estão sendo enviados
- Confirme formato do JSON

### Pagamento não redireciona
- Verifique deep links no AndroidManifest
- Confirme que `successUrl` e `cancelUrl` estão corretos

### Consulta não é criada após pagamento
- Verifique logs do Firestore
- Confirme permissões de escrita
- Verifique se `doctorId`, `patientId` e `slotId` são válidos

## 📞 Suporte

Para problemas com a API Stripe:
- Documentação: https://stripe.com/docs
- Dashboard: https://dashboard.stripe.com

## 🔑 Variáveis de Ambiente (Backend)

```env
STRIPE_SECRET_KEY=sk_test_xxx
STRIPE_WEBHOOK_SECRET=whsec_xxx
```

## ✅ Checklist de Implementação

- [x] StripeService criado
- [x] PaymentActivity implementada
- [x] WebView configurado
- [x] Deep links configurados
- [x] BookingConfirmationActivity atualizada
- [x] AndroidManifest.xml atualizado
- [x] Layout de pagamento criado
- [x] Logs de debug adicionados
- [x] Tratamento de erros implementado
- [x] Criação de consulta após pagamento
- [x] Atualização de slot após pagamento

## 📄 Licença

Propriedade de PsyConnect © 2024

