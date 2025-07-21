package com.example.trade_up;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity {
    private RecyclerView rvReports;
    private ReportAdapter reportAdapter;
    private List<Report> reportList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);
        rvReports = findViewById(R.id.rvReports);
        rvReports.setLayoutManager(new LinearLayoutManager(this));
        reportAdapter = new ReportAdapter(reportList);
        rvReports.setAdapter(reportAdapter);
        db = FirebaseFirestore.getInstance();
        loadReports();
    }

    private void loadReports() {
        db.collection("reports").get()
            .addOnSuccessListener(querySnapshot -> {
                reportList.clear();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    Long ts = doc.getLong("timestamp");
                    reportList.add(new Report(
                        doc.getString("reporterId"),
                        doc.getString("targetId"),
                        doc.getString("type"),
                        doc.getString("reason"),
                        ts != null ? ts : 0L
                    ));
                }
                reportAdapter.notifyDataSetChanged();
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Failed to load reports", Toast.LENGTH_SHORT).show());
    }

    private void showAdminActionsDialog(Report report) {
        String[] actions = {"Delete User/Listing", "Suspend User/Listing", "Warn User/Listing"};
        new android.app.AlertDialog.Builder(this)
            .setTitle("Admin Actions")
            .setItems(actions, (dialog, which) -> {
                switch (which) {
                    case 0:
                        performAdminAction(report, "delete");
                        break;
                    case 1:
                        performAdminAction(report, "suspend");
                        break;
                    case 2:
                        performAdminAction(report, "warn");
                        break;
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void performAdminAction(Report report, String action) {
        if (report.type.equals("user")) {
            db.collection("users").document(report.targetId)
                .update(action, true)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "User " + action + "d", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to " + action + " user", Toast.LENGTH_SHORT).show());
        } else if (report.type.equals("listing")) {
            db.collection("items").document(report.targetId)
                .update(action, true)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Listing " + action + "d", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to " + action + " listing", Toast.LENGTH_SHORT).show());
        } else if (report.type.equals("chat_message")) {
            // Optionally, delete or flag chat message
            Toast.makeText(this, "Chat message action: " + action, Toast.LENGTH_SHORT).show();
        }
    }

    // Report model
    public static class Report {
        public String reporterId, targetId, type, reason;
        public long timestamp;
        public Report(String reporterId, String targetId, String type, String reason, long timestamp) {
            this.reporterId = reporterId;
            this.targetId = targetId;
            this.type = type;
            this.reason = reason;
            this.timestamp = timestamp;
        }
    }

    // RecyclerView Adapter for reports
    public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {
        List<Report> reports;
        ReportAdapter(List<Report> reports) { this.reports = reports; }
        @Override
        public ReportViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ReportViewHolder(v);
        }
        @Override
        public void onBindViewHolder(ReportViewHolder holder, int position) {
            Report r = reports.get(position);
            holder.tv1.setText(r.type + " | " + r.reason);
            holder.tv2.setText("Reporter: " + r.reporterId + " | Target: " + r.targetId);
            holder.itemView.setOnClickListener(v -> {
                showAdminActionsDialog(r);
            });
        }
        @Override
        public int getItemCount() { return reports.size(); }
        class ReportViewHolder extends RecyclerView.ViewHolder {
            TextView tv1, tv2;
            ReportViewHolder(View itemView) {
                super(itemView);
                tv1 = itemView.findViewById(android.R.id.text1);
                tv2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}
