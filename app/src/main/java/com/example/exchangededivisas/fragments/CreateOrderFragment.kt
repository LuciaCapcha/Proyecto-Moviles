package com.example.exchangededivisas.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.exchangededivisas.R
import com.example.exchangededivisas.api.ApiClient
import com.example.exchangededivisas.api.OrderService
import com.example.exchangededivisas.models.CurrentPrice
import com.example.exchangededivisas.models.Order
import com.example.exchangededivisas.models.Wallet
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class CreateOrderFragment : Fragment() {

    private lateinit var etCurrencyPair: Spinner
    private lateinit var tvHighestPrice: TextView
    private lateinit var etQuantity: EditText
    private lateinit var etUnitPrice: EditText
    private lateinit var tvTotal: TextView
    private lateinit var tvValidationMessage: TextView
    private lateinit var btnConfirm: Button
    private lateinit var orderService: OrderService

    private var currentPrice = CurrentPrice()
    private var userWallet = Wallet()
    private var userId = "user_123"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create_order, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etCurrencyPair = view.findViewById(R.id.spinner_currency_pair)
        tvHighestPrice = view.findViewById(R.id.tv_highest_price)
        etQuantity = view.findViewById(R.id.et_quantity)
        etUnitPrice = view.findViewById(R.id.et_unit_price)
        tvTotal = view.findViewById(R.id.tv_total)
        tvValidationMessage = view.findViewById(R.id.tv_validation_message)
        btnConfirm = view.findViewById(R.id.btn_confirm_order)

        orderService = ApiClient.getClient().create(OrderService::class.java)

        cargarPrecioActual()
        cargarWallet()
        configurarListeners()
    }

    private fun cargarPrecioActual() {
        orderService.getCurrentPrice("USD/PEN").enqueue(object : Callback<CurrentPrice> {
            override fun onResponse(call: Call<CurrentPrice>, response: Response<CurrentPrice>) {
                if (response.isSuccessful) {
                    currentPrice = response.body() ?: return
                    tvHighestPrice.text = "Precio más alto: S/. ${currentPrice.highestBuyPrice}"
                    etUnitPrice.setText(currentPrice.highestBuyPrice.toString())
                }
            }

            override fun onFailure(call: Call<CurrentPrice>, t: Throwable) {
                Toast.makeText(context, "Error al cargar precio: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun cargarWallet() {
        orderService.getWallet(userId).enqueue(object : Callback<Wallet> {
            override fun onResponse(call: Call<Wallet>, response: Response<Wallet>) {
                if (response.isSuccessful) {
                    userWallet = response.body() ?: return
                }
            }

            override fun onFailure(call: Call<Wallet>, t: Throwable) {
                Toast.makeText(context, "Error al cargar wallet: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun configurarListeners() {
        etQuantity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calcularTotal()
                validarFormulario()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        etUnitPrice.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calcularTotal()
                validarFormulario()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnConfirm.setOnClickListener {
            if (validarFormulario()) {
                generarOrden()
            }
        }
    }

    private fun calcularTotal() {
        try {
            val quantity = etQuantity.text.toString().toDoubleOrNull() ?: 0.0
            val unitPrice = etUnitPrice.text.toString().toDoubleOrNull() ?: 0.0
            val total = quantity * unitPrice
            tvTotal.text = "Total: S/. %.2f".format(total)
        } catch (e: Exception) {
            tvTotal.text = "Total: S/. 0.00"
        }
    }

    private fun validarFormulario(): Boolean {
        val quantity = etQuantity.text.toString().toDoubleOrNull()
        val unitPrice = etUnitPrice.text.toString().toDoubleOrNull()
        val total = (quantity ?: 0.0) * (unitPrice ?: 0.0)

        if (etQuantity.text.isEmpty() || etUnitPrice.text.isEmpty()) {
            tvValidationMessage.text = ""
            btnConfirm.isEnabled = false
            return false
        }

        if (quantity == null || quantity <= 0 || unitPrice == null || unitPrice <= 0) {
            tvValidationMessage.text = "❌ Valor inválido"
            tvValidationMessage.setTextColor(resources.getColor(android.R.color.holo_red_dark))
            btnConfirm.isEnabled = false
            return false
        }

        val saldoDisponible = userWallet.balance - userWallet.committedAmount
        if (total > saldoDisponible) {
            tvValidationMessage.text = "❌ Saldo insuficiente (Disponible: S/. %.2f)".format(saldoDisponible)
            tvValidationMessage.setTextColor(resources.getColor(android.R.color.holo_red_dark))
            btnConfirm.isEnabled = false
            return false
        }

        if (quantity > currentPrice.totalLiquidity) {
            tvValidationMessage.text = "❌ Liquidez insuficiente"
            tvValidationMessage.setTextColor(resources.getColor(android.R.color.holo_red_dark))
            btnConfirm.isEnabled = false
            return false
        }

        tvValidationMessage.text = "✅ Orden válida"
        tvValidationMessage.setTextColor(resources.getColor(android.R.color.holo_green_dark))
        btnConfirm.isEnabled = true
        return true
    }

    private fun generarOrden() {
        val quantity = etQuantity.text.toString().toDouble()
        val unitPrice = etUnitPrice.text.toString().toDouble()
        val total = quantity * unitPrice

        val order = Order(
            userId = userId,
            currencyPair = "USD/PEN",
            quantity = quantity,
            unitPrice = unitPrice,
            total = total,
            walletCurrency = userWallet.currency,
            status = "pendiente"
        )

        orderService.createOrder(order).enqueue(object : Callback<Order> {
            override fun onResponse(call: Call<Order>, response: Response<Order>) {
                if (response.isSuccessful) {
                    val createdOrder = response.body()
                    Toast.makeText(context, "✅ Orden #${createdOrder?.orderId} creada", Toast.LENGTH_LONG).show()
                    limpiarFormulario()
                    cargarWallet()
                } else {
                    Toast.makeText(context, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Order>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun limpiarFormulario() {
        etQuantity.setText("")
        etUnitPrice.setText(currentPrice.highestBuyPrice.toString())
        tvTotal.text = "Total: S/. 0.00"
        tvValidationMessage.text = ""
        btnConfirm.isEnabled = false
    }
}

