package com.jobnet.app.ui.search;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.jobnet.app.R;
import com.jobnet.app.data.model.Job;
import com.jobnet.app.data.model.JobCategory;
import com.jobnet.app.data.repository.JobNetRepository;
import com.jobnet.app.ui.home.JobsAdapter;
import com.jobnet.app.util.SkeletonShimmerHelper;
import com.jobnet.app.util.SalaryUtils;
import com.jobnet.app.util.SampleData;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class SearchFragment extends Fragment {

    private RecyclerView rvResults;
    private EditText etSearch;
    private TextView tvResultsCount;
    private TextView tvSort;
    private TextView tvFilterCountBadge;
    private TextView chipAll, chipFullTime, chipPartTime, chipRemote, chipInternship;
    private View btnFilter;
    private JobsAdapter adapter;
    private final List<Job> allJobs = new ArrayList<>();
    private String activeFilter = "All Jobs";
    private SortOption activeSort = SortOption.RELEVANCE;
    private JobNetRepository repository;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;
    private Dialog skeletonDialog;
    private final List<ObjectAnimator> skeletonAnimators = new ArrayList<>();
    private boolean skipNextResumeRefresh = true;
    private int homeLoadRequestVersion = 0;
    private int searchRequestVersion = 0;
    private final AdvancedSearchFilter advancedFilter = new AdvancedSearchFilter();
    private static final Pattern RELATIVE_PATTERN = Pattern.compile("(\\d+)\\s*([mhd])");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvResults      = view.findViewById(R.id.rv_search_results);
        etSearch       = view.findViewById(R.id.et_search);
        tvResultsCount = view.findViewById(R.id.tv_results_count);
        tvSort         = view.findViewById(R.id.btn_sort);
        tvFilterCountBadge = view.findViewById(R.id.tv_filter_count_badge);
        chipAll        = view.findViewById(R.id.chip_all);
        chipFullTime   = view.findViewById(R.id.chip_full_time);
        chipPartTime   = view.findViewById(R.id.chip_part_time);
        chipRemote     = view.findViewById(R.id.chip_remote);
        chipInternship = view.findViewById(R.id.chip_internship);
        btnFilter      = view.findViewById(R.id.btn_filter);

        repository = JobNetRepository.getInstance(requireContext());

        adapter = new JobsAdapter(new ArrayList<>(), new JobsAdapter.OnJobClickListener() {
            @Override
            public void onJobClick(Job job, int position) {
                Bundle args = new Bundle();
                args.putSerializable("job", job);
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_search_to_jobDetails, args);
            }
            @Override
            public void onBookmarkClick(Job job, int position) {
                job.setSaved(!job.isSaved());
                adapter.notifyItemChanged(position);
            }
        });

        rvResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvResults.setAdapter(adapter);
        rvResults.setHasFixedSize(true);

        setupSearch();
        setupChips();
        btnFilter.setOnClickListener(v -> showAdvancedFilterSheet());
        if (tvSort != null) {
            tvSort.setOnClickListener(v -> showSortOptions());
        }
        updateFilterButtonState();
        updateSortLabel();
        showSearchSkeleton(true);
        loadInitialJobs();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (skipNextResumeRefresh) {
            skipNextResumeRefresh = false;
            return;
        }
        showSearchSkeleton(true);
        loadInitialJobs();
    }

    private void loadInitialJobs() {
        final int requestVersion = ++homeLoadRequestVersion;
        repository.loadHomeData(new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(JobNetRepository.HomeData data) {
                if (!isActiveHomeLoad(requestVersion)) {
                    return;
                }

                allJobs.clear();
                allJobs.addAll(mergeDistinctJobs(
                        data == null ? null : data.featured,
                        data == null ? null : data.recommended
                ));
                if (allJobs.isEmpty()) {
                    allJobs.addAll(SampleData.getAllJobs());
                }
                List<Job> filtered = applySorting(applyAdvancedFilters(localFilter(allJobs, "", activeFilter)));
                adapter.updateData(filtered);
                updateResultsCount(filtered.size());
                showSearchSkeleton(false);
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isActiveHomeLoad(requestVersion)) {
                    return;
                }
                allJobs.clear();
                allJobs.addAll(SampleData.getAllJobs());
                List<Job> filtered = applySorting(applyAdvancedFilters(localFilter(allJobs, "", activeFilter)));
                adapter.updateData(filtered);
                updateResultsCount(filtered.size());
                showSearchSkeleton(false);
            }
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                scheduleSearch();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupChips() {
        View.OnClickListener chipClick = v -> {
            TextView clicked = (TextView) v;
            activeFilter = clicked.getText().toString();
            updateChipUI();
            scheduleSearch();
        };
        chipAll.setOnClickListener(chipClick);
        chipFullTime.setOnClickListener(chipClick);
        chipPartTime.setOnClickListener(chipClick);
        chipRemote.setOnClickListener(chipClick);
        chipInternship.setOnClickListener(chipClick);
    }

    private void updateChipUI() {
        TextView[] chips = {chipAll, chipFullTime, chipPartTime, chipRemote, chipInternship};
        for (TextView chip : chips) {
            boolean selected = chip.getText().toString().equals(activeFilter);
            chip.setBackgroundResource(selected ?
                    R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            chip.setTextColor(requireContext().getColor(
                    selected ? R.color.white : R.color.text_secondary));
        }
    }

    private void filterJobs(String query) {
        String trimmed = query == null ? "" : query.trim();

        if (trimmed.isEmpty()) {
            searchRequestVersion++;
            List<Job> filtered = applySorting(applyAdvancedFilters(localFilter(allJobs.isEmpty() ? SampleData.getAllJobs() : allJobs, "", activeFilter)));
            adapter.updateData(filtered);
            updateResultsCount(filtered.size());
            return;
        }

        final int requestVersion = ++searchRequestVersion;
        repository.searchJobs(trimmed, activeFilter, new JobNetRepository.DataCallback<>() {
            @Override
            public void onSuccess(List<Job> data) {
                if (!isActiveSearchRequest(requestVersion)) {
                    return;
                }
                List<Job> jobs = dedupeJobs(data == null ? Collections.emptyList() : data);
                List<Job> filtered = applySorting(applyAdvancedFilters(jobs));
                adapter.updateData(filtered);
                updateResultsCount(filtered.size());
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isActiveSearchRequest(requestVersion)) {
                    return;
                }
                List<Job> filtered = applySorting(applyAdvancedFilters(localFilter(allJobs.isEmpty() ? SampleData.getAllJobs() : allJobs, trimmed, activeFilter)));
                adapter.updateData(filtered);
                updateResultsCount(filtered.size());
            }
        });
    }

    private List<Job> mergeDistinctJobs(List<Job> featured, List<Job> recommended) {
        LinkedHashMap<String, Job> unique = new LinkedHashMap<>();
        appendDistinctJobs(unique, featured);
        appendDistinctJobs(unique, recommended);
        return new ArrayList<>(unique.values());
    }

    private List<Job> dedupeJobs(List<Job> source) {
        LinkedHashMap<String, Job> unique = new LinkedHashMap<>();
        appendDistinctJobs(unique, source);
        return new ArrayList<>(unique.values());
    }

    private void appendDistinctJobs(Map<String, Job> unique, List<Job> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        for (Job job : source) {
            if (job == null) {
                continue;
            }
            String key = stableJobKey(job);
            if (!unique.containsKey(key)) {
                unique.put(key, job);
            }
        }
    }

    private String stableJobKey(Job job) {
        String id = safe(job.getId()).trim();
        if (!id.isEmpty()) {
            return "id:" + id;
        }
        return "meta:"
                + safe(job.getTitle()).trim().toLowerCase(Locale.ROOT) + "|"
                + safe(job.getCompany()).trim().toLowerCase(Locale.ROOT) + "|"
                + safe(job.getLocation()).trim().toLowerCase(Locale.ROOT) + "|"
                + safe(job.getType()).trim().toLowerCase(Locale.ROOT);
    }

    private boolean isActiveHomeLoad(int requestVersion) {
        return isAdded() && requestVersion == homeLoadRequestVersion;
    }

    private boolean isActiveSearchRequest(int requestVersion) {
        return isAdded() && requestVersion == searchRequestVersion;
    }

    private void scheduleSearch() {
        if (pendingSearch != null) {
            searchHandler.removeCallbacks(pendingSearch);
        }
        pendingSearch = () -> filterJobs(etSearch.getText().toString());
        searchHandler.postDelayed(pendingSearch, 300);
    }

    private List<Job> localFilter(List<Job> source, String query, String filter) {
        List<Job> filtered = new ArrayList<>();
        String lowerQuery = query == null ? "" : query.toLowerCase(Locale.getDefault());
        String resolvedFilter = filter == null ? "All Jobs" : filter;

        for (Job job : source) {
            String title = safe(job.getTitle()).toLowerCase(Locale.ROOT);
            String company = safe(job.getCompany()).toLowerCase(Locale.ROOT);
            String location = safe(job.getLocation()).toLowerCase(Locale.ROOT);

            boolean matchesQuery = lowerQuery.isEmpty()
                    || title.contains(lowerQuery)
                    || company.contains(lowerQuery)
                    || location.contains(lowerQuery);

            boolean matchesFilter = resolvedFilter.equals("All Jobs")
                    || safe(job.getType()).equalsIgnoreCase(resolvedFilter)
                    || (resolvedFilter.equalsIgnoreCase("Remote")
                    && safe(job.getWorkMode()).equalsIgnoreCase("Remote"));

            if (matchesQuery && matchesFilter) filtered.add(job);
        }
        return filtered;
    }

    private List<Job> applyAdvancedFilters(List<Job> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }

        List<Job> filtered = new ArrayList<>();
        for (Job job : source) {
            if (job == null) {
                continue;
            }

            if (!advancedFilter.company.isEmpty() && !safe(job.getCompany()).toLowerCase(Locale.ROOT)
                    .contains(advancedFilter.company.toLowerCase(Locale.ROOT))) {
                continue;
            }

            if (!advancedFilter.location.isEmpty() && !safe(job.getLocation()).toLowerCase(Locale.ROOT)
                    .contains(advancedFilter.location.toLowerCase(Locale.ROOT))) {
                continue;
            }

            if (!advancedFilter.category.isEmpty() && !safe(job.getCategory()).equalsIgnoreCase(advancedFilter.category)) {
                continue;
            }

            if (!advancedFilter.workMode.isEmpty() && !safe(job.getWorkMode()).equalsIgnoreCase(advancedFilter.workMode)) {
                continue;
            }

            if (advancedFilter.minSalary > 0 && SalaryUtils.approximateLpa(job.getSalary()) < advancedFilter.minSalary) {
                continue;
            }

            filtered.add(job);
        }

        return filtered;
    }

    private List<Job> applySorting(List<Job> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<Job> sorted = new ArrayList<>(source);
        switch (activeSort) {
            case NEWEST:
                sorted.sort((left, right) -> Long.compare(resolveSortTimestamp(right), resolveSortTimestamp(left)));
                break;
            case SALARY_HIGH_TO_LOW:
                sorted.sort((left, right) -> Integer.compare(SalaryUtils.approximateLpa(right.getSalary()), SalaryUtils.approximateLpa(left.getSalary())));
                break;
            case SALARY_LOW_TO_HIGH:
                sorted.sort((left, right) -> {
                    int leftSalary = SalaryUtils.approximateLpa(left.getSalary());
                    int rightSalary = SalaryUtils.approximateLpa(right.getSalary());
                    if (leftSalary == 0 && rightSalary == 0) {
                        return 0;
                    }
                    if (leftSalary == 0) {
                        return 1;
                    }
                    if (rightSalary == 0) {
                        return -1;
                    }
                    return Integer.compare(leftSalary, rightSalary);
                });
                break;
            case COMPANY_A_TO_Z:
                sorted.sort((left, right) -> safe(left.getCompany()).compareToIgnoreCase(safe(right.getCompany())));
                break;
            case RELEVANCE:
            default:
                break;
        }
        return sorted;
    }

    private long resolveSortTimestamp(Job job) {
        if (job == null) {
            return 0L;
        }
        long iso = parseIsoTimestamp(job.getDateTime());
        if (iso > 0L) {
            return iso;
        }
        iso = parseIsoTimestamp(job.getUpdatedAt());
        if (iso > 0L) {
            return iso;
        }

        String posted = safe(job.getPostedDate()).toLowerCase(Locale.ROOT);
        if (posted.contains("today") || posted.contains("just now")) {
            return System.currentTimeMillis();
        }

        Matcher matcher = RELATIVE_PATTERN.matcher(posted);
        if (!matcher.find()) {
            return 0L;
        }
        int value = parseIntSafe(matcher.group(1));
        String unit = matcher.group(2);
        long delta;
        if ("m".equals(unit)) {
            delta = value * 60_000L;
        } else if ("h".equals(unit)) {
            delta = value * 3_600_000L;
        } else {
            delta = value * 86_400_000L;
        }
        return System.currentTimeMillis() - delta;
    }

    private long parseIsoTimestamp(String value) {
        String raw = safe(value);
        if (raw.isEmpty()) {
            return 0L;
        }
        try {
            return OffsetDateTime.parse(raw).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(raw).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {
        }
        return 0L;
    }

    private void showSortOptions() {
        if (!isAdded()) {
            return;
        }
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View root = LayoutInflater.from(requireContext()).inflate(R.layout.sheet_search_sort, null, false);
        dialog.setContentView(root);

        updateSortSelectionUI(root);

        bindSortOption(root, dialog, R.id.option_sort_relevance, SortOption.RELEVANCE);
        bindSortOption(root, dialog, R.id.option_sort_newest, SortOption.NEWEST);
        bindSortOption(root, dialog, R.id.option_sort_salary_high, SortOption.SALARY_HIGH_TO_LOW);
        bindSortOption(root, dialog, R.id.option_sort_salary_low, SortOption.SALARY_LOW_TO_HIGH);
        bindSortOption(root, dialog, R.id.option_sort_company, SortOption.COMPANY_A_TO_Z);

        dialog.show();
    }

    private void bindSortOption(View root, BottomSheetDialog dialog, int viewId, SortOption option) {
        View optionView = root.findViewById(viewId);
        if (optionView == null) {
            return;
        }
        optionView.setOnClickListener(v -> {
            activeSort = option;
            updateSortLabel();
            scheduleSearch();
            dialog.dismiss();
        });
    }

    private void updateSortSelectionUI(View root) {
        updateSortRowState(root, R.id.option_sort_relevance, R.id.iv_sort_selected_relevance, activeSort == SortOption.RELEVANCE);
        updateSortRowState(root, R.id.option_sort_newest, R.id.iv_sort_selected_newest, activeSort == SortOption.NEWEST);
        updateSortRowState(root, R.id.option_sort_salary_high, R.id.iv_sort_selected_salary_high, activeSort == SortOption.SALARY_HIGH_TO_LOW);
        updateSortRowState(root, R.id.option_sort_salary_low, R.id.iv_sort_selected_salary_low, activeSort == SortOption.SALARY_LOW_TO_HIGH);
        updateSortRowState(root, R.id.option_sort_company, R.id.iv_sort_selected_company, activeSort == SortOption.COMPANY_A_TO_Z);
    }

    private void updateSortRowState(View root, int rowId, int indicatorId, boolean selected) {
        View row = root.findViewById(rowId);
        View indicator = root.findViewById(indicatorId);
        if (row != null) {
            row.setBackgroundResource(selected ? R.drawable.bg_sort_option_selected : R.drawable.bg_sort_option);
        }
        if (indicator != null) {
            indicator.setVisibility(selected ? View.VISIBLE : View.GONE);
        }
    }

    private void updateSortLabel() {
        if (tvSort == null) {
            return;
        }
        if (activeSort == SortOption.RELEVANCE) {
            tvSort.setText("Sort by");
            return;
        }
        tvSort.setText(activeSort.shortLabel);
    }

    private void showAdvancedFilterSheet() {
        if (!isAdded()) {
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(R.layout.sheet_search_filters);

        TextInputEditText inputCompany = dialog.findViewById(R.id.input_filter_company);
        TextInputEditText inputLocation = dialog.findViewById(R.id.input_filter_location);
        TextInputEditText inputMinSalary = dialog.findViewById(R.id.input_filter_min_salary);
        MaterialAutoCompleteTextView inputWorkMode = dialog.findViewById(R.id.input_filter_work_mode);
        MaterialAutoCompleteTextView inputCategory = dialog.findViewById(R.id.input_filter_category);
        View btnReset = dialog.findViewById(R.id.btn_reset_filters);
        View btnApply = dialog.findViewById(R.id.btn_apply_filters);

        if (inputCompany != null) {
            inputCompany.setText(advancedFilter.company);
        }
        if (inputLocation != null) {
            inputLocation.setText(advancedFilter.location);
        }
        if (inputMinSalary != null) {
            inputMinSalary.setText(advancedFilter.minSalary > 0 ? String.valueOf(advancedFilter.minSalary) : "");
        }

        String[] workModes = new String[]{"Any", "On-site", "Hybrid", "Remote"};
        List<String> categoryOptions = new ArrayList<>();
        categoryOptions.add("Any");
        for (JobCategory category : SampleData.getCategories()) {
            if (category != null && category.getName() != null && !category.getName().isBlank()) {
                categoryOptions.add(category.getName());
            }
        }

        if (inputWorkMode != null) {
            inputWorkMode.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, workModes));
            inputWorkMode.setText(advancedFilter.workMode.isEmpty() ? "Any" : advancedFilter.workMode, false);
        }
        if (inputCategory != null) {
            inputCategory.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, categoryOptions));
            inputCategory.setText(advancedFilter.category.isEmpty() ? "Any" : advancedFilter.category, false);
        }

        if (btnReset != null) {
            btnReset.setOnClickListener(v -> {
                advancedFilter.clear();
                updateFilterButtonState();
                scheduleSearch();
                dialog.dismiss();
            });
        }

        if (btnApply != null) {
            btnApply.setOnClickListener(v -> {
                advancedFilter.company = inputCompany == null ? "" : safe(inputCompany.getText() == null ? "" : inputCompany.getText().toString()).trim();
                advancedFilter.location = inputLocation == null ? "" : safe(inputLocation.getText() == null ? "" : inputLocation.getText().toString()).trim();
                advancedFilter.minSalary = parseIntSafe(inputMinSalary == null || inputMinSalary.getText() == null
                        ? ""
                        : inputMinSalary.getText().toString());

                String selectedWorkMode = inputWorkMode == null || inputWorkMode.getText() == null
                        ? "Any"
                        : inputWorkMode.getText().toString().trim();
                advancedFilter.workMode = "Any".equalsIgnoreCase(selectedWorkMode) ? "" : selectedWorkMode;

                String selectedCategory = inputCategory == null || inputCategory.getText() == null
                        ? "Any"
                        : inputCategory.getText().toString().trim();
                advancedFilter.category = "Any".equalsIgnoreCase(selectedCategory) ? "" : selectedCategory;

                updateFilterButtonState();
                scheduleSearch();
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private void updateFilterButtonState() {
        if (btnFilter == null) {
            return;
        }
        btnFilter.setAlpha(advancedFilter.hasValues() ? 1f : 0.82f);
        if (tvFilterCountBadge == null) {
            return;
        }
        int count = advancedFilter.activeCount();
        if (count > 0) {
            tvFilterCountBadge.setText(count > 99 ? "99+" : String.valueOf(count));
            tvFilterCountBadge.setVisibility(View.VISIBLE);
            tvFilterCountBadge.bringToFront();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                tvFilterCountBadge.setTranslationZ(12f);
            }
        } else {
            tvFilterCountBadge.setVisibility(View.GONE);
        }
    }

    private int parseIntSafe(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static class AdvancedSearchFilter {
        String company = "";
        String location = "";
        String category = "";
        String workMode = "";
        int minSalary = 0;

        void clear() {
            company = "";
            location = "";
            category = "";
            workMode = "";
            minSalary = 0;
        }

        boolean hasValues() {
            return !company.isEmpty()
                    || !location.isEmpty()
                    || !category.isEmpty()
                    || !workMode.isEmpty()
                    || minSalary > 0;
        }

        int activeCount() {
            int count = 0;
            if (!company.isEmpty()) count++;
            if (!location.isEmpty()) count++;
            if (!category.isEmpty()) count++;
            if (!workMode.isEmpty()) count++;
            if (minSalary > 0) count++;
            return count;
        }
    }

    private enum SortOption {
        RELEVANCE("Relevance", "Sort by"),
        NEWEST("Newest", "Newest"),
        SALARY_HIGH_TO_LOW("Salary: High to Low", "Salary High"),
        SALARY_LOW_TO_HIGH("Salary: Low to High", "Salary Low"),
        COMPANY_A_TO_Z("Company: A to Z", "Company A-Z");

        final String label;
        final String shortLabel;

        SortOption(String label, String shortLabel) {
            this.label = label;
            this.shortLabel = shortLabel;
        }
    }

    private void updateResultsCount(int count) {
        tvResultsCount.setText(count + " Jobs Found");
    }

    private void showSearchSkeleton(boolean show) {
        if (!isAdded()) {
            return;
        }
        if (!show) {
            SkeletonShimmerHelper.stop(skeletonAnimators);
            if (skeletonDialog != null && skeletonDialog.isShowing()) {
                skeletonDialog.dismiss();
            }
            return;
        }

        if (skeletonDialog == null) {
            skeletonDialog = new Dialog(requireContext());
            skeletonDialog.setContentView(R.layout.dialog_search_skeleton);
            skeletonDialog.setCancelable(false);
            if (skeletonDialog.getWindow() != null) {
                skeletonDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                skeletonDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            }
        }
        if (!skeletonDialog.isShowing()) {
            skeletonDialog.show();
            View root = skeletonDialog.findViewById(R.id.skeleton_root_search);
            SkeletonShimmerHelper.start(root, skeletonAnimators);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pendingSearch != null) {
            searchHandler.removeCallbacks(pendingSearch);
        }
        homeLoadRequestVersion++;
        searchRequestVersion++;
        showSearchSkeleton(false);
    }
}
