package com.saif.jobnet;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.saif.jobnet.Activities.AddEducationActivity;

import java.util.ArrayList;
import java.util.List;

public class BottomSheetFragment extends BottomSheetDialogFragment {

    private ArrayList<String> items;
    private ArrayAdapter<String> adapter;
    private Context context;

    public BottomSheetFragment(Context context,ArrayList<String> items) {
        this.context = context;
        this.items = items;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_layout, container, false);

        EditText searchBar = view.findViewById(R.id.searchBar);
        ListView listView = view.findViewById(R.id.listView);

        // Set Adapter
        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, items);
        listView.setAdapter(adapter);


        listView.setOnItemClickListener((parent, view1, position, id) -> {
            String selectedCourse = adapter.getItem(position);
            ((AddEducationActivity) context).setSelectedCourse(selectedCourse);
            dismiss();
        });

        // Search bar functionality
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                adapter.getFilter().filter(charSequence);
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) { }

            @Override
            public void afterTextChanged(Editable editable) { }
        });

        return view;
    }

}
