package com.example.emergencypreparednessmanager.UI.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emergencypreparednessmanager.R;
import com.example.emergencypreparednessmanager.UI.adapters.ReportRowAdapter;
import com.example.emergencypreparednessmanager.database.ItemSearchRow;
import com.example.emergencypreparednessmanager.database.Repository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textview.MaterialTextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportsActivity extends AppCompatActivity {

    private static final String PREFS = "epm_prefs";
    private static final String KEY_HOUSEHOLD_SIZE = "household_size";
    private static final int DEFAULT_HOUSEHOLD_SIZE = 1;

    private MaterialToolbar toolbar;

    private MaterialTextView tvGenerated;
    private MaterialTextView tvHousehold;
    private MaterialTextView tvSummaryBody;
    private MaterialTextView tvReadinessBody;

    private RecyclerView rvExpiration;
    private RecyclerView rvInventory;

    private View expirationSection;

    private MaterialTextView tvSourcesBody;
    private MaterialTextView tvCommunityBody;

    private List<ItemSearchRow> lastAllRows = new ArrayList<>();
    private List<ItemSearchRow> lastExpirationRows = new ArrayList<>();
    private ReportComputed lastComputed;
    private int lastHouseholdSize = DEFAULT_HOUSEHOLD_SIZE;
    private String lastGeneratedTimestamp = "";

    private Repository repository;

    private final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        repository = new Repository(getApplication());

        bindViews();
        setupToolbar();
        setupLists();

        loadReport();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbar);

        tvGenerated = findViewById(R.id.tvGenerated);
        tvHousehold = findViewById(R.id.tvHousehold);
        tvSummaryBody = findViewById(R.id.tvSummaryBody);
        tvReadinessBody = findViewById(R.id.tvReadinessBody);

        expirationSection = findViewById(R.id.expirationSection);
        rvExpiration = findViewById(R.id.rvExpiration);
        rvInventory = findViewById(R.id.rvInventory);

        tvSourcesBody = findViewById(R.id.tvSourcesBody);
        tvCommunityBody = findViewById(R.id.tvCommunityBody);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupLists() {
        rvExpiration.setLayoutManager(new LinearLayoutManager(this));
        rvInventory.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_reports, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_export_csv) {
            exportCsv();
            return true;
        } else if (id == R.id.action_copy_summary) {
            copySummaryToClipboard();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void loadReport() {
        lastGeneratedTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());
        tvGenerated.setText(getString(R.string.generated_format, lastGeneratedTimestamp));

        lastHouseholdSize = getHouseholdSize();
        tvHousehold.setText(getString(R.string.household_size_format, lastHouseholdSize));
        tvSourcesBody.setMovementMethod(LinkMovementMethod.getInstance());
        tvCommunityBody.setMovementMethod(LinkMovementMethod.getInstance());

        // Sources Links Setup
        SpannableStringBuilder sources = new SpannableStringBuilder();

        appendLink(sources, "Ready.gov – Emergency Kit",
                "https://www.ready.gov/kit");
        sources.append("\n");

        appendLink(sources, "Ready.gov – Water Storage",
                "https://www.ready.gov/water");
        sources.append("\n");

        appendLink(sources, "CDC – Emergency Water Supply",
                "https://www.cdc.gov/water-emergency/about/how-to-create-and-store-an-emergency-water-supply.html");
        sources.append("\n");

        appendLink(sources, "UGA - Long Term Food Storage",
                "https://www.fcs.uga.edu/extension/preparing-an-emergency-food-supply-long-term-food-storage");

        tvSourcesBody.setText(sources);
        tvSourcesBody.setLinkTextColor(
                com.google.android.material.color.MaterialColors.getColor(
                        tvSourcesBody,
                        androidx.appcompat.R.attr.colorPrimary
                )
        );

        // Community Resources Links Setup
        SpannableStringBuilder community = new SpannableStringBuilder();

        appendLink(community, "TheUrbanPrepper",
                "https://www.youtube.com/@TheUrbanPrepper");
        community.append("\n");

        appendLink(community, "PeakSurvival",
                "https://www.youtube.com/@PeakSurvival"
        );
        community.append("\n");

        appendLink(community, "Garand Thumb",
                "https://www.youtube.com/@GarandThumb"
        );

        tvCommunityBody.setText(community);
        tvCommunityBody.setLinkTextColor(
                com.google.android.material.color.MaterialColors.getColor(
                        tvCommunityBody,
                        androidx.appcompat.R.attr.colorPrimary
                )
        );


        repository.getAllKits(kits -> {
            int totalKits = (kits == null) ? 0 : kits.size();

            repository.getInventoryReportRows(rows -> {
                List<ItemSearchRow> allRows = (rows == null) ? new ArrayList<>() : rows;

                ReportComputed computed = computeReport(allRows, totalKits);
                lastComputed = computed;
                lastAllRows = allRows;
                lastExpirationRows = computed.expirationRows;

                tvSummaryBody.setText(buildSummaryText(computed));
                tvReadinessBody.setText(buildReadinessText(lastHouseholdSize, allRows));

                // Expiration section visible only if it has rows
                if (computed.expirationRows.isEmpty()) {
                    expirationSection.setVisibility(View.GONE);
                } else {
                    expirationSection.setVisibility(View.VISIBLE);
                }

                rvExpiration.setAdapter(new ReportRowAdapter(computed.expirationRows, true));
                rvInventory.setAdapter(new ReportRowAdapter(allRows, false));
            });
        });
    }

    private void appendLink(SpannableStringBuilder sb, String text, String url) {
        int start = sb.length();
        sb.append(text);
        sb.setSpan(
                new URLSpan(url),
                start,
                start + text.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
    }

    private ReportComputed computeReport(List<ItemSearchRow> rows, int totalKits) {
        Date today = startOfDay(new Date());

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(today);
        calendar.add(Calendar.DAY_OF_YEAR, 30);
        Date in30 = calendar.getTime();

        int totalTrackedItems = rows.size();
        int expiringIn30 = 0;
        int expired = 0;

        List<ItemSearchRow> expirationList = new ArrayList<>();

        for (ItemSearchRow r : rows) {
            Date expDate = parseExpiration(r.getExpirationDate());
            if (expDate == null) continue;

            if (expDate.before(today)) {
                expired++;
                expirationList.add(r);
            } else if (!expDate.after(in30)) {
                expiringIn30++;
                expirationList.add(r);
            }
        }

        expirationList.sort(Comparator.comparing(a -> safeDate(parseExpiration(a.getExpirationDate()))));

        ReportComputed c = new ReportComputed();
        c.totalKits = totalKits;
        c.totalTrackedItems = totalTrackedItems;
        c.itemsExpiringIn30Days = expiringIn30;
        c.expiredItems = expired;
        c.expirationRows = expirationList;

        return c;
    }

    private String buildSummaryText(ReportComputed c) {
        return "Total Kits: " + c.totalKits + "\n" +
                "Total Tracked Items: " + c.totalTrackedItems + "\n" +
                "Items Expiring in 30 Days: " + c.itemsExpiringIn30Days + "\n" +
                "Expired Items: " + c.expiredItems;
    }

    private CharSequence buildReadinessText(int householdSize, List<ItemSearchRow> allRows) {
        int waterGallons = computeWaterOnHand(allRows);
        int waterRowCount = countWaterRows(allRows);

        int min3 = householdSize * 3;
        int rec14 = householdSize * 14;
        int prep30 = householdSize * 30;

        String ratingLine;
        String hintLine;

        if (waterRowCount == 0) {
            ratingLine = "Rating: Water category not detected";
            hintLine = "Add items in the water category (assumed gallons)";
        } else if (waterGallons < min3) {
            ratingLine = "Rating: Below minimum";
            hintLine = "See recommendations below";
        } else if (waterGallons < rec14) {
            ratingLine = "Rating: Meets minimum";
            hintLine = "See recommendations below";
        } else if (waterGallons < prep30) {
            ratingLine = "Rating: Meets recommended";
            hintLine = "See recommendations below";
        } else {
            ratingLine = "Rating: You're well prepared!";
            hintLine = "See recommendations below";
        }

        SpannableStringBuilder sb = new SpannableStringBuilder();

        appendBoldLine(sb, "Water");
        sb.append("Tracked: ").append(String.valueOf(waterGallons)).append(" gallons\n");
        sb.append(ratingLine).append("\n");
        sb.append(hintLine).append("\n\n");

        appendBoldLine(sb, "Food");
        sb.append("Rating: Not enough data to estimate automatically.\n");
        sb.append("See recommendations below.\n\n");

        appendBoldLine(sb, "Recommendations");
        sb.append("Minimum (3 days)\n");
        sb.append("• Water: ").append(String.valueOf(min3)).append(" gallons\n");
        sb.append("• Food: At least 3 days of non-perishable food for your household\n\n");
        sb.append("Recommended (2 weeks)\n");
        sb.append("• Water: ").append(String.valueOf(rec14)).append(" gallons\n");
        sb.append("• Food: At least 14 days of non-perishable food for your household\n\n");
        sb.append("Well-prepared (30 days)\n");
        sb.append("• Water: ").append(String.valueOf(prep30)).append(" gallons\n");
        sb.append("• Food: At least 30 days of non-perishable food for your household\n");

        return sb;
    }

    private void appendBoldLine(SpannableStringBuilder sb, String text) {

        int start = sb.length();
        sb.append(text).append("\n");
        sb.setSpan(
                new StyleSpan(Typeface.BOLD),
                start,
                start + text.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
    }

    private int computeWaterOnHand(List<ItemSearchRow> allRows) {
        if (allRows == null) return 0;

        int sum = 0;
        for (ItemSearchRow r : allRows) {
            String cat = r.getCategoryName();
            if (cat != null && cat.trim().equalsIgnoreCase("Water")) {
                sum += Math.max(0, r.getQuantity()); // int gallons
            }
        }
        return sum;
    }

    private int countWaterRows(List<ItemSearchRow> allRows) {
        if (allRows == null) return 0;

        int count = 0;
        for (ItemSearchRow r : allRows) {
            String cat = r.getCategoryName();
            if (cat != null && cat.trim().equalsIgnoreCase("Water")) {
                count++;
            }
        }
        return count;
    }

    private int getHouseholdSize() {
        SharedPreferences sp = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sp.getInt(KEY_HOUSEHOLD_SIZE, DEFAULT_HOUSEHOLD_SIZE);
    }

    private Date parseExpiration(String s) {
        if (TextUtils.isEmpty(s)) return null;
        try {
            return sdf.parse(s.trim());
        } catch (ParseException e) {
            return null;
        }
    }

    private Date startOfDay(Date d) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(d);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date safeDate(Date d) {
        return (d == null) ? new Date(0) : d;
    }

    private void exportCsv() {
        if (lastComputed == null) {
            showToast("Report is not ready yet.");
            return;
        }

        String csv = buildCsvReport();

        String fileName = "EmergencyPreparednessReport_" +
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                        .format(new Date()) + ".csv";

        Uri uri = writeCsv(fileName, csv);
        if (uri == null) return;

        showToast("Saved to Downloads: " + fileName);

        // Share sheet
        shareFile(uri, "text/csv");
    }

    private String buildCsvReport() {
        StringBuilder sb = new StringBuilder();

        String generated = TextUtils.isEmpty(lastGeneratedTimestamp)
                ? new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date())
                : lastGeneratedTimestamp;

        // Header block (2 columns: key, value)
        sb.append(csvRow("Emergency Preparedness Inventory Report")).append("\n");
        sb.append(csvRow("Generated", generated)).append("\n");
        sb.append(csvRow("Household Size", String.valueOf(lastHouseholdSize))).append("\n");
        sb.append("\n");

        // Summary
        sb.append(csvRow("Summary")).append("\n");
        sb.append(csvRow("Total Kits", String.valueOf(lastComputed.totalKits))).append("\n");
        sb.append(csvRow("Total Tracked Items", String.valueOf(lastComputed.totalTrackedItems))).append("\n");
        sb.append(csvRow("Items Expiring in 30 Days", String.valueOf(lastComputed.itemsExpiringIn30Days))).append("\n");
        sb.append(csvRow("Expired Items", String.valueOf(lastComputed.expiredItems))).append("\n");
        sb.append("\n");

        // Readiness
        int waterGallons = computeWaterOnHand(lastAllRows);
        int waterRowCount = countWaterRows(lastAllRows);

        int min3 = lastHouseholdSize * 3;
        int rec14 = lastHouseholdSize * 14;
        int prep30 = lastHouseholdSize * 30;

        String rating;
        if (waterRowCount == 0) {
            rating = "Water category not detected";
        } else if (waterGallons < min3) {
            rating = "Below minimum";
        } else if (waterGallons < rec14) {
            rating = "Meets minimum";
        } else if (waterGallons < prep30) {
            rating = "Meets recommended";
        } else {
            rating = "You're well prepared!";
        }

        sb.append(csvRow("Readiness")).append("\n");
        sb.append(csvRow("Tracked Water (gallons)", String.valueOf(waterGallons))).append("\n");
        sb.append(csvRow("Rating", rating)).append("\n");
        sb.append(csvRow("Minimum (3 days)", String.valueOf(min3))).append("\n");
        sb.append(csvRow("Recommended (2 weeks)", String.valueOf(rec14))).append("\n");
        sb.append(csvRow("Well-prepared (30 days)", String.valueOf(prep30))).append("\n");
        sb.append("\n");

        // Time-Sensitive Items table
        sb.append(csvRow("Time-Sensitive Items (Soonest first)")).append("\n");
        sb.append(csvRow("Item", "Kit", "Location", "Category", "Quantity", "Expiration", "Status")).append("\n");
        for (ItemSearchRow r : safeList(lastExpirationRows)) {
            sb.append(csvRow(
                    nullToEmpty(r.getItemName()),
                    nullToEmpty(r.getKitName()),
                    nullToEmpty(r.getLocation()),
                    nullToEmpty(r.getCategoryName()),
                    String.valueOf(r.getQuantity()),
                    nullToEmpty(r.getExpirationDate()),
                    computeStatusForCsv(r.getExpirationDate())
            )).append("\n");
        }
        sb.append("\n");

        // Full Inventory table
        sb.append(csvRow("Full Inventory (Sorted A-Z)")).append("\n");
        sb.append(csvRow("Item", "Kit", "Location", "Category", "Quantity", "Expiration", "Status")).append("\n");
        for (ItemSearchRow r : safeList(lastAllRows)) {
            sb.append(csvRow(
                    nullToEmpty(r.getItemName()),
                    nullToEmpty(r.getKitName()),
                    nullToEmpty(r.getLocation()),
                    nullToEmpty(r.getCategoryName()),
                    String.valueOf(r.getQuantity()),
                    nullToEmpty(r.getExpirationDate()),
                    computeStatusForCsv(r.getExpirationDate())
            )).append("\n");
        }

        return sb.toString();
    }

    // Writes CSV and returns a shareable Uri - saves to Downloads
    private Uri writeCsv(String fileName, String csv) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = getContentResolver();
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) {
                    showToast("Could not create CSV file.");
                    return null;
                }

                try (OutputStream os = resolver.openOutputStream(uri)) {
                    if (os == null) {
                        showToast("Could not write CSV file.");
                        return null;
                    }
                    os.write(csv.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }

                return uri;
            } else {
                File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                if (dir == null) dir = getFilesDir();

                File outFile = new File(dir, fileName);

                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    fos.write(csv.getBytes(StandardCharsets.UTF_8));
                    fos.flush();
                }

                return FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".fileprovider",
                        outFile
                );
            }
        } catch (Exception e) {
            showToast("CSV export failed.");
            return null;
        }
    }

    private void shareFile(Uri uri, String mimeType) {
        if (uri == null) return;

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType(mimeType);
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        share.setClipData(ClipData.newRawUri("report", uri));

        startActivity(Intent.createChooser(share, "Share report"));
    }

    private String csvRow(String... cols) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cols.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(csvEscape(cols[i]));
        }
        return sb.toString();
    }

    private String csvEscape(String s) {
        if (s == null) return "\"\"";
        String value = s;

        boolean mustQuote = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        value = value.replace("\"", "\"\"");

        if (mustQuote) return "\"" + value + "\"";
        return value;
    }

    private List<ItemSearchRow> safeList(List<ItemSearchRow> list) {
        return list == null ? new ArrayList<>() : list;
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private String computeStatusForCsv(String exp) {
        Date expDate = parseExpiration(exp);
        if (expDate == null) return "OK";

        Date today = startOfDay(new Date());

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(today);
        calendar.add(Calendar.DAY_OF_YEAR, 30);
        Date in30 = calendar.getTime();

        if (expDate.before(today)) return "Expired";
        if (!expDate.after(in30)) return "Expiring Soon";
        return "OK";
    }

    private void copySummaryToClipboard() {
        if (lastComputed == null) {
            showToast("Report is not ready yet.");
            return;
        }

        String text = buildCopySummaryText();

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        if (clipboard == null) {
            showToast("Clipboard is not available.");
            return;
        }

        clipboard.setPrimaryClip(ClipData.newPlainText("Emergency Preparedness Report", text));
        showToast("Summary copied to clipboard.");
    }

    private String buildCopySummaryText() {
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("Emergency Preparedness Inventory Report").append("\n");
        sb.append("Generated: ").append(TextUtils.isEmpty(lastGeneratedTimestamp) ? "Unknown" : lastGeneratedTimestamp).append("\n");
        sb.append("Household Size: ").append(lastHouseholdSize).append("\n\n");

        // Summary numbers
        sb.append("Summary").append("\n");
        sb.append("Total Kits: ").append(lastComputed.totalKits).append("\n");
        sb.append("Total Tracked Items: ").append(lastComputed.totalTrackedItems).append("\n");
        sb.append("Items Expiring in 30 Days: ").append(lastComputed.itemsExpiringIn30Days).append("\n");
        sb.append("Expired Items: ").append(lastComputed.expiredItems).append("\n\n");

        // Readiness (same logic as CSV)
        int waterGallons = computeWaterOnHand(lastAllRows);
        int waterRowCount = countWaterRows(lastAllRows);

        int min3 = lastHouseholdSize * 3;
        int rec14 = lastHouseholdSize * 14;
        int prep30 = lastHouseholdSize * 30;

        String rating;
        if (waterRowCount == 0) {
            rating = "Water category not detected";
        } else if (waterGallons < min3) {
            rating = "Below minimum";
        } else if (waterGallons < rec14) {
            rating = "Meets minimum";
        } else if (waterGallons < prep30) {
            rating = "Meets recommended";
        } else {
            rating = "You're well prepared!";
        }

        sb.append("Readiness").append("\n");
        sb.append("Tracked Water (gallons): ").append(waterGallons).append("\n");
        sb.append("Rating: ").append(rating).append("\n");
        sb.append("Minimum (3 days): ").append(min3).append(" gallons").append("\n");
        sb.append("Recommended (2 weeks): ").append(rec14).append(" gallons").append("\n");
        sb.append("Well-prepared (30 days): ").append(prep30).append(" gallons").append("\n\n");

        return sb.toString();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, message.length() >= 30 ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
    }

    private static class ReportComputed {
        int totalKits;
        int totalTrackedItems;
        int itemsExpiringIn30Days;
        int expiredItems;
        List<ItemSearchRow> expirationRows = new ArrayList<>();
    }
}
