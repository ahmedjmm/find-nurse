package com.example.findnurse.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import com.example.findnurse.R;

import java.util.Locale;
import java.util.Objects;

public class WelcomeActivity extends AppCompatActivity {
    TextView textView;
    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkLanguage();

        setContentView(R.layout.activity_welcome);

        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.welcome_activity_title);

        textView = findViewById(R.id.text_view);
        textView.setText(R.string.welcome_activity_textView);

        button = findViewById(R.id.button);
        button.setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, RegistrationActivity.class));
            finish();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.arabic:
                PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit().putString("language", "ar").commit();
                language("ar");
                recreate();
                break;
            case R.id.english:
                PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit().putString("language", "en").commit();
                language("en");
                recreate();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app_menu, menu);
        menu.removeItem(R.id.reset_app);
        return super.onCreateOptionsMenu(menu);
    }

    public void language(String langCode){
        Resources res = getResources();
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLayoutDirection(locale);
        }
        res.updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }

    public void checkLanguage(){
        String langCode = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString("language","en" );
        if(Objects.requireNonNull(langCode).equals("ar"))
            language(langCode);
        else
            language("en");
    }
}
