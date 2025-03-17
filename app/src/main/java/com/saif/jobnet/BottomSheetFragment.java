package com.saif.jobnet;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import androidx.appcompat.widget.SearchView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.saif.jobnet.Activities.AddEducationActivity;

import java.util.ArrayList;
import java.util.List;

public class BottomSheetFragment extends BottomSheetDialogFragment {

    private ArrayList<String> items;
    private CustomAdapter adapter;
    private Context context;

    public BottomSheetFragment(Context context, ArrayList<String> items) {
        this.context = context;
        this.items = items;
        System.out.println("bottom sheet fragment constructor called: course= "+items.size());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_layout, container, false);

        SearchView searchView = view.findViewById(R.id.searchView);
        ListView listView = view.findViewById(R.id.listView);

        // Set Bottom Sheet to Expand Half of the Screen
        getDialog().setOnShowListener(dialog -> {
            BottomSheetDialogFragment bottomSheet = (BottomSheetDialogFragment) BottomSheetFragment.this;
            View bottomSheetInternal = bottomSheet.getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheetInternal);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);  // By default, expanded
        });

        // Set Custom Adapter
        adapter = new CustomAdapter(requireContext(), items);
        listView.setAdapter(adapter);

        // Search Functionality (Case-Insensitive)
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.getFilter().filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return true;
            }
        });

        // Set Selected Course
        listView.setOnItemClickListener((parent, view1, position, id) -> {
            String selectedCourse = adapter.getItem(position);
            ((AddEducationActivity) context).setSelectedCourse(selectedCourse);
            dismiss();
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        View bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.setBackgroundResource(R.drawable.bottom_sheet_bg);
        }

        Window window = getDialog() != null ? getDialog().getWindow() : null;
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }


    }

    // Custom Adapter for Case-Insensitive Filter
    private static class CustomAdapter extends ArrayAdapter<String> implements Filterable {
        private final List<String> originalList;
        private List<String> filteredList;

        public CustomAdapter(Context context, List<String> items) {
            super(context, android.R.layout.simple_list_item_1, items);
            this.originalList = new ArrayList<>(items);
            this.filteredList = items;
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    List<String> filtered = new ArrayList<>();

                    if (constraint == null || constraint.length() == 0) {
                        filtered.addAll(originalList);
                    } else {
                        String filterPattern = constraint.toString().toLowerCase().trim();

                        for (String item : originalList) {
                            if (item.toLowerCase().contains(filterPattern)) {
                                filtered.add(item);
                            }
                        }
                    }

                    results.values = filtered;
                    results.count = filtered.size();
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    clear();
                    addAll((List<String>) results.values);
                    notifyDataSetChanged();
                }
            };
        }
    }
}
