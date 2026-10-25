package com.example.rentview

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rentview.data.ViewingTime
import com.example.rentview.ui.ViewingAdapter
import com.example.rentview.viewmodel.MyViewingsState
import com.example.rentview.viewmodel.MyViewingsViewModel
import kotlinx.coroutines.launch

// Tenant screen: list of booked viewings, with reschedule and cancel.
class MyViewingsActivity : AppCompatActivity() {

    private val viewModel: MyViewingsViewModel by viewModels()
    private var userId = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_viewings)

        userId = intent.getLongExtra("USER_ID", -1L)

        val recycler = findViewById<RecyclerView>(R.id.rvViewings)
        val progress = findViewById<ProgressBar>(R.id.progressViewings)
        val stateLayout = findViewById<LinearLayout>(R.id.layoutViewingsState)
        val stateText = findViewById<TextView>(R.id.tvViewingsState)
        val stateButton = findViewById<Button>(R.id.btnViewingsStateAction)
        val backButton = findViewById<Button>(R.id.btnViewingsBack)

        val adapter = ViewingAdapter(
            onReschedule = { viewing ->
                val rescheduleIntent = Intent(this, ScheduleViewingActivity::class.java)
                rescheduleIntent.putExtra("VIEWING_ID", viewing.id)
                rescheduleIntent.putExtra("USER_ID", userId)
                startActivity(rescheduleIntent)
            },
            onCancel = { viewing ->
                AlertDialog.Builder(this)
                    .setTitle("Cancel viewing")
                    .setMessage(
                        "Cancel your viewing of ${viewing.propertyTitle} on " +
                                "${ViewingTime.displayDate(viewing.date)} at ${viewing.timeSlot}?"
                    )
                    .setPositiveButton("Cancel viewing") { _, _ ->
                        viewModel.cancel(viewing, userId)
                    }
                    .setNegativeButton("Keep it", null)
                    .show()
            }
        )

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        backButton.setOnClickListener { finish() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { state ->

                        recycler.visibility = View.GONE
                        progress.visibility = View.GONE
                        stateLayout.visibility = View.GONE

                        when (state) {
                            is MyViewingsState.Loading -> {
                                progress.visibility = View.VISIBLE
                            }

                            is MyViewingsState.Success -> {
                                adapter.submitList(state.items)
                                recycler.visibility = View.VISIBLE
                            }

                            is MyViewingsState.Empty -> {
                                stateText.text = "You have no viewings booked yet."
                                stateButton.text = "Browse properties"
                                stateButton.setOnClickListener { openPropertyList() }
                                stateLayout.visibility = View.VISIBLE
                            }

                            is MyViewingsState.Error -> {
                                stateText.text = state.message
                                stateButton.text = "Try again"
                                stateButton.setOnClickListener { viewModel.load(userId) }
                                stateLayout.visibility = View.VISIBLE
                            }
                        }
                    }
                }

                launch {
                    viewModel.message.collect { message ->
                        if (message != null) {
                            Toast.makeText(
                                this@MyViewingsActivity,
                                message,
                                Toast.LENGTH_LONG
                            ).show()
                            viewModel.messageShown()
                        }
                    }
                }
            }
        }
    }

    // Reload whenever the screen comes back (e.g. after rescheduling).
    override fun onResume() {
        super.onResume()
        viewModel.load(userId)
    }

    private fun openPropertyList() {
        val listIntent = Intent(this, PropertyListActivity::class.java)
        listIntent.putExtra("USER_ID", userId)
        startActivity(listIntent)
    }
}
