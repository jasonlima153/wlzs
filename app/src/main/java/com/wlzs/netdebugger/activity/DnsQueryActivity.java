package com.wlzs.netdebugger.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.wlzs.netdebugger.R;
import com.wlzs.netdebugger.adapter.LogAdapter;
import com.wlzs.netdebugger.model.LogEntry;
import com.wlzs.netdebugger.util.DnsQueryUtil;

import java.util.List;

public class DnsQueryActivity extends AppCompatActivity {

    private EditText etDomain, etDnsServer;
    private AutoCompleteTextView actvRecordType;
    private LogAdapter logAdapter;
    private DnsQueryUtil dnsQueryUtil = new DnsQueryUtil();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dns_query);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("DNS 查询");
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        etDomain = findViewById(R.id.et_domain);
        etDnsServer = findViewById(R.id.et_dns_server);
        actvRecordType = findViewById(R.id.actv_record_type);
        MaterialButton btnQuery = findViewById(R.id.btn_query);
        RecyclerView recyclerResults = findViewById(R.id.recycler_results);

        String[] types = {"A", "AAAA", "CNAME", "MX", "NS", "TXT", "SOA"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, types);
        actvRecordType.setAdapter(typeAdapter);

        recyclerResults.setLayoutManager(new LinearLayoutManager(this));
        logAdapter = new LogAdapter(this);
        recyclerResults.setAdapter(logAdapter);

        btnQuery.setOnClickListener(v -> queryDns());
    }

    private void queryDns() {
        String domain = etDomain.getText().toString().trim();
        if (domain.isEmpty()) return;

        String dnsServer = etDnsServer.getText().toString().trim();
        if (dnsServer.isEmpty()) dnsServer = "8.8.8.8";

        String typeStr = actvRecordType.getText().toString().trim();
        int type = DnsQueryUtil.getTypeCode(typeStr);

        logAdapter.clear();
        logAdapter.addEntry(new LogEntry(LogEntry.TYPE_INFO, "正在查询 " + domain + " (" + typeStr + ") 使用 " + dnsServer + "…"));

        dnsQueryUtil.query(domain, dnsServer, type, new DnsQueryUtil.DnsListener() {
            @Override
            public void onResult(List<DnsQueryUtil.DnsRecord> records) {
                if (records.isEmpty()) {
                    logAdapter.addEntry(new LogEntry(LogEntry.TYPE_WARN, "无查询结果"));
                } else {
                    logAdapter.addEntry(new LogEntry(LogEntry.TYPE_INFO, "查询结果 (" + records.size() + " 条):"));
                    for (DnsQueryUtil.DnsRecord record : records) {
                        logAdapter.addEntry(new LogEntry(LogEntry.TYPE_RECEIVE, record.toString()));
                    }
                }
            }

            @Override
            public void onError(String error) {
                logAdapter.addEntry(new LogEntry(LogEntry.TYPE_ERROR, error));
            }
        });
    }
}
