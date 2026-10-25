package com.example.rentview.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rentview.R
import com.example.rentview.data.ViewingItem
import com.example.rentview.data.ViewingTime

// Shows the tenant's booked viewings, each with Reschedule and Cancel buttons.
class ViewingAdapter(
    private val onReschedule: (ViewingItem) -> Unit,
    private val onCancel: (ViewingItem) -> Unit
) : ListAdapter<ViewingItem, ViewingAdapter.ViewingViewHolder>(DIFF) {

    class ViewingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvViewingTitle)
        val location: TextView = view.findViewById(R.id.tvViewingLocation)
        val whenText: TextView = view.findViewById(R.id.tvViewingWhen)
        val rescheduleButton: Button = view.findViewById(R.id.btnReschedule)
        val cancelButton: Button = view.findViewById(R.id.btnCancelViewing)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_viewing, parent, false)
        return ViewingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewingViewHolder, position: Int) {
        val viewing = getItem(position)

        holder.title.text = viewing.propertyTitle
        holder.location.text = viewing.propertyLocation
        holder.whenText.text =
            "${ViewingTime.displayDate(viewing.date)} at ${viewing.timeSlot}"

        holder.rescheduleButton.setOnClickListener { onReschedule(viewing) }
        holder.cancelButton.setOnClickListener { onCancel(viewing) }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ViewingItem>() {
            override fun areItemsTheSame(old: ViewingItem, new: ViewingItem) =
                old.id == new.id

            override fun areContentsTheSame(old: ViewingItem, new: ViewingItem) =
                old == new
        }
    }
}
