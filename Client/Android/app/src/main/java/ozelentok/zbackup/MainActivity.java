package ozelentok.zbackup;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements ActionBar.TabListener {

    public static final int LOCAL_ARRAY = 0;
    public static final int NETWORK_ARRAY = 1;
    private static final int FILE_CHOICE_REQUEST = 1;
    private static final String CONFIG_FILE_NAME = "path.config";

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private ArrayList<ArrayList<BackupItem>> backupItemsArrays;
    private BackupListAdapter[] backupAdapters;
    private boolean backupsLocked;
    private Backuper backuper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadBackupItems();
        initAdapters();
        initPages();
        checkForPermissions();
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveBackupItems();
    }

    private void checkForPermissions() {
        if (Build.VERSION.SDK_INT < 23) {
        	return;
        }
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    private void saveBackupItems() {
        File f = new File(getFilesDir(), CONFIG_FILE_NAME);
        try {
            FileOutputStream fileOut = new FileOutputStream(f);
            ObjectOutputStream objOut = new ObjectOutputStream(fileOut);
            objOut.writeObject(backupItemsArrays);
            objOut.close();
            fileOut.close();
        }
        catch (IOException e) {
            Toast.makeText(this, "Failed to Save Paths\n" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadBackupItems() {
        File f = new File(getFilesDir(), CONFIG_FILE_NAME);
        try {
            FileInputStream fileIn = new FileInputStream(f);
            ObjectInputStream objIn = new ObjectInputStream(fileIn);
            backupItemsArrays = (ArrayList<ArrayList<BackupItem>>)(objIn.readObject());
            fileIn.close();
            objIn.close();
        }
        catch (IOException | ClassNotFoundException e) {
            Toast.makeText(this, "Failed to Load Paths\n" + e.getMessage(), Toast.LENGTH_SHORT).show();
            initEmptyPaths();
        }
    }

    private void initEmptyPaths() {
        backupItemsArrays = new ArrayList<ArrayList<BackupItem>>();
        backupItemsArrays.add(new ArrayList<BackupItem>());
        backupItemsArrays.add(new ArrayList<BackupItem>());
    }

    private void initAdapters() {
        backupAdapters = new BackupListAdapter[] {
                new BackupListAdapter(backupItemsArrays.get(LOCAL_ARRAY), this),
                new BackupListAdapter(backupItemsArrays.get(NETWORK_ARRAY), this)
        };
    }

    private void initPages() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        mSectionsPagerAdapter = new SectionsPagerAdapter(
                getSupportFragmentManager(),backupAdapters);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
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
        switch(id) {
            case R.id.action_add_item:
                showNewItemPrompt();
                return true;

            case R.id.action_backup_all:
                startBackup(false);
                return true;

            case R.id.action_backup_selected:
                startBackup(true);
                return true;

            case R.id.action_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                return true;

            case R.id.action_backup_cancel:
                cancelCurrentBackup();
                return true;

            case R.id.action_about:
                showAboutDialog();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void cancelCurrentBackup() {
        if (backuper == null) {
            return;
        }
        backuper.cancel();
        backuper = null;
        unlockBackup();
    }

    private void showAboutDialog() {
        PackageManager pManager = getPackageManager();
        String pName = getPackageName();
        String vName;
        try {
            PackageInfo pInfo = pManager.getPackageInfo(pName, 0);
            vName = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            vName = "Unknown";
        }
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("About ZBackup");
        dialogBuilder.setMessage("Version " + vName);
        dialogBuilder.show();
    }

    private void startBackup(boolean onlySelected) {
        if (backupsLocked) {
            Toast.makeText(this, "Backup in already progress...", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            int currentPage = mViewPager.getCurrentItem();
            if (currentPage == LOCAL_ARRAY) {
                startLocalBackup(onlySelected);
            } else {
                startNetworkBackup(onlySelected);
            }
        }
        catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid Port Number", Toast.LENGTH_SHORT).show();
        }
        catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startLocalBackup(final boolean onlySelected) {
        final File[] externalDirs = getExternalFilesDirs(null);
        if (externalDirs == null || externalDirs.length < 2) {
            Toast.makeText(this, "External SD Card Not Found", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String backupDir = externalDirs[1].getAbsolutePath();
		char[] password = prefs.getString("zip_password", "").toCharArray();
		backuper = new LocalBackuper(
				backupItemsArrays.get(LOCAL_ARRAY),
				onlySelected, backupDir, password);
		backuper.backup(MainActivity.this);
    }

    private void startNetworkBackup(boolean onlySelected) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        backuper = new NetworkBackuper(
                backupItemsArrays.get(NETWORK_ARRAY), onlySelected,
                prefs.getString("server", "127.0.0.1"),
                Short.parseShort(prefs.getString("port", "1234")),
                prefs.getString("password", ""));
        ;
        backuper.backup(this);
    }

    public void lockBackup() {
        backupsLocked = true;
    }

    public void unlockBackup() {
        backupsLocked = false;
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    private void showNewItemPrompt() {
        openPathDialog();
    }

    private void addBackupItem(String localPath) {
        BackupItem item = new BackupItem(localPath, false);
        backupItemsArrays.get(mViewPager.getCurrentItem()).add(item);
        backupAdapters[mViewPager.getCurrentItem()].notifyDataSetChanged();
    }

    private void editBackupItem(int index, String newLocalPath) {
        BackupItem item = backupItemsArrays.get(mViewPager.getCurrentItem()).get(index);
        File f = new File(newLocalPath);
        if (!f.exists()) {
            Toast.makeText(this, "File Not Found", Toast.LENGTH_SHORT).show();
            return;
        }
        item.setLocalPath(f.getAbsolutePath());
        item.setLastBackupTime(new Date(0));
        backupAdapters[mViewPager.getCurrentItem()].notifyDataSetChanged();
    }

    private void removeBackupItem(int index) {
        backupItemsArrays.get(mViewPager.getCurrentItem()).remove(index);
        backupAdapters[mViewPager.getCurrentItem()].notifyDataSetChanged();
    }

    public void refreshAdapter(int adapterIndex) {
        backupAdapters[adapterIndex].notifyDataSetChanged();
    }

    public void openPathDialog() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(Intent.createChooser(intent, "Select File"), FILE_CHOICE_REQUEST);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!(requestCode == FILE_CHOICE_REQUEST && resultCode == RESULT_OK)) {
            return;
        }
        String newPath = (data.getData().getPath());
        addBackupItem(newPath);
    }

    public void openItemEditDialog(final int pathPosition) {
        String localPath = backupItemsArrays.get(mViewPager.getCurrentItem()).get(pathPosition).getLocalPath();
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View rootView = inflater.inflate(R.layout.path_dialog_layout, null);
        final EditText pathEdit = (EditText) rootView.findViewById(R.id.file_path_edit_text);
        pathEdit.setText(localPath);
        ColorByFileExistence(pathEdit, localPath);
        pathEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                ColorByFileExistence(pathEdit, s.toString());
            }
        });
        dialogBuilder.setTitle("Backup Path");
        dialogBuilder.setView(rootView);
        dialogBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                editBackupItem(pathPosition, pathEdit.getText().toString());
            }
        });
        dialogBuilder.setNegativeButton("Cancel", null);
        dialogBuilder.show();
    }

    public static void ColorByFileExistence(final EditText editText, String filePath) {
        File f = new File(filePath);
        if (f.exists()) {
            editText.setTextColor(Color.GREEN);
        } else {
            editText.setTextColor(Color.RED);
        }
    }

    public void openItemDeletionDialog(final int pathPosition) {
        String localPath = backupItemsArrays.get(mViewPager.getCurrentItem()).get(pathPosition).getLocalPath();
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Are you sure you want to delete this entry?");
        dialogBuilder.setMessage(localPath);
        dialogBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                removeBackupItem(pathPosition);
            }
        });
        dialogBuilder.setNegativeButton("Cancel", null);
        dialogBuilder.show();
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private BackupListAdapter[] adapters;

        public SectionsPagerAdapter(FragmentManager fm, BackupListAdapter[] adapters) {
            super(fm);
            this.adapters = adapters;
        }

        @Override
        public Fragment getItem(int position) {
            return PlaceholderFragment.newInstance(position + 1, adapters[position]);
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_local_backup).toUpperCase(l);
                case 1:
                    return getString(R.string.title_network_backup).toUpperCase(l);
            }
            return null;
        }
    }

    public static class PlaceholderFragment extends Fragment {
        private static final String ARG_SECTION_NUMBER = "section_number";

        private BackupListAdapter adapter;

        public static PlaceholderFragment newInstance(int sectionNumber, BackupListAdapter adapter) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            fragment.adapter = adapter;
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {}

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            ListView listView = (ListView)rootView.findViewById(R.id.section_list_view);
            listView.setAdapter(this.adapter);
            listView.setOnItemClickListener(this.adapter.getItemClickListener());
            listView.setOnItemLongClickListener(this.adapter.getItemLongClickListener());
            return rootView;
        }
    }
}
