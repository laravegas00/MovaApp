package edu.pmdm.movaapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import edu.pmdm.movaapp.databinding.ItemReservaBinding
import edu.pmdm.movaapp.models.Reservation
import java.text.SimpleDateFormat
import java.util.Locale

class ReservationAdapter : RecyclerView.Adapter<ReservationAdapter.ReservaViewHolder>() {

    private var reservas: List<Reservation> = listOf()

    fun setReservas(nuevasReservas: List<Reservation>) {
        reservas = nuevasReservas
        notifyDataSetChanged()
    }

    inner class ReservaViewHolder(private val binding: ItemReservaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(reserva: Reservation) {
            val from = reserva.outboundFlight["from"]?.toString() ?: "Origen"
            val to = reserva.outboundFlight["to"]?.toString() ?: "Destino"
            val departureRaw = reserva.outboundFlight["departure"]?.toString() ?: ""
            val arrivalRaw = reserva.returnFlight["arrival"]?.toString() ?: ""

            // Formateo de fechas
            val formatoEntrada = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val formatoSalida = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

            val fechaIda = try {
                val date = formatoEntrada.parse(departureRaw)
                formatoSalida.format(date ?: "")
            } catch (e: Exception) {
                "-"
            }

            val fechaVuelta = try {
                val date = formatoEntrada.parse(arrivalRaw)
                formatoSalida.format(date ?: "")
            } catch (e: Exception) {
                "-"
            }

            // Asignar a la vista
            binding.tvReservaRuta.text = "$from ➝ $to"
            binding.tvReservaFecha.text = "$fechaIda - $fechaVuelta"
            binding.tvReservaPrecio.text = reserva.totalPrice
        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservaViewHolder {
        val binding = ItemReservaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReservaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReservaViewHolder, position: Int) {
        holder.bind(reservas[position])
    }

    override fun getItemCount(): Int = reservas.size
}
