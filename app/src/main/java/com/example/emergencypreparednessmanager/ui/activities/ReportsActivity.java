package com.example.emergencypreparednessmanager.ui.activities;

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
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.emergencypreparednessmanager.R;
import com.example.emergencypreparednessmanager.database.ItemSearchRow;
import com.example.emergencypreparednessmanager.database.Repository;
import com.example.emergencypreparednessmanager.ui.adapters.ReportRowAdapter;
import com.example.emergencypreparednessmanager.util.AppConstants;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.color.MaterialColors;
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
import java.util.Objects;

/**
 * Activity displaying the preparedness reports (summary, readiness, expiration, full inventory).
 * <p>
 * Features:
 * <ul>
 *   <li>Computed summary (kits/items/expiring/expired)</li>
 *   <li>Water readiness calculation with household size</li>
 *   <li>Time-sensitive expiration list + full inventory</li>
 *   <li>Export to CSV + copy summary to clipboard (via overflow menu)</li>
 *   <li>External resource links (Ready.gov, CDC, etc.)</li>
 * </ul>
 */
public class ReportsActivity extends AppCompatActivity {

  private final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
  private MaterialToolbar toolbar;
  private MaterialTextView tvGenerated;
  private MaterialTextView tvHousehold;
  private MaterialTextView tvSummaryBody;
  private MaterialTextView tvReadinessBody;
  private RecyclerView rvExpiration;
  private RecyclerView rvInventory;
  private MaterialTextView tvExpirationTitle;
  private MaterialTextView tvSourcesBody;
  private MaterialTextView tvCommunityBody;
  private Repository repository;
  private List<ItemSearchRow> lastAllRows = new ArrayList<>();
  private List<ItemSearchRow> lastExpirationRows = new ArrayList<>();
  private ReportComputed lastComputed;
  private int lastHouseholdSize = AppConstants.DEFAULT_HOUSEHOLD_SIZE;
  private String lastGeneratedTimestamp = "";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_reports);

    repository = new Repository(getApplication());

    bindViews();
    setupInsets();
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

    tvExpirationTitle = findViewById(R.id.tvExpirationTitle);  // New reference
    rvExpiration = findViewById(R.id.rvExpiration);
    rvInventory = findViewById(R.id.rvInventory);

    tvSourcesBody = findViewById(R.id.tvSourcesBody);
    tvCommunityBody = findViewById(R.id.tvCommunityBody);
  }

  private void setupInsets() {
    if (toolbar != null) {
      ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
        WindowInsetsCompat insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(
            Objects.requireNonNull(insets.toWindowInsets()));
        androidx.core.graphics.Insets systemBars = insetsCompat.getInsets(
            WindowInsetsCompat.Type.systemBars());

        v.setPadding(
            systemBars.left,
            systemBars.top,
            systemBars.right,
            v.getPaddingBottom()
        );

        toolbar.setTitleCentered(true);

        return insets;
      });
    }

    NestedScrollView scrollView = findViewById(R.id.scrollView);
    if (scrollView != null) {
      ViewCompat.setOnApplyWindowInsetsListener(scrollView, (v, insets) -> {
        WindowInsetsCompat insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(
            Objects.requireNonNull(insets.toWindowInsets()));
        androidx.core.graphics.Insets systemBars = insetsCompat.getInsets(
            WindowInsetsCompat.Type.systemBars());

        v.setPadding(
            v.getPaddingLeft(),
            v.getPaddingTop(),
            v.getPaddingRight(),
            systemBars.bottom
        );

        return insets;
      });
    }
  }

  private void setupToolbar() {
    if (toolbar == null) {
      return;
    }

    setSupportActionBar(toolbar);
    toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
  }

  /**
   * Inflates the overflow menu with Copy Summary and Export CSV actions.
   */
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_reports, menu);
    return true;
  }

  /**
   * Disables menu items if the report data is not yet loaded/ready.
   */
  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    boolean ready = lastComputed != null;

    MenuItem copy = menu.findItem(R.id.action_copy_summary);
    if (copy != null) {
      copy.setEnabled(ready);
    }

    MenuItem export = menu.findItem(R.id.action_export_csv);
    if (export != null) {
      export.setEnabled(ready);
    }

    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    int id = item.getItemId();
    if (id == R.id.action_copy_summary) {
      copySummaryToClipboard();
      return true;
    } else if (id == R.id.action_export_csv) {
      exportCsv();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void setupLists() {
    rvExpiration.setLayoutManager(new LinearLayoutManager(this));
    rvInventory.setLayoutManager(new LinearLayoutManager(this));
  }

  private void loadReport() {
    lastGeneratedTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());
    tvGenerated.setText(getString(R.string.generated_format, lastGeneratedTimestamp));

    lastHouseholdSize = getHouseholdSize();
    tvHousehold.setText(getString(R.string.household_size_format, lastHouseholdSize));

    setupResourceLinks();

    repository.getAllKits(kits -> {
      int totalKits = kits == null ? 0 : kits.size();

      repository.getInventoryReportRows(rows -> {
        List<ItemSearchRow> allRows = rows == null ? new ArrayList<>() : rows;

        ReportComputed computed = computeReport(allRows, totalKits);
        lastComputed = computed;
        lastAllRows = allRows;
        lastExpirationRows = computed.expirationRows;

        tvSummaryBody.setText(buildSummaryText(computed));
        tvReadinessBody.setText(buildReadinessText(lastHouseholdSize, allRows));

        // Control expiration visibility directly (no wrapper View)
        boolean hasExpiring = !computed.expirationRows.isEmpty();
        rvExpiration.setVisibility(hasExpiring ? View.VISIBLE : View.GONE);
        tvExpirationTitle.setVisibility(
            hasExpiring ? View.VISIBLE : View.GONE);  // Hide title too if empty

        rvExpiration.setAdapter(new ReportRowAdapter(computed.expirationRows, true));
        rvInventory.setAdapter(new ReportRowAdapter(allRows, false));
      });
    });
  }

  private void setupResourceLinks() {
    tvSourcesBody.setMovementMethod(LinkMovementMethod.getInstance());
    tvCommunityBody.setMovementMethod(LinkMovementMethod.getInstance());

    SpannableStringBuilder sources = new SpannableStringBuilder();

    appendLink(sources, "Ready.gov – Emergency Kit", "https://www.ready.gov/kit");
    sources.append("\n");

    appendLink(sources, "Ready.gov – Water Storage", "https://www.ready.gov/water");
    sources.append("\n");

    appendLink(sources, "CDC – Emergency Water Supply", "https://www.cdc.gov/"
        + "water-emergency/about/how-to-create-and-store-an-emergency-water-supply.html");
    sources.append("\n");

    appendLink(sources, "UGA - Long Term Food Storage", "https://www.fcs.uga.edu/"
        + "extension/preparing-an-emergency-food-supply-long-term-food-storage");

    tvSourcesBody.setText(sources);
    tvSourcesBody.setLinkTextColor(
        com.google.android.material.color.MaterialColors.getColor(
            tvSourcesBody,
            androidx.appcompat.R.attr.colorPrimary
        )
    );

    SpannableStringBuilder community = new SpannableStringBuilder();

    appendLink(community, "TheUrbanPrepper", "https://www.youtube.com/@TheUrbanPrepper");
    community.append("\n");

    appendLink(community, "PeakSurvival", "https://www.youtube.com/@PeakSurvival"
    );
    community.append("\n");

    appendLink(community, "Garand Thumb", "https://www.youtube.com/@GarandThumb"
    );

    tvCommunityBody.setText(community);
    tvCommunityBody.setLinkTextColor(
        com.google.android.material.color.MaterialColors.getColor(
            tvCommunityBody,
            androidx.appcompat.R.attr.colorPrimary
        )
    );
  }

  private void appendLink(SpannableStringBuilder sb, String text, String url) {
    int start = sb.length();
    sb.append(text);
    sb.setSpan(new URLSpan(url), start, start + text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
  }
  //endregion

  //region Report Computation
  private ReportComputed computeReport(List<ItemSearchRow> rows, int totalKits) {
    Date today = startOfDay(new Date());

    Calendar cal = Calendar.getInstance();
    cal.setTime(today);
    cal.add(Calendar.DAY_OF_YEAR, AppConstants.DAYS_BEFORE_EXPIRATION_FOR_WARNING);
    Date in30 = cal.getTime();

    int totalTrackedItems = rows.size();
    int expiringIn30 = 0;
    int expired = 0;

    List<ItemSearchRow> expirationList = new ArrayList<>();

    for (ItemSearchRow r : rows) {
      Date expDate = parseExpiration(r.getExpirationDate());
      if (expDate == null) {
        continue;
      }

      if (expDate.before(today)) {
        expired++;
        expirationList.add(r);
      } else if (!expDate.after(in30)) {
        expiringIn30++;
        expirationList.add(r);
      }
    }

    expirationList.sort(Comparator.comparing(r ->
        safeDate(parseExpiration(r.getExpirationDate()))));

    ReportComputed c = new ReportComputed();
    c.totalKits = totalKits;
    c.totalTrackedItems = totalTrackedItems;
    c.itemsExpiringIn30Days = expiringIn30;
    c.expiredItems = expired;
    c.expirationRows = expirationList;

    return c;
  }

  private String buildSummaryText(ReportComputed c) {
    return getString(R.string.report_summary_format,
        c.totalKits,
        c.totalTrackedItems,
        c.itemsExpiringIn30Days,
        c.expiredItems);
  }

  private CharSequence buildReadinessText(int householdSize, List<ItemSearchRow> allRows) {
    int waterGallons = computeWaterOnHand(allRows);
    int waterRowCount = countWaterRows(allRows);

    int min3 = householdSize * AppConstants.WATER_MIN_DAYS;
    int rec14 = householdSize * AppConstants.WATER_REC_DAYS;
    int prep30 = householdSize * AppConstants.WATER_PREP_DAYS;

    String ratingLine;
    String hintLine;

    if (waterRowCount == 0) {
      ratingLine = getString(R.string.water_not_detected);
      hintLine = getString(R.string.add_water_items);
    } else if (waterGallons < min3) {
      ratingLine = getString(R.string.rating_below_minimum);
      hintLine = getString(R.string.see_recommendations);
    } else if (waterGallons < rec14) {
      ratingLine = getString(R.string.rating_meets_minimum);
      hintLine = getString(R.string.see_recommendations);
    } else if (waterGallons < prep30) {
      ratingLine = getString(R.string.rating_meets_recommended);
      hintLine = getString(R.string.see_recommendations);
    } else {
      ratingLine = getString(R.string.rating_well_prepared);
      hintLine = getString(R.string.see_recommendations);
    }

    SpannableStringBuilder sb = new SpannableStringBuilder();

    // Water section
    appendBoldLine(sb, getString(R.string.water));
    sb.append(
        getResources().getQuantityString(
            R.plurals.tracked_water_gallons,
            waterGallons,
            waterGallons
        )
    ).append("\n");
    sb.append(ratingLine).append("\n");
    sb.append(hintLine).append("\n\n");

    // Food section
    appendBoldLine(sb, getString(R.string.food));
    sb.append(getString(R.string.rating_not_enough_data)).append("\n");
    sb.append(getString(R.string.see_recommendations)).append("\n\n");

    // Recommendations section
    int titleStart = sb.length();
    sb.append(getText(R.string.recommendations)).append("\n");

    // Apply title medium & primary color to "Recommendations"
    sb.setSpan(
        new android.text.style.TextAppearanceSpan(
            this,
            com.google.android.material.R.style.TextAppearance_Material3_TitleMedium
        ),
        titleStart,
        sb.length(),
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    );
    sb.setSpan(
        new android.text.style.ForegroundColorSpan(
            MaterialColors.getColor(tvReadinessBody, androidx.appcompat.R.attr.colorPrimary)
        ),
        titleStart,
        sb.length(),
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    );

    // Minimum (3 days)
    sb.append(getString(R.string.recommendations_min_label)).append("\n");
    sb.append(
        getResources().getQuantityString(
            R.plurals.recommendations_water,
            min3,
            min3
        )
    ).append("\n");
    sb.append(getString(R.string.food_min_3_days)).append("\n\n");

    // Recommended (2 weeks)
    sb.append(getString(R.string.recommendations_rec_label)).append("\n");
    sb.append(
        getResources().getQuantityString(
            R.plurals.recommendations_water,
            rec14,
            rec14
        )
    ).append("\n");
    sb.append(getString(R.string.food_rec_14_days)).append("\n\n");

    // Well-prepared (30 days)
    sb.append(getString(R.string.recommendations_prep_label)).append("\n");
    sb.append(
        getResources().getQuantityString(
            R.plurals.recommendations_water,
            prep30,
            prep30
        )
    ).append("\n");
    sb.append(getString(R.string.food_prep_30_days)).append("\n");

    return sb;
  }

  private void appendBoldLine(SpannableStringBuilder sb, String text) {
    int start = sb.length();
    sb.append(text).append("\n");
    sb.setSpan(new StyleSpan(Typeface.BOLD),
        start,
        start + text.length(),
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
  }

  private int computeWaterOnHand(List<ItemSearchRow> allRows) {
    if (allRows == null) {
      return 0;
    }

    int sum = 0;
    String water = getString(R.string.water);

    for (ItemSearchRow r : allRows) {
      String cat = r.getCategoryName();
      if (water.equalsIgnoreCase(cat)) {
        sum += Math.max(0, r.getQuantity());
      }
    }
    return sum;
  }

  private int countWaterRows(List<ItemSearchRow> allRows) {
    if (allRows == null) {
      return 0;
    }

    int count = 0;
    String water = getString(R.string.water);
    for (ItemSearchRow r : allRows) {
      String cat = r.getCategoryName();
      if (water.equalsIgnoreCase(cat)) {
        count++;
      }
    }
    return count;
  }

  private int getHouseholdSize() {
    SharedPreferences sp = getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE);
    return sp.getInt(AppConstants.KEY_HOUSEHOLD_SIZE, AppConstants.DEFAULT_HOUSEHOLD_SIZE);
  }
  //endregion

  //region Export & Share
  private void exportCsv() {
    if (lastComputed == null) {
      showToast(getString(R.string.report_not_ready));
      return;
    }

    String csv = buildCsvReport();

    String fileName = AppConstants.REPORT_FILE_PREFIX +
        new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".csv";

    Uri uri = writeCsv(fileName, csv);
    if (uri == null) {
      return;
    }

    showToast(getString(R.string.saved_to_downloads, fileName));
    shareFile(uri);
  }

  private String buildCsvReport() {
    StringBuilder sb = new StringBuilder();

    String generated = TextUtils.isEmpty(lastGeneratedTimestamp)
        ? new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date())
        : lastGeneratedTimestamp;

    // Header block
    sb.append(csvRow(getString(R.string.emergency_preparedness_report))).append("\n");
    sb.append(csvRow(getString(R.string.generated), generated)).append("\n");
    sb.append(csvRow(getString(R.string.household_size), String.valueOf(lastHouseholdSize)))
        .append("\n\n");

    // Summary
    sb.append(csvRow(getString(R.string.summary))).append("\n");
    sb.append(csvRow(getString(
                R.string.total_kits), String.valueOf(lastComputed.totalKits
            )
        ))
        .append("\n");
    sb.append(csvRow(getString(
                R.string.total_tracked_items), String.valueOf(lastComputed.totalTrackedItems
            )
        ))
        .append("\n");
    sb.append(csvRow(getString(
                R.string.items_expiring_in_30_days), String.valueOf(lastComputed.itemsExpiringIn30Days
            )
        ))
        .append("\n");
    sb.append(csvRow(getString(R.string.expired_items), String.valueOf(lastComputed.expiredItems
            )
        ))
        .append("\n\n");

    // Readiness
    int waterGallons = computeWaterOnHand(lastAllRows);
    int waterRowCount = countWaterRows(lastAllRows);

    int min3 = lastHouseholdSize * AppConstants.WATER_MIN_DAYS;
    int rec14 = lastHouseholdSize * AppConstants.WATER_REC_DAYS;
    int prep30 = lastHouseholdSize * AppConstants.WATER_PREP_DAYS;

    String rating;
    if (waterRowCount == 0) {
      rating = getString(R.string.water_not_detected);
    } else if (waterGallons < min3) {
      rating = getString(R.string.below_minimum);
    } else if (waterGallons < rec14) {
      rating = getString(R.string.meets_minimum);
    } else if (waterGallons < prep30) {
      rating = getString(R.string.meets_recommended);
    } else {
      rating = getString(R.string.well_prepared);
    }

    sb.append(csvRow(getString(R.string.readiness))).append("\n");

    // Two-column tracked water line
    String gallonsText = getResources().getQuantityString(
        R.plurals.gallons,
        waterGallons,
        waterGallons
    );
    sb.append(csvRow(getString(R.string.tracked_water), gallonsText)).append("\n");

    // Two-column rating line
    sb.append(csvRow(getString(R.string.rating), rating)).append("\n\n");

    // Recommendations (two columns each)
    sb.append(csvRow(getString(R.string.recommendations))).append("\n");

    sb.append(csvRow(
        getString(R.string.recommendations_min_label), // "Minimum (3 days)"
        getResources().getQuantityString(R.plurals.gallons, min3, min3)
    )).append("\n");

    sb.append(csvRow(
        getString(R.string.recommendations_rec_label), // "Recommended (2 weeks)"
        getResources().getQuantityString(R.plurals.gallons, rec14, rec14)
    )).append("\n");

    sb.append(csvRow(
        getString(R.string.recommendations_prep_label), // "Well-prepared (30 days)"
        getResources().getQuantityString(R.plurals.gallons, prep30, prep30)
    )).append("\n\n");

    // Time-Sensitive Items table
    sb.append(csvRow(getString(R.string.time_sensitive_items)))
        .append("\n");
    sb.append(csvRow(
            getString(R.string.csv_header_item),
            getString(R.string.csv_header_kit),
            getString(R.string.csv_header_location),
            getString(R.string.csv_header_category),
            getString(R.string.header_quantity),
            getString(R.string.csv_header_expiration),
            getString(R.string.csv_header_status)))
        .append("\n");
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
    sb.append(csvRow(getString(R.string.full_inventory)))
        .append("\n");
    sb.append(csvRow(
            getString(R.string.csv_header_item),
            getString(R.string.csv_header_kit),
            getString(R.string.csv_header_location),
            getString(R.string.csv_header_category),
            getString(R.string.csv_header_quantity),
            getString(R.string.csv_header_expiration),
            getString(R.string.csv_header_status)))
        .append("\n");
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

  private Uri writeCsv(String fileName, String csv) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, AppConstants.MIME_TYPE_CSV);
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
          showToast(getString(R.string.csv_create_failed));
          return null;
        }

        try (OutputStream os = resolver.openOutputStream(uri)) {
          if (os == null) {
            showToast(getString(R.string.csv_write_failed));
            return null;
          }
          os.write(csv.getBytes(StandardCharsets.UTF_8));
          os.flush();
        }

        return uri;
      } else {
        File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir == null) {
          dir = getFilesDir();
        }

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
      showToast(getString(R.string.csv_export_failed));
      return null;
    }
  }

  private void shareFile(Uri uri) {
    if (uri == null) {
      return;
    }

    Intent share = new Intent(Intent.ACTION_SEND);
    share.setType(AppConstants.MIME_TYPE_CSV);
    share.putExtra(Intent.EXTRA_STREAM, uri);
    share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

    // Helps email clients set a default subject
    share.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.report_share_subject));

    // Plain text fallback for apps that can't handle CSV
    share.putExtra(Intent.EXTRA_TEXT, getString(R.string.report_share_text));

    share.setClipData(ClipData.newRawUri("report", uri));

    try {
      startActivity(Intent.createChooser(share, getString(R.string.share_report)));
    } catch (Exception e) {
      showToast(getString(R.string.share_no_app_available));
    }
  }

  private String csvRow(String... cols) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < cols.length; i++) {
      if (i > 0) {
        sb.append(",");
      }
      sb.append(csvEscape(cols[i]));
    }
    return sb.toString();
  }

  private String csvEscape(String s) {
    if (s == null) {
      return "\"\"";
    }
    String value = s.replace("\"", "\"\"");
    boolean mustQuote = value.contains(",")
        || value.contains("\"")
        || value.contains("\n")
        || value.contains("\r");
    value = value.replace("\"", "\"\"");
    return mustQuote ? "\"" + value + "\"" : value;
  }

  private void copySummaryToClipboard() {
    if (lastComputed == null) {
      showToast(getString(R.string.report_not_ready));
      return;
    }

    String text = buildCopySummaryText();

    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    if (clipboard == null) {
      showToast(getString(R.string.clipboard_unavailable));
      return;
    }

    clipboard.setPrimaryClip(ClipData.newPlainText(
        AppConstants.REPORT_SUMMARY_CLIPBOARD_LABEL, text));
    showToast(getString(R.string.summary_copied));
  }

  private String buildCopySummaryText() {
    StringBuilder sb = new StringBuilder();

    // Header
    sb.append(getString(R.string.emergency_preparedness_report)).append("\n");
    sb.append(getString(R.string.generated)).append(": ")
        .append(TextUtils.isEmpty(lastGeneratedTimestamp) ? "Unknown" : lastGeneratedTimestamp)
        .append("\n");
    sb.append(getString(R.string.household_size)).append(": ").append(lastHouseholdSize)
        .append("\n\n");

    // Summary numbers
    sb.append(getString(R.string.summary)).append("\n");
    sb.append(getString(R.string.total_kits_formatted, lastComputed.totalKits)).append("\n");
    sb.append(getString(R.string.total_tracked_items_formatted, lastComputed.totalTrackedItems))
        .append("\n");
    sb.append(
            getString(R.string.items_expiring_in_30_days_formatted, lastComputed.itemsExpiringIn30Days))
        .append("\n");
    sb.append(getString(R.string.expired_items_formatted, lastComputed.expiredItems))
        .append("\n\n");

    // Readiness (same logic as CSV)
    int waterGallons = computeWaterOnHand(lastAllRows);
    int waterRowCount = countWaterRows(lastAllRows);

    int min3 = lastHouseholdSize * AppConstants.WATER_MIN_DAYS;
    int rec14 = lastHouseholdSize * AppConstants.WATER_REC_DAYS;
    int prep30 = lastHouseholdSize * AppConstants.WATER_PREP_DAYS;

    String rating;
    if (waterRowCount == 0) {
      rating = getString(R.string.water_not_detected);
    } else if (waterGallons < min3) {
      rating = getString(R.string.rating_below_minimum);
    } else if (waterGallons < rec14) {
      rating = getString(R.string.rating_meets_minimum);
    } else if (waterGallons < prep30) {
      rating = getString(R.string.rating_meets_recommended);
    } else {
      rating = getString(R.string.rating_well_prepared);
    }

    sb.append(getString(R.string.readiness)).append("\n");

    // Tracked water
    sb.append(
        getResources().getQuantityString(
            R.plurals.tracked_water_gallons,
            waterGallons,
            waterGallons
        )
    ).append("\n");
    sb.append(getString(R.string.rating_formatted, rating)).append("\n\n");

    sb.append(getString(R.string.recommendations)).append("\n");

    // Water recommendations
    sb.append(
        getResources().getQuantityString(
            R.plurals.water_min_3_days,
            min3,
            min3
        )
    ).append("\n");
    sb.append(
        getResources().getQuantityString(
            R.plurals.water_rec_14_days,
            rec14,
            rec14
        )
    ).append("\n");
    sb.append(
        getResources().getQuantityString(
            R.plurals.water_prep_30_days,
            prep30,
            prep30
        )
    ).append("\n");

    return sb.toString();
  }
  //endregion

  //region Helpers
  private Date parseExpiration(String s) {
    if (TextUtils.isEmpty(s)) {
      return null;
    }
    try {
      return sdf.parse(s.trim());
    } catch (ParseException e) {
      return null;
    }
  }

  private Date startOfDay(Date d) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(d);
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    return cal.getTime();
  }

  private Date safeDate(Date d) {
    return (d == null) ? new Date(0) : d;
  }

  private List<ItemSearchRow> safeList(List<ItemSearchRow> list) {
    return list == null ? new ArrayList<>() : list;
  }

  private String nullToEmpty(String s) {
    return s == null ? "" : s;
  }

  private String computeStatusForCsv(String exp) {
    Date expDate = parseExpiration(exp);
    if (expDate == null) {
      return getString(R.string.OK);
    }

    Date today = startOfDay(new Date());
    Calendar cal = Calendar.getInstance();
    cal.setTime(today);
    cal.add(Calendar.DAY_OF_YEAR, AppConstants.DAYS_BEFORE_EXPIRATION_FOR_WARNING);
    Date in30 = cal.getTime();

    if (expDate.before(today)) {
      return getString(R.string.expired);
    }
    if (!expDate.after(in30)) {
      return getString(R.string.expiring_soon);
    }
    return getString(R.string.OK);
  }

  private void showToast(String message) {
    Toast.makeText(this, message, message.length() >= 30
        ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
  }
  //endregion

  // region Inner Class
  private static class ReportComputed {

    int totalKits;
    int totalTrackedItems;
    int itemsExpiringIn30Days;
    int expiredItems;
    List<ItemSearchRow> expirationRows = new ArrayList<>();
  }
  //endregion
}
