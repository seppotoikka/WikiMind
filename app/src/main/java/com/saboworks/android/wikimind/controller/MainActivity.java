package com.saboworks.android.wikimind.controller;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.saboworks.android.wikimind.R;
import com.saboworks.android.wikimind.analysisEngine.Concept;
import com.saboworks.android.wikimind.analysisEngine.SearchResult;
import com.saboworks.android.wikimind.analysisEngine.WikiSearchTask;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements AsyncResponse {

    //TODO: finish localization

    private SearchHistory searchHistory;

    private Button searchButton;
    private ImageButton backButton;
    private ImageButton openInWikipediaButton;
    private EditText searchText;
    private TextView summaryText;
    private ListView linksList;
    private ArrayAdapter<String> linksListAdapter;

    private boolean apiQueryInProgress;

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        searchHistory = new SearchHistory();

        searchButton = (Button) findViewById(R.id.search_button);
        searchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                initiateSearch();
            }
        });

        backButton = (ImageButton) findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                SearchResult lastResult = searchHistory.goBack();
                updateGUI(lastResult);
            }
        });

        searchText = (EditText) findViewById(R.id.search_text);
        searchText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event)
            {
                if (event.getAction() == KeyEvent.ACTION_DOWN)
                {
                    switch (keyCode)
                    {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:
                            initiateSearch();
                            return true;
                        default:
                            break;
                    }
                }
                return false;
            }
        });

        openInWikipediaButton = (ImageButton) findViewById(R.id.open_wikipedia_button);
        openInWikipediaButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                openInWikipedia();
            }
        });

        summaryText = (TextView) findViewById(R.id.summary_text);
        summaryText.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                showFullSummaryText();
            }
        });

        linksList = (ListView) findViewById(R.id.links_list);

        apiQueryInProgress = false;
        mContext = this;
    }

    /* Open the current wikipedia article in browser
    */
    private void openInWikipedia() {
        if (apiQueryInProgress){
            //do nothing. Just in case, this should not be happening
        }
        else {
            SearchResult currentResult = searchHistory.getLatestResult();
            if (currentResult != null) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://"
                        + getString(R.string.url_language_prefix) + ".wikipedia.org/wiki/" + currentResult.getPageName()));
                startActivity(browserIntent);
            }
        }
    }


    /*Opens a dialog with full article summary text when summary box is tapped */
    private void showFullSummaryText() {
        SearchResult result = searchHistory.getLatestResult();
        if (result != null) {
            new AlertDialog.Builder(mContext)
                    .setTitle(result.getPageName())
                    .setMessage(result.getPageSummaryText())
                    .setNeutralButton("Close", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*If there is no query in progress, hide the soft keyboard and initiate
    * Wikipedia API queries using the string parameter from searchText field
     */
    private void initiateSearch() {
        if (apiQueryInProgress){
            //just in case, this should not happen
        }
        else {
            //hide soft keyboard if it is visible
            InputMethodManager imm = (InputMethodManager)
            getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(
                    searchButton.getWindowToken(), 0);

            setQueryInProgressStatus(true);

            WikiSearchTask wst = new WikiSearchTask();
            //pass a reference to main activity to the created asyncTask
            wst.delegate = this;
            wst.execute(searchText.getText().toString(), getString(R.string.url_language_prefix));
        }
    }

    /*Update UI elements status and content when a query is initiated or finished
     */
    private void setQueryInProgressStatus(boolean apiQueryRunning) {
        apiQueryInProgress = apiQueryRunning;

        searchText.setFocusable(!apiQueryRunning);
        searchText.setFocusableInTouchMode(!apiQueryRunning);
        searchButton.setEnabled(!apiQueryRunning);
        openInWikipediaButton.setEnabled(!apiQueryRunning);

        if (apiQueryRunning) {
            summaryText.setText(R.string.searching_message);
            linksList.setAdapter(null);
        }
    }

    /* NOTE! Called from another thread, use volatile/synchronized if needed! */
    public void updateGUI(SearchResult sr) {
        if (sr != null) {
            searchHistory.addResult(sr);
            searchText.setText(sr.getPageName());
            summaryText.setText(sr.getPageSummaryText());

            // Populate links list adapter
            ArrayList<String> relatedConceptNames = new ArrayList<>();
            for (Concept c : sr.getRelatedConcepts()) {
                relatedConceptNames.add(c.getName());
            }
            linksListAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, android.R.id.text1, relatedConceptNames);
            linksList.setAdapter(linksListAdapter);

            // Set listener to open a dialog with concept summary text when selected
            linksList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    // ListView Clicked item value
                    String  itemValue    = (String) linksList.getItemAtPosition(position);
                    Concept clickedConcept = null;
                    SearchResult latestResult = searchHistory.getLatestResult();
                    if (latestResult != null) {
                        for (Concept c: searchHistory.getLatestResult().getRelatedConcepts()) {
                            if (c.getName().equals(itemValue)) {
                                clickedConcept = c;
                            }
                        }
                        // Show Dialog
                        if (clickedConcept != null) {
                            final Concept fClickedConcept = clickedConcept;
                            new AlertDialog.Builder(mContext)
                                    .setTitle(clickedConcept.getName())
                                    .setMessage(clickedConcept.getSummaryText())
                                    .setPositiveButton("Go to", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            searchText.setText(fClickedConcept.getName());
                                            initiateSearch();
                                            dialog.cancel();
                                        }
                                    })
                                    .setNeutralButton("Close", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.cancel();
                                        }
                                    })
                                    .setIcon(android.R.drawable.ic_dialog_info)
                                    .show();
                        }
                    }
                }
            });
        }
        else {
            summaryText.setText(getString(R.string.no_results_found));
        }
        //finally, set ongoing query status to false
        setQueryInProgressStatus(false);
    }
}
