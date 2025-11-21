package com.example.psyconnectandroid

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

/**
 * Dialog para configurar o Stripe Account ID
 */
class StripeConfigurationDialog(
    private val onSave: (accountId: String) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_stripe_configuration, null)
        val etAccountId: EditText = view.findViewById(R.id.etAccountId)
        val btnSave: Button = view.findViewById(R.id.btnSave)
        val btnInstructions: Button = view.findViewById(R.id.btnInstructions)

        btnInstructions.setOnClickListener {
            showInstructions()
        }

        btnSave.setOnClickListener {
            val accountId = etAccountId.text.toString().trim()
            if (accountId.isEmpty()) {
                Toast.makeText(context, "Por favor, insira o Account ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (!accountId.startsWith("acct_")) {
                Toast.makeText(context, "Account ID deve começar com 'acct_'", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            onSave(accountId)
            dismiss()
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("Configurar Stripe")
            .setView(view)
            .setNegativeButton("Cancelar", null)
            .create()
    }

    private fun showInstructions() {
        AlertDialog.Builder(requireContext())
            .setTitle("Como obter o Account ID")
            .setMessage(
                "1. Acesse https://dashboard.stripe.com\n" +
                "2. Vá em Configurações > Conta\n" +
                "3. Copie o Account ID (formato: acct_xxxxxxxxxxxxxxxxxx)\n" +
                "4. Cole no campo acima"
            )
            .setPositiveButton("OK", null)
            .show()
    }
}

