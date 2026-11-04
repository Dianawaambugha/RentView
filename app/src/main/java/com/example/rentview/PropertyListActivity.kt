package com.example.rentview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rentview.data.PropertyFilter
import com.example.rentview.ui.PropertyAdapter
import com.example.rentview.viewmodel.PropertyListState
import com.example.rentview.viewmodel.PropertyListViewModel
import kotlinx.coroutines.launch

// Tenant screen: browse approved properties, search them and filter them.
class PropertyListActivity : AppCompatActivity() {

    private val viewModel: PropertyListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_property_list)

        val userId = intent.getLongExtra("USER_ID", -1L)

        val searchInput = findViewById<EditText>(R.id.etSearch)
        val minRentInput = findViewById<EditText>(R.id.etMinRent)
        val maxRentInput = findViewById<EditText>(R.id.etMaxRent)
        val minBedroomsInput = findViewById<EditText>(R.id.etMinBedrooms)
        val resultCount = findViewById<TextView>(R.id.tvResultCount)
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

        // Read all four fields and hand the result to the ViewModel.
        val applyFilter = {
            val minRent = minRentInput.text.toString().trim().toDoubleOrNull()
            val maxRent = maxRentInput.text.toString().trim().toDoubleOrNull()
            val minBedrooms = minBedroomsInput.text.toString().trim().toIntOrNull() ?: 0

            maxRentInput.error =
                if (minRent != null && maxRent != null && minRent > maxRent) {
                    "Max rent must be at least the min rent"
                } else {
                    null
                }

            viewModel.updateFilter(
                PropertyFilter(
                    query = searchInput.text.toString(),
                    minRent = minRent,
                    maxRent = maxRent,
                    minBedrooms = minBedrooms
                )
            )
        }

        searchInput.doAfterTextChanged { applyFilter() }
        minRentInput.doAfterTextChanged { applyFilter() }
        maxRentInput.doAfterTextChanged { applyFilter() }
        minBedroomsInput.doAfterTextChanged { applyFilter() }

        // The keyboard's search button just closes the keyboard.
        // The list has already been filtered while the tenant was typing.
        searchInput.setOnEditorActionListener { view, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
                view.clearFocus()
                true
            } else {
                false
            }
        }

        val clearFilters = {
            searchInput.text.clear()
            minRentInput.text.clear()
            maxRentInput.text.clear()
            minBedroomsInput.text.clear()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->

                    recycler.visibility = View.GONE
                    progress.visibility = View.GONE
                    stateLayout.visibility = View.GONE
                    resultCount.visibility = View.GONE

                    when (state) {
                        is PropertyListState.Loading -> {
                            progress.visibility = View.VISIBLE
                        }

                        is PropertyListState.Success -> {
                            adapter.submitList(state.items)
                            recycler.visibility = View.VISIBLE

                            // Only mention the count when a filter is hiding some.
                            if (state.items.size != state.total) {
                                resultCount.text =
                                    "Showing ${state.items.size} of ${state.total} properties"
                                resultCount.visibility = View.VISIBLE
                            }
                        }

                        is PropertyListState.NoMatches -> {
                            stateText.text = "No properties match your search or filters."
                            stateButton.text = "Clear filters"
                            stateButton.setOnClickListener { clearFilters() }
                            stateLayout.visibility = View.VISIBLE
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
