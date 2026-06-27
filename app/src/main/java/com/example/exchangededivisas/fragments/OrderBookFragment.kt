package com.example.exchangededivisas.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.exchangededivisas.R
import com.example.exchangededivisas.api.ApiClient
import com.example.exchangededivisas.api.OrderService
import com.example.exchangededivisas.models.OrderBook
import com.example.exchangededivisas.models.OrderItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OrderBookFragment : Fragment() {

    private lateinit var tvCurrencyPair: TextView
    private lateinit var lvBuyOrders: ListView
    private lateinit var lvSellOffers: ListView
    private lateinit var tvNoBuyOrders: TextView
    private lateinit var tvNoSellOffers: TextView
    private lateinit var orderService: OrderService

    private var currentCurrencyPair = "USD/PEN"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_order_book, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar vistas
        tvCurrencyPair = view.findViewById(R.id.tv_currency_pair)
        lvBuyOrders = view.findViewById(R.id.lv_buy_orders)
        lvSellOffers = view.findViewById(R.id.lv_sell_offers)
        tvNoBuyOrders = view.findViewById(R.id.tv_no_buy_orders)
        tvNoSellOffers = view.findViewById(R.id.tv_no_sell_offers)

        // Inicializar API
        orderService = ApiClient.getClient().create(OrderService::class.java)

        // Cargar datos iniciales
        cargarLibroOrdenes()
    }

    private fun cargarLibroOrdenes() {
        orderService.getOrderBook(currentCurrencyPair).enqueue(object : Callback<OrderBook> {
            override fun onResponse(call: Call<OrderBook>, response: Response<OrderBook>) {
                if (response.isSuccessful) {
                    val orderBook = response.body() ?: return

                    // Mostrar par de monedas
                    tvCurrencyPair.text = orderBook.currencyPair

                    // Mostrar órdenes de compra (derecha) - mayor a menor precio
                    val buyOrdersSorted = orderBook.buyOrders
                        .sortedByDescending { it.unitPrice }
                        .take(5) // Solo los 5 primeros

                    if (buyOrdersSorted.isEmpty()) {
                        lvBuyOrders.visibility = View.GONE
                        tvNoBuyOrders.visibility = View.VISIBLE
                    } else {
                        lvBuyOrders.visibility = View.VISIBLE
                        tvNoBuyOrders.visibility = View.GONE
                        lvBuyOrders.adapter = OrderAdapter(buyOrdersSorted)
                    }

                    // Mostrar ofertas de venta (izquierda) - menor a mayor precio
                    val sellOffersSorted = orderBook.sellOffers
                        .sortedBy { it.unitPrice }
                        .take(5) // Solo los 5 primeros

                    if (sellOffersSorted.isEmpty()) {
                        lvSellOffers.visibility = View.GONE
                        tvNoSellOffers.visibility = View.VISIBLE
                    } else {
                        lvSellOffers.visibility = View.VISIBLE
                        tvNoSellOffers.visibility = View.GONE
                        lvSellOffers.adapter = OrderAdapter(sellOffersSorted)
                    }
                }
            }

            override fun onFailure(call: Call<OrderBook>, t: Throwable) {
                Toast.makeText(
                    context,
                    "Error al cargar libro de órdenes: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    // Adapter personalizado para mostrar órdenes
    private inner class OrderAdapter(private val orders: List<OrderItem>) :
        ArrayAdapter<OrderItem>(requireContext(), 0, orders) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_order, parent, false)

            val order = orders[position]

            val tvQuantity = view.findViewById<TextView>(R.id.tv_quantity)
            val tvPrice = view.findViewById<TextView>(R.id.tv_price)

            tvQuantity.text = "Qty: ${order.quantity}"
            tvPrice.text = "S/. ${order.unitPrice}"

            return view
        }
    }

    fun actualizar() {
        cargarLibroOrdenes()
    }
}

