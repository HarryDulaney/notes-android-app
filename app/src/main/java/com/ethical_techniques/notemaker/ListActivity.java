package com.ethical_techniques.notemaker;


import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ethical_techniques.notemaker.DAL.DBUtil;
import com.ethical_techniques.notemaker.adapters.NoteRecycleAdapter;
import com.ethical_techniques.notemaker.auth.AppFlowActivity;
import com.ethical_techniques.notemaker.auth.BaseActivity;
import com.ethical_techniques.notemaker.auth.UpdateProfileActivity;
import com.ethical_techniques.notemaker.auth.UserLoginActivity;
import com.ethical_techniques.notemaker.model.Category;
import com.ethical_techniques.notemaker.model.Note;
import com.ethical_techniques.notemaker.model.PRIORITY;
import com.ethical_techniques.notemaker.utils.DialogUtil;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The Main Activity containing the navigation drawer, most other activities,excluding authentication activities
 * return here have back buttons that will return here.
 *
 * @author Harry Dulaney
 */
public class ListActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    private final String TAG = this.getClass().getName();

    private static List<Note> notes;
    RecyclerView recyclerView;
    NoteRecycleAdapter noteRecycleAdapter;
    boolean editModeActive;
    private List<Category> categories;
    private Spinner spinner;
    private SharedPreferences sharedPreferences;
    ArrayAdapter<Category> categoryArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkMessages();
        //Initialize SharedPreferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.drawer_layout_list);

        //Handle Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //initialize Categories Dropdown (i.e. Spinner)
        spinner = findViewById(R.id.list_activity_category_spinner);
        //Handle create new note floating button
        FloatingActionButton floatingActionButton = findViewById(R.id.new_note_float_button);
        floatingActionButton.setOnLongClickListener((v) -> {
            Snackbar.make(v, "Create a new note", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();

            return true;
        });

        floatingActionButton.setOnClickListener(view -> {
            Intent intent = new Intent(ListActivity.this, NoteActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

        });
        //Handle Nav Drawer
        DrawerLayout navDrawer = findViewById(R.id.drawer_layout_list);
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, navDrawer,
                toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        navDrawer.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        //Handle NavigationView
        NavigationView navigationView = findViewById(R.id.navigation_view);
        Menu menu = navigationView.getMenu();
        menu.findItem(R.id.nav_my_notes).setEnabled(false);

        navigationView.setNavigationItemSelectedListener(ListActivity.this);

        //Replace User Avatar Icon in drawer with users img
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser != null) {
            initUserAvatar(fUser, navigationView);
        }
        menu.findItem(R.id.nav_sync).setEnabled(fUser == null);
        menu.findItem(R.id.nav_logout).setEnabled(fUser != null);
        menu.findItem(R.id.nav_update).setEnabled(fUser != null);

    }

    /* load bundle to get message */
    private void checkMessages() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.getBoolean(getString(R.string.launch_key))) {
                showSyncDecisionDialog();
            }
        }
    }

    /**
     * Display choice dialog prompting the user to login to their cloud account
     */
    private void showSyncDecisionDialog() {
        DialogUtil.makeAndShow(this,
                "Welcome back!",
                "Click SYNC to sign into your account and synchronize your notes now or CONTINUE to " +
                        "take notes now.",
                "SIGN-IN",
                "CONTINUE",
                () -> {
                    Intent i = new Intent(this, UserLoginActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);

                },
                () -> {

                });

    }

    private void initUserAvatar(FirebaseUser fUser, NavigationView nv) {
        ImageView imageView = new ImageView(this);
        Glide.with(this).load(fUser.getPhotoUrl()).into(imageView);

        View navHeaderView = nv.getHeaderView(0);
        TextView displayName = navHeaderView.findViewById(R.id.textViewUserNameDrawer);

        findViewById(R.id.textViewUserNameDrawer);
        displayName.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_account_circle_black_48dp, 0, 0, 0);

        if (fUser.getDisplayName() != null && !fUser.getDisplayName().isEmpty()) {
            displayName.setText(fUser.getDisplayName());
        } else {
            displayName.setText(R.string.nav_drawer_username_default);
            displayName.setOnClickListener(e -> {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(ListActivity.this);
                final EditText dialogInput = new EditText(this);
                dialogInput.setInputType(InputType.TYPE_CLASS_TEXT);
                dialogBuilder.setView(dialogInput);
                dialogBuilder.setTitle("Username not set");
                dialogBuilder.setMessage("What do you like to be called?");
                dialogBuilder.setPositiveButton("SUBMIT", (dialogInterface, which) -> {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    String res = dialogInput.getText().toString();

                });
                dialogBuilder.setNegativeButton("CANCEL", (dialogInterface, which) -> {
                    dialogInterface.cancel();
                });
            });
        }
    }

    /**
     * @param item the MenuItem that was clicked
     * @return boolean success indicator
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_new_note:
                //Open create new note activity
                Intent intent = new Intent(this, NoteActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;

            case R.id.nav_edit_categories:
                //Edit the note categories
                Intent i2 = new Intent(this, CategoryListActivity.class);
//                i2.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i2);
                break;

            case R.id.nav_settings:
                //Open the settings activity
                Intent i = new Intent(this, SettingsActivity.class);
//                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                break;

            case R.id.nav_sync:
                //Open user login activity
                Intent intent1 = new Intent(this, UserLoginActivity.class);
//                intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent1);
                break;
            case R.id.nav_update:
                //Open update user profile activity
                Intent i3 = new Intent(this, UpdateProfileActivity.class);
//                i3.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i3);
                break;
            case R.id.nav_logout:
                FirebaseAuth.getInstance().signOut();
                Snackbar.make(getCurrentFocus(), "", Snackbar.LENGTH_LONG).show();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + item.getItemId());
        }

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout_list);
        drawerLayout.closeDrawer(GravityCompat.START);

        return true;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout_list);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app_bar_top_list_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return true;
    }

    /**
     * @param item that was selected
     * @return true if the item is recognized
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();
        if (id == R.id.action_bar_editSwitch) {
            toggleDeleteIcon();
            //Toggle the icons
            if (!editModeActive) {
                item.setIcon(R.drawable.ic_playlist_check_white_48dp);
                editModeActive = true;
            } else {
                item.setIcon(R.drawable.ic_playlist_edit_white_48dp);
                editModeActive = false;
            }
            return true;
        } else if (id == R.id.list_activity_category_spinner) {
            String categoryName = (String) spinner.getSelectedItem();
            Toast.makeText(this, "Category by the name of " + categoryName + " was selected", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);

    }


    private void toggleDeleteIcon() {
        for (int i = 0; i < noteRecycleAdapter.getItemCount();
             i++) {
            NoteRecycleAdapter.ViewHolder viewHolder = (NoteRecycleAdapter.ViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
            if (viewHolder != null) {
                if (viewHolder.deleteButton.getVisibility() == View.VISIBLE) {
                    viewHolder.deleteButton.setVisibility(View.GONE);
                    viewHolder.priorityStar.setVisibility(View.VISIBLE);
                } else {
                    viewHolder.priorityStar.setVisibility(View.GONE);
                    viewHolder.deleteButton.setVisibility(View.VISIBLE);
                }
                Log.i(TAG, "Edit button on list item " + i + "set to visible");
            } else {
                Log.e(TAG, " The ViewHolder is null");
            }
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        //Retrieve Sort By from sp
        String sortBy = sharedPreferences.getString("sort.by.preference", getString(R.string.SORT_BY_DATE));
        //Retrieves Sort Order from sp
        String sortOrder = sharedPreferences.getString("sort.order.preference", getString(R.string.SORT_ORDER_HIGH_TO_LOW));

        try {
            notes = DBUtil.findNotes(this, sortBy, sortOrder);

        } catch (Exception e) {
            Log.e(TAG, "Error in onResume, inspect DataSource. ");
            Toast.makeText(this, "Error retrieving notes, please try reloading. ", Toast.LENGTH_LONG).show();
        }
        initDropDown();
        recyclerView = findViewById(R.id.recycleList);
        TextView emptyListMessageTopHalf = findViewById(R.id.empty_view1);
        ImageView emptyListImage = findViewById(R.id.empty_view2);
        noteRecycleAdapter = new NoteRecycleAdapter(notes);

        if (notes.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyListMessageTopHalf.setVisibility(View.VISIBLE);
            emptyListImage.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyListMessageTopHalf.setVisibility(View.GONE);
            emptyListImage.setVisibility(View.GONE);
            initNoteList();
        }

    }

    /**
     * Initialize the list of notes and define listener events
     */
    private void initNoteList() {
        /* Set listener event behavior for long click on list item event */
        noteRecycleAdapter.setNoteLongClickListener((view, position) -> {
            int noteId = (int) noteRecycleAdapter.getItemId(position);
            Intent intent = new Intent(ListActivity.this, NoteActivity.class);
            intent.putExtra(getString(R.string.NOTE_ID_KEY), noteId);
            ListActivity.this.startActivity(intent);
        });
        /* Set listener event behavior for regular (short) click on list item */
        noteRecycleAdapter.setNoteClickListener((view, position) -> Toast.makeText(ListActivity.this,
                "Long click to open the note the editing"
                , Toast.LENGTH_LONG).show());

        noteRecycleAdapter.setPriorityStarListener((priorityView, position) -> {
            String toast = "";
            boolean saved = false;
            Note note = notes.get(position);
            if (note.getPRIORITY_LEVEL().equals(PRIORITY.HIGH.getString())) { //Note is currently high priority and user desires to change to low priority
                note.setPRIORITY_LEVEL(PRIORITY.LOW.getString());

                try {
                    saved = DBUtil.saveNote(this, note);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "Exception trying to save note with ID# " + note.getNoteID() + " and title " + note.getNoteName());
                }
                if (saved) {
                    handleTogglePriorityStar(priorityView, false);
                    toast = "This note is set to " + note.getPRIORITY_LEVEL() + " priority";

                } else {
                    toast = "Something went wrong we could not save the note to " + note.getPRIORITY_LEVEL() + " priority";
                }

            } else { //Note is currently Low priority and the user is pressing the star to change it to High priority
                note.setPRIORITY_LEVEL(PRIORITY.HIGH.getString());

                try {
                    saved = DBUtil.saveNote(this, note);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "Exception trying to save note with ID# " + note.getNoteID() + " and title " + note.getNoteName());
                }

                if (saved) {
                    handleTogglePriorityStar(priorityView, true);
                    toast = "This note is set to " + note.getPRIORITY_LEVEL() + " priority";

                } else {
                    toast = "Something went wrong we could not save the note to " + note.getPRIORITY_LEVEL() + " priority";
                }
            }

            Toast.makeText(ListActivity.this,
                    toast,
                    Toast.LENGTH_SHORT).show();

            noteRecycleAdapter.notifyDataSetChanged();
        });
        /* Set listener event behavior for regular click on delete button */
        noteRecycleAdapter.setDeleteButtonListener((view, position) -> {
            Note note = notes.get(position);

            DialogUtil.makeAndShow(this,
                    "Confirmation Popup",
                    "Are you sure you want to permanently delete the Note: " + note.getNoteName(),
                    "delete",
                    "cancel",
                    () -> {
                        try {
                            DBUtil.deleteNote(this, note.getNoteID());
                            Toast.makeText(this, "The Note titled " + note.getNoteName() + " was deleted.", Toast.LENGTH_LONG).show();
                            notes.remove(position);
                            noteRecycleAdapter.notifyDataSetChanged();

                        } catch (Exception e) {
                            Toast.makeText(this, "An error occurred and the note could note be deleted :(", Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Exception @ delete(int noteID)..NoteId = " + note.getNoteID() +
                                    " Exception's message: " + e.getMessage(), e);
                        }
                    });
        });

        recyclerView.setAdapter(noteRecycleAdapter);

    }

    /**
     *
     */
    void saveNoteChanges() {
        for (int i = 0; i < noteRecycleAdapter.getItemCount(); i++) {
            Note note = noteRecycleAdapter.getItem(i);
            try {
                DBUtil.saveNote(this, note);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Initialize the category dropdown menu
     */
    private void initDropDown() {
        try {
            categories = DBUtil.getCategories(this);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        List<String> categoryStrings = new ArrayList<>();
        // Add all Categories names to the list for the dropdown spinner
        for (Category category : categories) {
            categoryStrings.add(category.getName());
        }

        ArrayAdapter<String> categoryArrayAdapter = new ArrayAdapter<>(this,
                R.layout.dropdown_item_simple, categoryStrings);
        spinner.setAdapter(categoryArrayAdapter);
        spinner.setSelection(0);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                String categoryName = (String) parent.getItemAtPosition(position);
                Toast.makeText(ListActivity.this,
                        "Displaying " + categoryName + " notes",
                        Toast.LENGTH_LONG).show();
                Log.i("checkedTextView = " + categoryName, TAG);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    /**
     * Switches the priority star on and off
     */
    private void handleTogglePriorityStar(ImageButton starView, boolean colorTheStar) {
        Drawable starDrawable = starView.getDrawable();
        if (starDrawable != null) {
            starDrawable.mutate();
            if (colorTheStar) {
                starDrawable.setColorFilter(ContextCompat.getColor(this, R.color.colorPriorityHigh), PorterDuff.Mode.SRC_ATOP);

            } else {
                starDrawable.setColorFilter(null);


            }
        }
    }
}