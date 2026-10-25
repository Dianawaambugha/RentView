package com.example.rentview

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rentview.ui.PropertyAdapter
import com.example.rentview.viewmodel.PropertyListState
import com.example.rentview.viewmodel.PropertyListViewModel
import kotlinx.coroutines.launch

// Tenant screen: browse all approved properties.
class PropertyListActivity : AppCompatActivity() {

    private val viewModel: PropertyListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_property_list)

        val userId = intent.getLongExtra("USER_ID", -1L)

        val recycler = findViewById<RecyclerView>(R.id.rvProperties)
        val progress = findViewById<ProgressBar>(R.id.progressProperties)
        val stateLayout = findViewById<LinearLayout>(R.id.layoutPropertiesState)
        val stateText = findViewById<TextView>(R.id.tvPropertiesState)
        val stateButton = findViewById<Button>(R.id.btnPropertiesStateAction)
        val backButton = findViewById<Button>(R.id.btnPropertiesBack)

        val adapter = PropertyAdapter { property ->
            val detailsIntent = Intent(this, PropertyDetailsActivity::class.java)
            detailsIntent.putExtra("PROPERTY_ID", property.id)
            detailsIntent.putExtra("USER_ID", userId)
            startActivity(detailsIntent)
        }

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        backButton.setOnClickListener { finish() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->

                    recycler.visibility = View.GONE
                    progress.visibility = View.GONE
                    stateLayout.visibility = View.GONE

                    when (state) {
                        is PropertyListState.Loading -> {
                            progress.visibility = View.VISIBLE
                        }

                        is PropertyListState.Success -> {
                            adapter.submitList(state.items)
                            recycler.visibility = View.VISIBLE
                        }

                        is PropertyListState.Empty -> {
                            stateText.text = "No properties are available yet. " +
                                    "Check back soon, owners are still listing houses."
                            // TESTING ONLY: lets you try the screens before real
                            // approved properties exist. Remove before submission.
                            stateButton.text = "Load sample properties (testing)"
                            stateButton.setOnClickListener { viewModel.loadSampleData() }
                            stateLayout.visibility = View.VISIBLE
                        }

                        is PropertyListState.Error -> {
                            stateText.text = state.message
                            stateButton.text = "Try again"
                            stateButton.setOnClickListener { viewModel.load() }
                            stateLayout.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }
}
