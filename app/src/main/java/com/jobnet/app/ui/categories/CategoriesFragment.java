package com.jobnet.app.ui.categories;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.jobnet.app.R;
import com.jobnet.app.data.model.JobCategory;
import com.jobnet.app.util.SampleData;

import java.util.List;

public class CategoriesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_categories, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        applyInsets(view);

        view.findViewById(R.id.btn_back_categories).setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());

        LinearLayout container = view.findViewById(R.id.container_categories_list);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        List<JobCategory> categories = SampleData.getCategories();

        for (int i = 0; i < categories.size(); i++) {
            JobCategory category = categories.get(i);
            View row = inflater.inflate(R.layout.item_category_row, container, false);
            ImageView icon = row.findViewById(R.id.iv_row_category_icon);
            TextView title = row.findViewById(R.id.tv_row_category_name);
            TextView count = row.findViewById(R.id.tv_row_category_count);

            icon.setImageResource(category.getIconRes());
            title.setText(category.getName());
            count.setText(category.getJobCount() + " openings");

            row.setOnClickListener(v -> openCategory(category.getName()));
            container.addView(row);

            if (i < categories.size() - 1) {
                View divider = new View(requireContext());
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(1)
                ));
                divider.setBackgroundResource(R.color.border);
                container.addView(divider);
            }
        }
    }

    private void openCategory(String categoryName) {
        Bundle args = new Bundle();
        args.putString("categoryName", categoryName);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_categoriesFragment_to_categoryJobsFragment, args);
    }

    private void applyInsets(View root) {
        View toolbar = root.findViewById(R.id.categories_toolbar);
        final int toolbarTop = toolbar.getPaddingTop();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            toolbar.setPadding(toolbar.getPaddingLeft(), toolbarTop + bars.top, toolbar.getPaddingRight(), toolbar.getPaddingBottom());
            return insets;
        });
    }

    private int dp(int value) {
        return Math.round(value * requireContext().getResources().getDisplayMetrics().density);
    }
}
