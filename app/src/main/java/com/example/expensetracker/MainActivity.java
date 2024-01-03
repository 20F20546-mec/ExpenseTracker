package com.example.expensetracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.expensetracker.databinding.ActivityMainBinding;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnItemsClick{
    public ActivityMainBinding binding;
    private ExpensesAdapter expensesAdapter;
    Intent intent;
    private long income = 0, expense = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        expensesAdapter = new ExpensesAdapter(this, this);
        binding.recycler.setAdapter(expensesAdapter);
        binding.recycler.setLayoutManager(new LinearLayoutManager(this));

        intent = new Intent(MainActivity.this, ExpenseActivity.class);

        binding.income.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                intent.putExtra("type", "Income");
                startActivity(intent);
            }
        });

        binding.expense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                intent.putExtra("type", "Expense");
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please");
        progressDialog.setMessage("Wait");
        progressDialog.setCancelable(false);

        if(FirebaseAuth.getInstance().getCurrentUser() == null) {
            progressDialog.show();
            FirebaseAuth.getInstance()
                    .signInAnonymously()
                    .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            progressDialog.cancel();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.cancel();
                            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        income = 0;
        expense = 0;
        getData();
    }

    private void getData() {
        FirebaseFirestore
                .getInstance()
                .collection("expenses")
                .whereEqualTo("uid", FirebaseAuth.getInstance().getUid())
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        expensesAdapter.clear();
                        List<DocumentSnapshot> dslist = queryDocumentSnapshots.getDocuments();
                        for (DocumentSnapshot ds:dslist) {
                            ExpenseModel expenseModel = ds.toObject(ExpenseModel.class);

                            if (expenseModel.getType().equals("Income")) {
                                income += expenseModel.getAmount();
                            }
                            else {
                                expense += expenseModel.getAmount();
                            }

                            expensesAdapter.add(expenseModel);
                        }

                        setUpGraph();
                    }
                });
    }

    private void setUpGraph() {
        PieChart chart = (PieChart)findViewById(R.id.pieChart);
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colorsList = new ArrayList<>();
        if(income != 0) {
            entries.add(new PieEntry(income, "Income"));
            colorsList.add(getResources().getColor(R.color.teal_700));
        }
        if(expense != 0) {
            entries.add(new PieEntry(expense, "Expense"));
            colorsList.add(getResources().getColor(R.color.red));
        }
        PieDataSet set = new PieDataSet(entries, "\n Final Balance = " + String.valueOf(income-expense));
        set.setColors(colorsList);
        set.setValueTextColor(getResources().getColor(R.color.white));
        set.setValueTextSize(10);
        PieData data = new PieData(set);
        binding.pieChart.setCenterTextSize(20);
        binding.pieChart.setData(data);
        binding.pieChart.invalidate();
    }

    @Override
    public void onClick(ExpenseModel expenseModel) {
        intent.putExtra("model", expenseModel);
        startActivity(intent);
    }
}