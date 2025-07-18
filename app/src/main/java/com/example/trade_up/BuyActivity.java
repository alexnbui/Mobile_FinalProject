package com.example.trade_up;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class BuyActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private SearchView searchView;
    private BuyItemAdapter adapter;
    private List<Item> allItems = new ArrayList<>();
    private List<Item> filteredItems = new ArrayList<>();
    private FirebaseFirestore db;
    private Spinner spinnerCategory, spinnerCondition, spinnerSort;
    private EditText etMinPrice, etMaxPrice, etDistance;
    private Button btnUseGPS;
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private double userLat = 0, userLng = 0;
    private static final int REQUEST_LOCATION_PERMISSION = 1001;

    // Sectioned item for adapter
    private static class SectionedItem {
        enum Type { HEADER, ITEM }
        Type type;
        String header;
        Item item;
        SectionedItem(Type type, String header, Item item) {
            this.type = type; this.header = header; this.item = item;
        }
    }
    private List<SectionedItem> sectionedList = new ArrayList<>();
    private SectionedBuyItemAdapter sectionedAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buy);
        Toolbar toolbar = findViewById(R.id.toolbarBuy);
        setSupportActionBar(toolbar);
        recyclerView = findViewById(R.id.recyclerViewBuyItems);
        searchView = findViewById(R.id.searchViewBuy);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        sectionedAdapter = new SectionedBuyItemAdapter(sectionedList, item -> openItemDetail(item));
        recyclerView.setAdapter(sectionedAdapter);
        db = FirebaseFirestore.getInstance();
        loadItems();
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerCondition = findViewById(R.id.spinnerCondition);
        spinnerSort = findViewById(R.id.spinnerSort);
        etMinPrice = findViewById(R.id.etMinPrice);
        etMaxPrice = findViewById(R.id.etMaxPrice);
        etDistance = findViewById(R.id.etDistance);
        btnUseGPS = findViewById(R.id.btnUseGPS);
        // Populate spinners
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, getCategories());
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);
        ArrayAdapter<String> condAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, getConditions());
        condAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCondition.setAdapter(condAdapter);
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, getSortOptions());
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(sortAdapter);
        // Listeners for filters
        spinnerCategory.setOnItemSelectedListener(new SimpleFilterListener());
        spinnerCondition.setOnItemSelectedListener(new SimpleFilterListener());
        spinnerSort.setOnItemSelectedListener(new SimpleFilterListener());
        etMinPrice.addTextChangedListener(new SimpleTextWatcher(this::triggerDebouncedSearch));
        etMaxPrice.addTextChangedListener(new SimpleTextWatcher(this::triggerDebouncedSearch));
        etDistance.addTextChangedListener(new SimpleTextWatcher(this::triggerDebouncedSearch));
        btnUseGPS.setOnClickListener(v -> requestLocation());
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                triggerDebouncedSearch();
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                triggerDebouncedSearch();
                return true;
            }
        });
    }

    private void loadItems() {
        db.collection("items").whereEqualTo("status", "Available").get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                allItems.clear();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    Item item = Item.fromDocument(doc);
                    allItems.add(item);
                }
                filterItems(searchView.getQuery().toString());
            });
    }

    private void triggerDebouncedSearch() {
        if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
        searchRunnable = this::applyFiltersAndSort;
        searchHandler.postDelayed(searchRunnable, 200);
    }

    private void applyFiltersAndSort() {
        String query = searchView.getQuery().toString().toLowerCase();
        String selectedCat = spinnerCategory.getSelectedItem().toString();
        String selectedCond = spinnerCondition.getSelectedItem().toString();
        String minPriceStr = etMinPrice.getText().toString();
        String maxPriceStr = etMaxPrice.getText().toString();
        String distStr = etDistance.getText().toString();
        String sortOpt = spinnerSort.getSelectedItem().toString();
        double minPrice = minPriceStr.isEmpty() ? Double.MIN_VALUE : Double.parseDouble(minPriceStr);
        double maxPrice = maxPriceStr.isEmpty() ? Double.MAX_VALUE : Double.parseDouble(maxPriceStr);
        double maxDist = distStr.isEmpty() ? Double.MAX_VALUE : Double.parseDouble(distStr);
        filteredItems.clear();
        for (Item item : allItems) {
            boolean matches = true;
            if (!query.isEmpty() && !(item.title.toLowerCase().contains(query) || item.description.toLowerCase().contains(query) || (item.category != null && item.category.toLowerCase().contains(query)))) matches = false;
            if (!selectedCat.equals("All") && (item.category == null || !item.category.equals(selectedCat))) matches = false;
            if (!selectedCond.equals("All") && (item.condition == null || !item.condition.equals(selectedCond))) matches = false;
            if (item.price < minPrice || item.price > maxPrice) matches = false;
            if (maxDist != Double.MAX_VALUE && item.lat != 0 && item.lng != 0 && userLat != 0 && userLng != 0) {
                double dist = distance(userLat, userLng, item.lat, item.lng);
                if (dist > maxDist) matches = false;
            }
            if (matches) filteredItems.add(item);
        }
        // Sorting
        if (sortOpt.equals("Newest")) {
            filteredItems.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
        } else if (sortOpt.equals("Price ↑")) {
            filteredItems.sort((a, b) -> Double.compare(a.price, b.price));
        } else if (sortOpt.equals("Price ↓")) {
            filteredItems.sort((a, b) -> Double.compare(b.price, a.price));
        } // else: default order (relevance)
        // Build sectioned list
        sectionedList.clear();
        List<Item> recommended = getRecommendedItems(filteredItems);
        if (!recommended.isEmpty()) {
            sectionedList.add(new SectionedItem(SectionedItem.Type.HEADER, "Recommended", null));
            for (Item item : recommended) sectionedList.add(new SectionedItem(SectionedItem.Type.ITEM, null, item));
        }
        // Group by category
        java.util.Map<String, List<Item>> catMap = new java.util.LinkedHashMap<>();
        for (Item item : filteredItems) {
            if (recommended.contains(item)) continue; // skip already shown
            String cat = item.category != null ? item.category : "Other";
            if (!catMap.containsKey(cat)) catMap.put(cat, new ArrayList<>());
            catMap.get(cat).add(item);
        }
        for (String cat : catMap.keySet()) {
            sectionedList.add(new SectionedItem(SectionedItem.Type.HEADER, cat, null));
            for (Item item : catMap.get(cat)) sectionedList.add(new SectionedItem(SectionedItem.Type.ITEM, null, item));
        }
        sectionedAdapter.notifyDataSetChanged();
    }

    private List<Item> getRecommendedItems(List<Item> items) {
        // Recommend top 3 by views, or closest if location is set
        List<Item> sorted = new ArrayList<>(items);
        if (userLat != 0 && userLng != 0) {
            sorted.sort((a, b) -> Double.compare(distance(userLat, userLng, a.lat, a.lng), distance(userLat, userLng, b.lat, b.lng)));
        } else {
            sorted.sort((a, b) -> Integer.compare(b.views, a.views));
        }
        List<Item> rec = new ArrayList<>();
        for (Item item : sorted) {
            if (rec.size() >= 3) break;
            rec.add(item);
        }
        return rec;
    }

    private void filterItems(String query) {
        filteredItems.clear();
        if (TextUtils.isEmpty(query)) {
            filteredItems.addAll(allItems);
        } else {
            String lower = query.toLowerCase();
            for (Item item : allItems) {
                if (item.title.toLowerCase().contains(lower) ||
                    item.description.toLowerCase().contains(lower) ||
                    (item.category != null && item.category.toLowerCase().contains(lower))) {
                    filteredItems.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void openItemDetail(Item item) {
        Intent intent = new Intent(this, ItemDetailActivity.class);
        intent.putExtra("ITEM_ID", item.id);
        startActivity(intent);
    }

    private void requestLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                userLat = location.getLatitude();
                userLng = location.getLongitude();
                etDistance.setText("5"); // Default to 5km
                triggerDebouncedSearch();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestLocation();
        }
    }

    private double distance(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return earthRadius * c;
    }

    private List<String> getCategories() {
        List<String> cats = new ArrayList<>();
        cats.add("All");
        cats.add("Electronics");
        cats.add("Clothing");
        cats.add("Books");
        cats.add("Home");
        cats.add("Other");
        return cats;
    }

    private List<String> getConditions() {
        List<String> conds = new ArrayList<>();
        conds.add("All");
        conds.add("New");
        conds.add("Used");
        return conds;
    }

    private List<String> getSortOptions() {
        List<String> opts = new ArrayList<>();
        opts.add("Relevance");
        opts.add("Newest");
        opts.add("Price ↑");
        opts.add("Price ↓");
        return opts;
    }

    private class SimpleFilterListener implements android.widget.AdapterView.OnItemSelectedListener {
        @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { triggerDebouncedSearch(); }
        @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
    }

    private static class SimpleTextWatcher implements android.text.TextWatcher {
        private final Runnable callback;
        SimpleTextWatcher(Runnable callback) { this.callback = callback; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) { callback.run(); }
        @Override public void afterTextChanged(android.text.Editable s) {}
    }

    // Item model and adapter class definitions would go here or in separate files.

    // Item model for Firestore integration and recommendations
    public static class Item {
        public String id;
        public String title;
        public String description;
        public String category;
        public String condition;
        public double price;
        public String location;
        public double lat;
        public double lng;
        public int views; // For popularity
        public long timestamp;
        // ...add other fields as needed...

        public static Item fromDocument(com.google.firebase.firestore.DocumentSnapshot doc) {
            Item item = new Item();
            item.id = doc.getId();
            item.title = doc.getString("title");
            item.description = doc.getString("description");
            item.category = doc.getString("category");
            item.condition = doc.getString("condition");
            item.price = doc.getDouble("price") != null ? doc.getDouble("price") : 0;
            item.location = doc.getString("location");
            item.lat = doc.getDouble("lat") != null ? doc.getDouble("lat") : 0;
            item.lng = doc.getDouble("lng") != null ? doc.getDouble("lng") : 0;
            item.views = doc.getLong("views") != null ? doc.getLong("views").intValue() : 0;
            item.timestamp = doc.getLong("timestamp") != null ? doc.getLong("timestamp") : 0;
            // ...add other fields as needed...
            return item;
        }
    }

    // SectionedBuyItemAdapter implementation
    private static class SectionedBuyItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<SectionedItem> items;
        private final java.util.function.Consumer<Item> onItemClick;
        SectionedBuyItemAdapter(List<SectionedItem> items, java.util.function.Consumer<Item> onItemClick) {
            this.items = items; this.onItemClick = onItemClick;
        }
        @Override
        public int getItemViewType(int position) {
            return items.get(position).type == SectionedItem.Type.HEADER ? 0 : 1;
        }
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == 0) {
                View v = android.view.LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
                return new HeaderVH(v);
            } else {
                View v = android.view.LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
                return new ItemVH(v);
            }
        }
        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            SectionedItem si = items.get(position);
            if (si.type == SectionedItem.Type.HEADER) {
                ((HeaderVH)holder).tv.setText(si.header);
            } else {
                ((ItemVH)holder).tv1.setText(si.item.title);
                ((ItemVH)holder).tv2.setText(si.item.price + " | " + (si.item.category != null ? si.item.category : "Other"));
                holder.itemView.setOnClickListener(v -> onItemClick.accept(si.item));
            }
        }
        @Override
        public int getItemCount() { return items.size(); }
        static class HeaderVH extends RecyclerView.ViewHolder {
            android.widget.TextView tv;
            HeaderVH(View v) { super(v); tv = v.findViewById(android.R.id.text1); }
        }
        static class ItemVH extends RecyclerView.ViewHolder {
            android.widget.TextView tv1, tv2;
            ItemVH(View v) { super(v); tv1 = v.findViewById(android.R.id.text1); tv2 = v.findViewById(android.R.id.text2); }
        }
    }
}
