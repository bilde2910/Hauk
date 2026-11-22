package info.varden.hauk.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import info.varden.hauk.Constants;
import info.varden.hauk.R;
import info.varden.hauk.system.launcher.OpenLinkListener;

public final class Cve2026130162AlertActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cve2026130162_alertactivity);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void dismiss(View view) {
        SharedPreferences prefs = getSharedPreferences(Constants.SHARED_PREFS_AUTHORIZATIONS, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        finish();
    }

    public void openAdvisory(View view) {
        new OpenLinkListener(this, R.string.cve_2026_130162_link).onClick(view);
    }

}