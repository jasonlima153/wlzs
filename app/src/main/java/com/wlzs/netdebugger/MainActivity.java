package com.wlzs.netdebugger;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.wlzs.netdebugger.fragment.CommFragment;
import com.wlzs.netdebugger.fragment.HomeFragment;
import com.wlzs.netdebugger.fragment.SettingsFragment;
import com.wlzs.netdebugger.fragment.ToolsFragment;

public class MainActivity extends AppCompatActivity {

    private FragmentManager fragmentManager;
    private Fragment currentFragment;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fragmentManager = getSupportFragmentManager();
        bottomNav = findViewById(R.id.bottom_nav);

        if (savedInstanceState == null) {
            switchFragment(new HomeFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                switchFragment(new HomeFragment());
                return true;
            } else if (id == R.id.nav_tools) {
                switchFragment(new ToolsFragment());
                return true;
            } else if (id == R.id.nav_comm) {
                switchFragment(new CommFragment());
                return true;
            } else if (id == R.id.nav_settings) {
                switchFragment(new SettingsFragment());
                return true;
            }
            return false;
        });
    }

    private void switchFragment(Fragment fragment) {
        if (currentFragment != null && currentFragment.getClass().equals(fragment.getClass())) {
            return;
        }
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (currentFragment != null) {
            transaction.hide(currentFragment);
        }
        Fragment existing = fragmentManager.findFragmentByTag(fragment.getClass().getName());
        if (existing != null) {
            transaction.show(existing);
            currentFragment = existing;
        } else {
            transaction.add(R.id.fragment_container, fragment, fragment.getClass().getName());
            currentFragment = fragment;
        }
        transaction.commitAllowingStateLoss();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
