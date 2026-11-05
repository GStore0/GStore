package com.kangrio.extension;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class AccountManagerActivity extends Activity {
    private static final String ACCOUNT_TYPE = "com.google";

    private AccountManager accountManager;
    private LinearLayout accountList;
    private TextView emptyView;
    private TextView countView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        accountManager = AccountManager.get(this);

        createLayout();
        refreshAccounts();
    }

    private void createLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 20, 20, 20);

        TextView title = new TextView(this);
        title.setText("Account Manager");
        title.setTextSize(24);
        title.setPadding(0, 0, 0, 12);
        root.addView(title);

        countView = new TextView(this);
        countView.setTextSize(14);
        root.addView(countView);

        Button refreshButton = new Button(this);
        refreshButton.setText("Refresh");
        refreshButton.setOnClickListener(v -> refreshAccounts());
        root.addView(refreshButton);

        Button removeAllButton = new Button(this);
        removeAllButton.setText("Remove All Accounts");
        removeAllButton.setOnClickListener(v -> confirmRemoveAll());
        root.addView(removeAllButton);

        ScrollView scrollView = new ScrollView(this);

        accountList = new LinearLayout(this);
        accountList.setOrientation(LinearLayout.VERTICAL);

        emptyView = new TextView(this);
        emptyView.setText("No accounts found.");
        emptyView.setTextSize(16);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setPadding(10, 30, 10, 30);

        scrollView.addView(accountList);
        root.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(root);
    }

    private void refreshAccounts() {
        accountList.removeAllViews();
        Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE);

        countView.setText("Accounts: " + accounts.length);
        if (accounts.length == 0) {
            accountList.addView(emptyView);
            return;
        }

        for (final Account account : accounts) {
            addAccountView(account);
        }
    }

    private void addAccountView(final Account account) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(16, 12, 16, 12);

        TextView name = new TextView(this);
        name.setText("Name: " + account.name);
        name.setTextSize(16);

        TextView type = new TextView(this);
        type.setText("Type: " + account.type);
        type.setTextSize(13);

        Button removeButton = new Button(this);
        removeButton.setText("Remove Account");

        removeButton.setOnClickListener(v -> confirmRemove(account));

        card.addView(name);
        card.addView(type);
        card.addView(removeButton);

        View divider = new View(this);
        divider.setBackgroundColor(0xFFCCCCCC);

        accountList.addView(card);
        accountList.addView(divider,
                new LinearLayout.LayoutParams(-1, 1));
    }

    private void confirmRemove(final Account account) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Account")
                .setMessage("Remove account \"" + account.name + "\"?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Remove",
                        (dialog, which) -> removeAccount(account))
                .show();
    }

    private void confirmRemoveAll() {
        new AlertDialog.Builder(this)
                .setTitle("Remove All Accounts")
                .setMessage("Are you sure you want to remove all accounts?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Remove All", (dialog, which) -> {
                    Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE);
                    int removed = 0;
                    for (Account account : accounts) {
                        if (accountManager.removeAccountExplicitly(account)) {
                            removed++;
                        }
                    }
                    Toast.makeText(this, "Removed " + removed + " accounts", Toast.LENGTH_SHORT).show();
                    refreshAccounts();
                }).show();
    }

    private void removeAccount(Account account) {
        boolean removed = accountManager.removeAccountExplicitly(account);
        Toast.makeText(this, removed ? "Account removed" : "Failed to remove account", Toast.LENGTH_SHORT).show();
        refreshAccounts();
    }
}
