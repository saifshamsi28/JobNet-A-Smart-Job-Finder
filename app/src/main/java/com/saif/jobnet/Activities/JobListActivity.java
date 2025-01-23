package com.saif.jobnet.Activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;

import com.saif.jobnet.Database.AppDatabase;
import com.saif.jobnet.Database.DatabaseClient;
import com.saif.jobnet.Models.Job;
import com.saif.jobnet.Database.JobDao;
import com.saif.jobnet.R;
import com.saif.jobnet.databinding.ActivityJobListBinding;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

public class JobListActivity extends AppCompatActivity {

    ActivityJobListBinding binding;
    private JobDao jobDao;
    private List<Job> jobs;
    private final Handler searchHandler = new Handler();
    ProgressDialog progressDialog ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityJobListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        EdgeToEdge.enable(this);
        getWindow().setStatusBarColor(ContextCompat.getColor(this,R.color.white));
        //to set the color of items of status bar black
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        AppDatabase appDatabase = DatabaseClient.getInstance(this).getAppDatabase();
        jobDao= appDatabase.jobDao();
        binding.searchedTitle.setVisibility(View.GONE);
        progressDialog = new ProgressDialog(this);

        Intent intent=getIntent();
        String source=intent.getStringExtra("source");
        String searchedTitle = intent.getStringExtra("search_title");
        assert source != null;
        if (!"history button".equals(source)) {
            binding.searchedTitle.setText("History");
        }else{
            if(getSupportActionBar()!=null)
                getSupportActionBar().setTitle("History");
        }

        // Fetching data from database
        new Thread(() -> {
//            if ("history button".equals(source)) {

                jobs=jobDao.getAllJobs();
//            } else {
//                //first fetch from database if not then request through retrofit
//                jobs = jobDao.getJobsByTitle(searchedTitle);
//            }
            runOnUiThread(() -> {
                binding.searchedTitle.setVisibility(View.VISIBLE);
                binding.jobTable.setVisibility(View.VISIBLE);
                Log.d("JobListActivity","Search title: "+searchedTitle);
                Log.d("JobListActivity", "Database: fetched from database " + jobs.size() + " jobs");
                populateTableWithJobs(jobs);
            });
        }).start();
    }

    // Function to dynamically add rows to the TableLayout
    private void populateTableWithJobs(List<Job> jobs) {
        TableLayout tableLayout = findViewById(R.id.job_table);
        Log.d("Database", "fetched from database " + jobs.size() + " jobs");

        // Clear previous rows (if any), while keeping the header row
        if (tableLayout.getChildCount() > 1) {
            tableLayout.removeViews(1, tableLayout.getChildCount() - 1);
        }

        int index = 1; // Starting serial number for jobs

        binding.jobTable.setVisibility(View.VISIBLE);
        for (Job job : jobs) {
            // Create a new row
            TableRow row = new TableRow(this);
            row.setLayoutParams(new TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

            // Serial Number
            TextView sno = new TextView(this);
            sno.setText(String.valueOf(index++));
            sno.setGravity(Gravity.CENTER);
            sno.setTextColor(Color.BLACK);
            sno.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1));
            sno.setPadding(8, 8, 8, 8);
//            sno.setBackground(ResourcesCompat.getDrawable(getResources(),R.drawable.table_cell_border,null));
            row.addView(sno);

            // to set the Job Title
            TextView jobTitle = new TextView(this);
            jobTitle.setText(job.getTitle());
            jobTitle.setLayoutParams(new TableRow.LayoutParams(500, TableRow.LayoutParams.WRAP_CONTENT)); // Weight = 3
            jobTitle.setPadding(8, 8, 8, 8);
            jobTitle.setTextColor(Color.BLACK);
            row.addView(jobTitle);

            // to set the Company
            TextView company = new TextView(this);
            company.setText(job.getCompany());
            company.setLayoutParams(new TableRow.LayoutParams(300, TableRow.LayoutParams.WRAP_CONTENT)); // Weight = 2
            company.setPadding(8, 8, 8, 8);
            company.setMaxLines(jobTitle.getMaxLines());
            company.setTextColor(Color.BLACK);
            row.addView(company);

            // to set the Location
            TextView location = new TextView(this);
            location.setText(job.getLocation());
            location.setLayoutParams(new TableRow.LayoutParams(300, TableRow.LayoutParams.WRAP_CONTENT)); // Weight = 2
            location.setPadding(8, 8, 8, 8);
            location.setMaxLines(jobTitle.getMaxLines());
            location.setEllipsize(TextUtils.TruncateAt.END);
            location.setTextColor(Color.BLACK);
            row.addView(location);

            // to set the Salary
            TextView salary = new TextView(this);
            salary.setText(job.getSalary());
            salary.setLayoutParams(new TableRow.LayoutParams(300, TableRow.LayoutParams.WRAP_CONTENT)); // Weight = 1
            salary.setPadding(8, 8, 8, 8);
            salary.setMaxLines(jobTitle.getMaxLines());
            salary.setEllipsize(TextUtils.TruncateAt.END);
            salary.setTextColor(Color.BLACK);
            row.addView(salary);

            //to set the ratings
            TextView rating = new TextView(this);
            rating.setText(String.valueOf(job.getRating()));
            rating.setGravity(Gravity.CENTER);
            rating.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT)); // Weight = 1
            rating.setPadding(8, 8, 8, 8);
            rating.setMaxLines(jobTitle.getMaxLines());
            rating.setEllipsize(TextUtils.TruncateAt.END);
            rating.setTextColor(Color.BLACK);
            row.addView(rating);

            //to set the reviews
            TextView review = new TextView(this);
            review.setText(String.valueOf(job.getReview()));
            review.setGravity(Gravity.CENTER);
            review.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT)); // Weight = 1
            review.setPadding(8, 8, 8, 8);
            review.setMaxLines(jobTitle.getMaxLines());
            review.setEllipsize(TextUtils.TruncateAt.END);
            review.setTextColor(Color.BLACK);
            row.addView(review);

            //to set the job post date
            TextView postDate = new TextView(this);
            postDate.setText(String.valueOf(job.getPostDate()));
            postDate.setGravity(Gravity.CENTER);
            postDate.setLayoutParams(new TableRow.LayoutParams(300, TableRow.LayoutParams.WRAP_CONTENT)); // Weight = 1
            postDate.setPadding(8, 8, 8, 8);
            postDate.setMaxLines(jobTitle.getMaxLines());
            postDate.setEllipsize(TextUtils.TruncateAt.END);
            postDate.setTextColor(Color.BLACK);
            row.addView(postDate);

            //to set the URL
            TextView url = new TextView(this);
            url.setText(job.getUrl());
            url.setLayoutParams(new TableRow.LayoutParams(800, TableRow.LayoutParams.WRAP_CONTENT)); // Weight = 2
            url.setPadding(8, 8, 8, 8);
            url.setTextColor(getResources().getColor(R.color.blue));
            url.setMaxLines(3);
            url.setClickable(true);
            url.setMovementMethod(LinkMovementMethod.getInstance()); // Enable clicking the link
            Linkify.addLinks(url, Linkify.WEB_URLS); // Automatically convert text to clickable link
            row.addView(url);

            //to set the shortDescription
            TextView description = new TextView(this);
            description.setText(job.getShortDescription());
            description.setLayoutParams(new TableRow.LayoutParams(800, TableRow.LayoutParams.WRAP_CONTENT));
            description.setPadding(8, 8, 8, 8);
            description.setMaxLines(3);
            description.setEllipsize(TextUtils.TruncateAt.END);
            description.setTextColor(Color.BLACK);
            row.addView(description);

            //to set the shortDescription
            TextView jobId = new TextView(this);
            jobId.setText(job.getJobId());
            jobId.setLayoutParams(new TableRow.LayoutParams(500, TableRow.LayoutParams.WRAP_CONTENT));
            jobId.setPadding(8, 8, 8, 8);
            jobId.setMaxLines(3);
            jobId.setEllipsize(TextUtils.TruncateAt.END);
            jobId.setTextColor(Color.BLACK);
            row.addView(jobId);

            // Adding OnClickListener to open Job Details activity
            row.setOnClickListener(v -> {
                Intent intent = new Intent(JobListActivity.this, JobDetailActivity.class);
                intent.putExtra("jobTitle", job.getTitle());
                intent.putExtra("company", job.getCompany());
                intent.putExtra("location", job.getLocation());
                intent.putExtra("salary", job.getSalary());
                intent.putExtra("description", job.getShortDescription());
                intent.putExtra("rating", job.getRating());
                intent.putExtra("reviews", job.getReview());
                intent.putExtra("url", job.getUrl());
                startActivity(intent);
            });

            // Add the row to the TableLayout
            tableLayout.addView(row);
            tableLayout.setStretchAllColumns(true);
        }
    }

    private void filterJobs(String query) {
        new Thread(() -> {
            List<Job> filteredJobs = jobDao.searchJobs(query);
            runOnUiThread(() -> populateTableWithJobs(filteredJobs));
        }).start();
    }


    public void exportTableToPDF(List<Job> jobs, String filePath) {
        try {
            progressDialog.setMessage("Exporting to PDF...");
            progressDialog.show();

            // Create a landscape-oriented document
            com.itextpdf.text.Document document = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();

            // Title
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("Job Listings", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Create table with column widths
            float[] columnWidths = {1, 3, 3, 3, 2, 2, 2, 2, 5, 5};
            PdfPTable table = new PdfPTable(columnWidths);
            table.setWidthPercentage(100);

            // Add table headers
            String[] headers = {"S.No", "Job Title", "Company", "Location", "Salary", "Rating", "Reviews", "Post Date", "URL", "Description"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                cell.setPadding(5);
                table.addCell(cell);
            }

            // Populate table rows
            int serialNumber = 1;
            for (Job job : jobs) {
                // to put Serial Number in center
                PdfPCell snoCell = new PdfPCell(new Phrase(String.valueOf(serialNumber++)));
                snoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(snoCell);
                table.addCell(new PdfPCell(new Phrase(job.getTitle())));
                table.addCell(new PdfPCell(new Phrase(job.getCompany())));
                table.addCell(new PdfPCell(new Phrase(job.getLocation())));
                table.addCell(new PdfPCell(new Phrase(job.getSalary())));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(job.getRating()))));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(job.getReview()))));
                table.addCell(new PdfPCell(new Phrase(job.getPostDate())));
                table.addCell(new PdfPCell(new Phrase(job.getUrl())));

                // Description with truncation
                String description = job.getShortDescription().length() > 50 ? job.getShortDescription().substring(0, 50) + "..." : job.getShortDescription();
                PdfPCell descCell = new PdfPCell(new Phrase(description));
                table.addCell(descCell);
            }

            // Add table to document
            document.add(table);
            document.close();

            System.out.println("PDF created at: " + filePath);
            progressDialog.dismiss();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                Toast.makeText(JobListActivity.this, "PDF created successfully", Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            progressDialog.dismiss();
//            Toast.makeText(this, "Failed to export PDF", Toast.LENGTH_SHORT).show();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(JobListActivity.this, "Failed to create pdf", Toast.LENGTH_LONG).show();
                }
            });
            e.printStackTrace();
        }
    }
//    private void exportToPDF() {
//        new Thread(() -> {
//            List<Job> jobs = jobDao.getAllJobs();
//
//            // Create a PDF document
//            PdfDocument pdfDocument = new PdfDocument();
//            Paint paint = new Paint();
//            int pageHeight = 1120;
//            int pageWidth = 792; // Standard PDF width
//            int y = 50; // Starting y position
//
//            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
//            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
//            Canvas canvas = page.getCanvas();
//
//            // Define column widths
//            int[] columnWidths = {50, 120, 120, 120, 100, 80, 100, 160,200}; // Adjust widths as needed
//            String[] headers = {"S.No", "Job Title", "Company", "Location", "Salary", "Rating", "Reviews", "Post Date", "URL"};
//
//            paint.setTextSize(14);
//            paint.setFakeBoldText(true);
//
//            // Draw headers
//            int x = 50; // Starting x position
//            for (int i = 0; i < headers.length; i++) {
//                canvas.drawText(headers[i], x, y, paint);
//                x += columnWidths[i];
//            }
//            y += 30; // Move down after headers
//            paint.setFakeBoldText(false);
//
//            int serialNumber = 1; // Serial number for jobs
//            for (Job job : jobs) {
//                int startY = y; // Row's starting Y position
//                x = 50; // Reset X position for each row
//
//                // Dynamic row height calculation
//                int maxRowHeight = 0;
//
//                // Draw Serial Number
//                canvas.drawText(String.valueOf(serialNumber), x, y, paint);
//                x += columnWidths[0];
//
//                // Job Title (Word wrapping)
//                List<String> titleLines = breakTextIntoLines(job.getTitle(), paint, columnWidths[1]);
//                for (String line : titleLines) {
//                    canvas.drawText(line, x, y, paint);
//                    y += 20; // Line spacing
//                }
//                maxRowHeight = Math.max(maxRowHeight, y - startY);
//                x += columnWidths[1];
//                y = startY; // Reset Y for alignment
//
//                // Company (Word wrapping)
//                List<String> companyLines = breakTextIntoLines(job.getCompany(), paint, columnWidths[2]);
//                for (String line : companyLines) {
//                    canvas.drawText(line, x, y, paint);
//                    y += 20;
//                }
//                maxRowHeight = Math.max(maxRowHeight, y - startY);
//                x += columnWidths[2];
//                y = startY;
//
//                // Location (Word wrapping)
//                List<String> locationLines = breakTextIntoLines(job.getLocation(), paint, columnWidths[3]);
//                for (String line : locationLines) {
//                    canvas.drawText(line, x, y, paint);
//                    y += 20;
//                }
//                maxRowHeight = Math.max(maxRowHeight, y - startY);
//                x += columnWidths[3];
//                y = startY;
//
//                // Salary
//                List<String> salaryLines = breakTextIntoLines(job.getSalary(), paint, columnWidths[4]);
//                for (String line : salaryLines) {
//                    canvas.drawText(line, x, y, paint);
//                    y += 20;
//                }
//                maxRowHeight = Math.max(maxRowHeight, y - startY);
//                x += columnWidths[4];
//                y = startY;
//
//                // Rating
//                canvas.drawText(String.valueOf(job.getRating()), x, y, paint);
//                x += columnWidths[5];
//
//                // Reviews
//                canvas.drawText(String.valueOf(job.getReview()), x, y, paint);
//                x += columnWidths[6];
//
//                // Post Date
//                canvas.drawText(job.getPostDate(), x, y, paint);
//                x += columnWidths[7];
//
//                // URL (truncate if too long)
//                String url = job.getUrl();
//                if (paint.measureText(url) > columnWidths[7]) {
//                    url = url.substring(0, 30) + "...";
//                }
//                canvas.drawText(url, x, y, paint);
//
//                y = startY + maxRowHeight + 20; // Move to the next row
//
//                // Create a new page if the current page is full
//                if (y > pageHeight - 100) {
//                    pdfDocument.finishPage(page);
//                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdfDocument.getPages().size() + 1).create();
//                    page = pdfDocument.startPage(pageInfo);
//                    canvas = page.getCanvas();
//                    y = 50;
//
//                    // Redraw headers on new page
//                    x = 50;
//                    for (int i = 0; i < headers.length; i++) {
//                        canvas.drawText(headers[i], x, y, paint);
//                        x += columnWidths[i];
//                    }
//                    y += 30; // Move down after headers
//                }
//
//                serialNumber++;
//            }
//
//            pdfDocument.finishPage(page);
//
//            // Save the PDF file
//            try {
//                File pdfFile = new File(getExternalFilesDir(null), "JobData.pdf");
//                pdfDocument.writeTo(new FileOutputStream(pdfFile));
//                runOnUiThread(() -> Toast.makeText(this, "PDF exported: " + pdfFile.getAbsolutePath(), Toast.LENGTH_LONG).show());
//            } catch (Exception e) {
//                e.printStackTrace();
//                runOnUiThread(() -> Toast.makeText(this, "Error exporting PDF", Toast.LENGTH_SHORT).show());
//            } finally {
//                pdfDocument.close();
//            }
//        }).start();
//    }
//
//
//    // Break text into lines, keeping whole words
//    private List<String> breakTextIntoLines(String text, Paint paint, int maxWidth) {
//        List<String> lines = new ArrayList<>();
//        String[] words = text.split(" ");
//        StringBuilder line = new StringBuilder();
//
//        for (String word : words) {
//            if (paint.measureText(line.toString() + " " + word) > maxWidth) {
//                lines.add(line.toString());
//                line = new StringBuilder(word);
//            } else {
//                if (line.length() > 0) {
//                    line.append(" ");
//                }
//                line.append(word);
//            }
//        }
//
//        if (line.length() > 0) {
//            lines.add(line.toString());
//        }
//
//        return lines;
//    }

    private void exportToExcel() {
        new Thread(() -> {
            List<Job> jobs = jobDao.getAllJobs();

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Jobs");

            // Add header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Job ID");
            headerRow.createCell(1).setCellValue("Title");
            headerRow.createCell(2).setCellValue("Company");
            headerRow.createCell(3).setCellValue("Location");
            headerRow.createCell(4).setCellValue("Salary");

            // Add job data
            int rowIndex = 1;
            for (Job job : jobs) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(job.getJobId());
                row.createCell(1).setCellValue(job.getTitle());
                row.createCell(2).setCellValue(job.getCompany());
                row.createCell(3).setCellValue(job.getLocation());
                row.createCell(4).setCellValue(job.getSalary());
            }

            // Save the Excel file
            try {
                File excelFile = new File(getExternalFilesDir(null), "JobData.xlsx");
                FileOutputStream fileOut = new FileOutputStream(excelFile);
                workbook.write(fileOut);
                fileOut.close();
                workbook.close();
                runOnUiThread(() -> Toast.makeText(this, "Excel exported: " + excelFile.getAbsolutePath(), Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error exporting Excel", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.history_screen_menu, menu);

        for (int i = 0; i < menu.size(); i++) {
            Log.d("MenuItems", "Item: " + menu.getItem(i).getTitle());
        }
        MenuItem searchItem = menu.findItem(R.id.search_bar);
        MenuItem moreMenu = menu.findItem(R.id.vertical_more);

        //to set more_menu which is containing share,delete and close
        if (searchItem != null) {
            SearchView searchView = (SearchView) searchItem.getActionView();
            searchView.setQueryHint("Search Jobs by Title or JobId");
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    binding.searchedTitle.setText("Searched: " + query);
                    binding.searchedTitle.setVisibility(View.VISIBLE);
                    filterJobs(query);
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    searchHandler.removeCallbacksAndMessages(null); // Cancel any pending search tasks
                    searchHandler.postDelayed(() -> {
                        binding.searchedTitle.setVisibility(View.GONE);
                        filterJobs(newText); // Trigger search after 500ms delay
                    }, 500); // Delay of 500ms
                    return true;
                }
            });
        } else {
            Log.e("JobListActivity", "Search item is null");
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId()==R.id.delete) {
            //populate a a confirmation dialogue to delete
            new AlertDialog.Builder(this)
                    .setTitle("Delete All Jobs")
                    .setMessage("Are you sure you want to delete all jobs?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // Delete all jobs
                        new Thread(() -> jobDao.deleteAllJobs()).start();
                        Toast.makeText(this, "All jobs deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("No", null)
                    .show();
            return true;
        } else if (item.getItemId() == R.id.share) {
            // Show dialog to select format
            new AlertDialog.Builder(this)
                    .setTitle("Export Data")
                    .setMessage("In which format would you like to export the data?")
                    .setPositiveButton("PDF", (dialog, which) -> {
                        new Thread(() -> {
                        exportTableToPDF(jobs, getExternalFilesDir(null) + "/JobData.pdf");
                        }).start();
                    })
                    .setNegativeButton("Excel", (dialog, which) -> exportToExcel())
                    .show();
            return true;
        }else
            return super.onOptionsItemSelected(item);
    }
}