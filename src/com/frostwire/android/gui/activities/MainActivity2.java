/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2015, FrostWire(R). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.frostwire.android.gui.activities;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.SimpleDrawerListener;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.andrew.apollo.IApolloService;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.MusicUtils.ServiceToken;
import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.SoftwareUpdater;
import com.frostwire.android.gui.SoftwareUpdater.ConfigurationUpdateListener;
import com.frostwire.android.gui.activities.internal.MainController2;
import com.frostwire.android.gui.activities.internal.MainMenuAdapter;
import com.frostwire.android.gui.activities.internal.MainMenuAdapter2;
import com.frostwire.android.gui.adnetworks.Offers;
import com.frostwire.android.gui.dialogs.NewTransferDialog;
import com.frostwire.android.gui.dialogs.TermsUseDialog;
import com.frostwire.android.gui.dialogs.YesNoDialog;
import com.frostwire.android.gui.fragments.BrowsePeerFragment;
import com.frostwire.android.gui.fragments.MainFragment;
import com.frostwire.android.gui.fragments.SearchFragment;
import com.frostwire.android.gui.fragments.TransfersFragment;
import com.frostwire.android.gui.fragments.TransfersFragment.TransferStatus;
import com.frostwire.android.gui.services.Engine;
import com.frostwire.android.gui.transfers.TransferManager;
import com.frostwire.android.gui.util.DangerousPermissionsChecker;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.AbstractActivity;
import com.frostwire.android.gui.views.AbstractDialog;
import com.frostwire.android.gui.views.AbstractDialog.OnDialogClickListener;
import com.frostwire.android.gui.views.PlayerMenuItemView;
import com.frostwire.android.gui.views.TimerObserver;
import com.frostwire.android.gui.views.TimerService;
import com.frostwire.android.gui.views.TimerSubscription;
import com.frostwire.logging.Logger;
import com.frostwire.util.Ref;
import com.frostwire.util.StringUtils;
import com.frostwire.uxstats.UXAction;
import com.frostwire.uxstats.UXStats;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import static com.andrew.apollo.utils.MusicUtils.mService;

/**
 * @author gubatron
 * @author aldenml
 */
public class MainActivity2 extends AbstractActivity implements ConfigurationUpdateListener,
        OnDialogClickListener,
        ServiceConnection,
        ActivityCompat.OnRequestPermissionsResultCallback,
        DangerousPermissionsChecker.PermissionsCheckerHolder {

    private static final Logger LOG = Logger.getLogger(MainActivity2.class);
    private static final String FRAGMENTS_STACK_KEY = "fragments_stack";
    private static final String CURRENT_FRAGMENT_KEY = "current_fragment";
    private static final String LAST_BACK_DIALOG_ID = "last_back_dialog";
    private static final String SHUTDOWN_DIALOG_ID = "shutdown_dialog";
    private static boolean firstTime = true;
    private final Map<Integer, DangerousPermissionsChecker> permissionsCheckers;
    private MainController2 controller2;
    private DrawerLayout drawerLayout;
    @SuppressWarnings("deprecation")
    private ActionBarDrawerToggle drawerToggle;
    private View leftDrawer;
    private ListView listMenu;
    private BrowsePeerFragment library;
    private Fragment currentFragment;
    private final Stack<Integer> fragmentsStack;
    private PlayerMenuItemView playerItem;
    private TimerSubscription playerSubscription;
    private BroadcastReceiver mainBroadcastReceiver;
    private boolean externalStoragePermissionsRequested = false;

    public MainActivity2() {
        super(R.layout.activity_main2);
        this.controller2 = new MainController2(this);
        this.fragmentsStack = new Stack<>();
        this.permissionsCheckers = initPermissionsCheckers();
    }

    @Override
    public void onBackPressed() {
        if (fragmentsStack.size() > 1) {
            try {
                fragmentsStack.pop();
                int id = fragmentsStack.peek();
                Fragment fragment = getFragmentManager().findFragmentById(id);
                switchContent(fragment, false);
            } catch (Throwable e) {
                // don't break the app
                showLastBackDialog();
            }
        } else {
            showLastBackDialog();
        }

        syncSlideMenu();
        updateHeader(getCurrentFragment());
    }

    public void onConfigurationUpdate() {
        setupMenuItems();
    }

    public void shutdown() {
        Offers.stopAdNetworks(this);
        UXStats.instance().flush(true); // sends data and ends 3rd party APIs sessions.
        finish();
        Engine.instance().shutdown();
    }

    private boolean isShutdown() {
        Intent intent = getIntent();
        boolean result = intent != null && intent.getBooleanExtra("shutdown-" + ConfigurationManager.instance().getUUIDString(), false);
        if (result) {
            shutdown();
        }
        return result;
    }

    @Override
    protected void initComponents(Bundle savedInstanceState) {
        if (isShutdown()) {
            return;
        }
        initDrawerListener();
        leftDrawer = findView(R.id.activity_main_left_drawer);
        listMenu = findView(R.id.left_drawer);
        initPlayerItemListener();
        setupFragments();
        setupMenuItems();
        setupInitialFragment(savedInstanceState);
        playerSubscription = TimerService.subscribe((TimerObserver) findView(R.id.activity_main_player_notifier), 1);
        onNewIntent(getIntent());
        SoftwareUpdater.instance().addConfigurationUpdateListener(this);
        setupActionBar();
        setupDrawer();
    }

    private void initPlayerItemListener() {
        playerItem = findView(R.id.slidemenu_player_menuitem);
        playerItem.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                controller2.launchPlayerActivity();
            }
        });
    }

    private void initDrawerListener() {
        drawerLayout = findView(R.id.drawer_layout);
        drawerLayout.setDrawerListener(new SimpleDrawerListener() {
            @Override
            public void onDrawerStateChanged(int newState) {
                refreshPlayerItem();
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                syncSlideMenu();
            }

            @Override
            public void onDrawerClosed(View drawerView) {

            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();

        initDrawerListener();
        setupDrawer();
        initPlayerItemListener();

        refreshPlayerItem();

        if (ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_TOS_ACCEPTED)) {
            if (ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_INITIAL_SETTINGS_COMPLETE)) {
                mainResume();
                Offers.initAdNetworks(this);
            } else {
                controller2.startWizardActivity();
            }
        }
//        } else {
//            TermsUseDialog dlg = new TermsUseDialog();
//            dlg.show(getFragmentManager());
//        }

        checkLastSeenVersion();
        syncSlideMenu();

        //uncomment to test social links dialog
        //UIUtils.showSocialLinksDialog(this, true, null, "");

        if (ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_TOS_ACCEPTED)) {
            checkExternalStoragePermissionsOrBindMusicService();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mainBroadcastReceiver != null) {
            try {
                unregisterReceiver(mainBroadcastReceiver);
            } catch (Throwable ignored) {
                //oh well (the api doesn't provide a way to know if it's been registered before,
                //seems like overkill keeping track of these ourselves.)
            }
        }
    }

    private Map<Integer, DangerousPermissionsChecker> initPermissionsCheckers() {
        Map<Integer, DangerousPermissionsChecker> checkers = new HashMap<>();

        // EXTERNAL STORAGE ACCESS CHECKER.
        final DangerousPermissionsChecker externalStorageChecker =
                new DangerousPermissionsChecker(this, DangerousPermissionsChecker.EXTERNAL_STORAGE_PERMISSIONS_REQUEST_CODE);
        externalStorageChecker.setPermissionsGrantedCallback(new DangerousPermissionsChecker.OnPermissionsGrantedCallback() {
            @Override
            public void onPermissionsGranted() {
                UIUtils.showInformationDialog(MainActivity2.this,
                        R.string.restarting_summary,
                        R.string.restarting,
                        false,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                externalStorageChecker.restartFrostWire(1000);
                            }
                        });
            }
        });
        checkers.put(DangerousPermissionsChecker.EXTERNAL_STORAGE_PERMISSIONS_REQUEST_CODE, externalStorageChecker);

        // add more permissions checkers if needed...

        return checkers;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (outState != null) {
            super.onSaveInstanceState(outState);
            saveLastFragment(outState);
            saveFragmentsStack(outState);
        }
    }

    private ServiceToken mToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_TOS_ACCEPTED)) {
            // we are still in the wizard.
            return;
        }

        if (isShutdown()) {
            return;
        }

        checkExternalStoragePermissionsOrBindMusicService();
    }

    private void checkExternalStoragePermissionsOrBindMusicService() {
        DangerousPermissionsChecker checker = permissionsCheckers.get(DangerousPermissionsChecker.EXTERNAL_STORAGE_PERMISSIONS_REQUEST_CODE);
        if (!externalStoragePermissionsRequested && checker != null && checker.noAccess()) {
            checker.requestPermissions();
            externalStoragePermissionsRequested = true;
        } else if (mToken == null && checker != null && !checker.noAccess()) {
            mToken = MusicUtils.bindToService(this, this);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (playerSubscription != null) {
            playerSubscription.unsubscribe();
        }

        //avoid memory leaks when the device is tilted and the menu gets recreated.
        SoftwareUpdater.instance().removeConfigurationUpdateListener(this);

        if (playerItem != null) {
            playerItem.unbindDrawables();
        }

        if (mToken != null) {
            MusicUtils.unbindFromService(mToken);
            mToken = null;
        }
    }

    private void saveLastFragment(Bundle outState) {
        Fragment fragment = getCurrentFragment();
        if (fragment != null) {
            getFragmentManager().putFragment(outState, CURRENT_FRAGMENT_KEY, fragment);
        }
    }

    private void mainResume() {
        syncSlideMenu();
        if (firstTime) {
            firstTime = false;
            Engine.instance().startServices(); // it's necessary for the first time after wizard
        }
        SoftwareUpdater.instance().checkForUpdate(this);
    }

    private void checkLastSeenVersion() {
        final String lastSeenVersion = ConfigurationManager.instance().getString(Constants.PREF_KEY_CORE_LAST_SEEN_VERSION);
        if (StringUtils.isNullOrEmpty(lastSeenVersion)) {
            //fresh install
            ConfigurationManager.instance().setString(Constants.PREF_KEY_CORE_LAST_SEEN_VERSION, Constants.FROSTWIRE_VERSION_STRING);
            UXStats.instance().log(UXAction.CONFIGURATION_WIZARD_FIRST_TIME);
        } else if (!Constants.FROSTWIRE_VERSION_STRING.equals(lastSeenVersion)) {
            //just updated.
            ConfigurationManager.instance().setString(Constants.PREF_KEY_CORE_LAST_SEEN_VERSION, Constants.FROSTWIRE_VERSION_STRING);
            UXStats.instance().log(UXAction.CONFIGURATION_WIZARD_AFTER_UPDATE);
        }
    }


    private void showLastBackDialog() {
        YesNoDialog dlg = YesNoDialog.newInstance(LAST_BACK_DIALOG_ID, R.string.minimize_frostwire, R.string.are_you_sure_you_wanna_leave);
        dlg.show(getFragmentManager()); //see onDialogClick
    }

    private void showShutdownDialog() {
        YesNoDialog dlg = YesNoDialog.newInstance(SHUTDOWN_DIALOG_ID, R.string.app_shutdown_dlg_title, R.string.app_shutdown_dlg_message);
        dlg.show(getFragmentManager()); //see onDialogClick
    }

    public void onDialogClick(String tag, int which) {
        if (tag.equals(LAST_BACK_DIALOG_ID) && which == AbstractDialog.BUTTON_POSITIVE) {
            onLastDialogButtonPositive();
        } else if (tag.equals(SHUTDOWN_DIALOG_ID) && which == AbstractDialog.BUTTON_POSITIVE) {
            onShutdownDialogButtonPositive();
        } else if (tag.equals(TermsUseDialog.TAG)) {
            controller2.startWizardActivity();
        }
    }

    private void onLastDialogButtonPositive() {
//        Offers.showInterstitial(this, false, true);
        finish();
    }

    private void onShutdownDialogButtonPositive() {
//        Offers.showInterstitial(this, true, false);
        finish();
    }

    private void syncSlideMenu() {
        listMenu.clearChoices();
        invalidateOptionsMenu();

        int menuId = R.id.menu_main_library;
        setCheckedItem(menuId);
        updateHeader(getCurrentFragment());
    }

    private void setCheckedItem(int id) {
        try {
            listMenu.clearChoices();
            ((MainMenuAdapter2) listMenu.getAdapter()).notifyDataSetChanged();

            int position = 0;
            MainMenuAdapter2 adapter = (MainMenuAdapter2) listMenu.getAdapter();
            for (int i = 0; i < adapter.getCount(); i++) {
                listMenu.setItemChecked(i, false);
                if (adapter.getItemId(i) == id) {
                    position = i;
                    break;
                }
            }

            if (id != -1) {
                listMenu.setItemChecked(position, true);
            }

            invalidateOptionsMenu();

            if (drawerToggle != null) {
                drawerToggle.syncState();
            }
        } catch (Throwable e) { // protecting from weird android UI engine issues
            LOG.warn("Error setting slide menu item selected", e);
        }
    }

    private void refreshPlayerItem() {
        if (playerItem != null) {
            playerItem.refresh();
        }
    }

    private void setupMenuItems() {
        listMenu.setAdapter(new MainMenuAdapter2(this));
        listMenu.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        listMenu.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                syncSlideMenu();
                controller2.closeSlideMenu();
                try {
                    if (id == R.id.menu_main_settings) {
                        controller2.showPreferences();
                    } else if (id == R.id.menu_main_shutdown) {
                        showShutdownDialog();
                    } else if (id == R.id.menu_main_my_music) {
                        controller2.launchMyMusic();
                    } else if (id == R.id.menu_main_support) {
                        UIUtils.openURL(MainActivity2.this, Constants.SUPPORT_URL);
                    } else {
                        listMenu.setItemChecked(position, true);
                        controller2.switchFragment((int) id);
                    }
                } catch (Throwable e) { // protecting from weird android UI engine issues
                    LOG.error("Error clicking slide menu item", e);
                }
            }
        });
    }

    private void setupFragments() {
        library = (BrowsePeerFragment) getFragmentManager().findFragmentById(R.id.activity_main_fragment_browse_peer);
    }


    private void setupInitialFragment(Bundle savedInstanceState) {
        Fragment fragment = null;

        if (savedInstanceState != null) {
            fragment = getFragmentManager().getFragment(savedInstanceState, CURRENT_FRAGMENT_KEY);
            restoreFragmentsStack(savedInstanceState);
        }
        if (fragment == null) {
            fragment = library;
            setCheckedItem(R.id.menu_main_library);
        }

        switchContent(fragment);
    }

    private void saveFragmentsStack(Bundle outState) {
        int[] stack = new int[fragmentsStack.size()];
        for (int i = 0; i < stack.length; i++) {
            stack[i] = fragmentsStack.get(i);
        }
        outState.putIntArray(FRAGMENTS_STACK_KEY, stack);
    }

    private void restoreFragmentsStack(Bundle savedInstanceState) {
        try {
            int[] stack = savedInstanceState.getIntArray(FRAGMENTS_STACK_KEY);
            for (int id : stack) {
                fragmentsStack.push(id);
            }
        } catch (Throwable ignored) {
        }
    }

    private void updateHeader(Fragment fragment) {
        try {
            RelativeLayout placeholder = (RelativeLayout) getActionBar().getCustomView();
            if (placeholder != null && placeholder.getChildCount() > 0) {
                placeholder.removeAllViews();
            }

            if (fragment instanceof MainFragment) {
                View header = ((MainFragment) fragment).getHeader(this);
                if (placeholder != null && header != null) {
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                    params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                    placeholder.addView(header, params);
                }
            }
        } catch (Throwable e) {
            LOG.error("Error updating main header", e);
        }
    }

    private void switchContent(Fragment fragment, boolean addToStack) {
//        hideFragments(getFragmentManager().beginTransaction()).show(fragment).commitAllowingStateLoss();
        if (addToStack && (fragmentsStack.isEmpty() || fragmentsStack.peek() != fragment.getId())) {
            fragmentsStack.push(fragment.getId());
        }
        currentFragment = fragment;
        updateHeader(fragment);
    }

    /*
     * The following methods are only public to be able to use them from another package(internal).
     */

    public Fragment getFragmentByMenuId(int id) {
        switch (id) {
            case R.id.menu_main_library:
                return library;
            default:
                return null;
        }
    }

    public void switchContent(Fragment fragment) {
        switchContent(fragment, true);
    }

    public Fragment getCurrentFragment() {
        return currentFragment;
    }

    public void closeSlideMenu() {
        drawerLayout.closeDrawer(leftDrawer);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    private void setupActionBar() {
        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setCustomView(R.layout.view_custom_actionbar);
            bar.setDisplayShowCustomEnabled(true);
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setHomeButtonEnabled(true);
        }
    }

    private void setupDrawer() {
        drawerToggle = new MenuDrawerToggle(this, drawerLayout);
        drawerLayout.setDrawerListener(drawerToggle);
    }

    public void onServiceConnected(final ComponentName name, final IBinder service) {
        mService = IApolloService.Stub.asInterface(service);
    }

    /**
     * {@inheritDoc}
     */
    public void onServiceDisconnected(final ComponentName name) {
        mService = null;
    }

    public DangerousPermissionsChecker getPermissionsChecker(int requestCode) {
        return permissionsCheckers.get(requestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        DangerousPermissionsChecker checker = permissionsCheckers.get(requestCode);
        if (checker != null) {
            checker.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private static final class MenuDrawerToggle extends ActionBarDrawerToggle {
        private final WeakReference<MainActivity2> activityRef;

        public MenuDrawerToggle(MainActivity2 activity, DrawerLayout drawerLayout) {
            super(activity, drawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close);

            // aldenml: even if the parent class holds a strong reference, I decided to keep a weak one
            this.activityRef = Ref.weak(activity);
        }

        @Override
        public void onDrawerClosed(View view) {
            if (Ref.alive(activityRef)) {
                activityRef.get().invalidateOptionsMenu();
                activityRef.get().syncSlideMenu();
            }
        }

        @Override
        public void onDrawerOpened(View drawerView) {
            if (Ref.alive(activityRef)) {
                UIUtils.hideKeyboardFromActivity(activityRef.get());
                activityRef.get().invalidateOptionsMenu();
                activityRef.get().syncSlideMenu();
            }
        }

        @Override
        public void onDrawerStateChanged(int newState) {
            if (Ref.alive(activityRef)) {
                MainActivity2 activity = activityRef.get();
                activity.refreshPlayerItem();
                activity.syncSlideMenu();
            }
        }
    }

}
