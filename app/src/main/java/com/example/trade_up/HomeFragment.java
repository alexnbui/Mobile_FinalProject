package com.example.trade_up;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Simple TextView for Home
        android.widget.TextView textView = new android.widget.TextView(getContext());
        textView.setText("Welcome to Home!");
        textView.setTextSize(24);
        textView.setGravity(android.view.Gravity.CENTER);
        return textView;
    }
}

