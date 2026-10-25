package com.example.rentvew.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rentvew.R
import com.example.rentvew.data.PropertyItem

// Shows the list of approved properties. RecyclerView only creates enough
// card views to fill the screen and reuses them while the user scrolls.
class PropertyAdapter(
    private val onPropertyClicked: (PropertyItem) -> Unit
) : ListAdapter<PropertyItem, PropertyAdapter.PropertyViewHolder>(DIFF) {

    class PropertyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvPropertyTitle)
        val location: TextView = view.findViewById(R.id.tvPropertyLocation)
        val bedrooms: TextView = view.findViewById(R.id.tvPropertyBedrooms)
        val rent: TextView = view.findViewById(R.id.tvPropertyRent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PropertyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_property, parent, false)
        return PropertyViewHolder(view)
    }

    override fun onBindViewHolder(holder: PropertyViewHolder, position: Int) {
        val property = getItem(position)

        holder.title.text = property.title
        holder.location.text = property.location
        holder.bedrooms.text = Formatters.bedrooms(property.bedrooms)
        holder.rent.text = Formatters.rent(property.rent)

        holder.itemView.setOnClickListener { onPropertyClicked(property) }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PropertyItem>() {
            override fun areItemsTheSame(old: PropertyItem, new: PropertyItem) =
                old.id == new.id

            override fun areContentsTheSame(old: PropertyItem, new: PropertyItem) =
                old == new
        }
    }
}
