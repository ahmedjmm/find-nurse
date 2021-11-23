package com.example.findnurse.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.findnurse.R;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static com.example.findnurse.Home.firebaseAuth;

public class RegistrationActivity extends AppCompatActivity {
    static String email, password, name, age, gender, mobile;
    boolean signUpResult;

    EditText emailEditText, passwordEditText, nameEditText, ageEditText;
    TextView newMember;

    //shared preferences
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkLanguage();

        setContentView(R.layout.activity_registeration);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        editor = sharedPreferences.edit();
        (Objects.requireNonNull(getSupportActionBar())).setTitle(R.string.title);

        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        nameEditText = findViewById(R.id.name);
        ageEditText = findViewById(R.id.age);
        final Spinner spinner = findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> arrayAdapter = ArrayAdapter.createFromResource(this, R.array.spinner, android.R.layout.simple_spinner_item);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                gender = parent.getItemAtPosition(position).toString().trim();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        name = sharedPreferences.getString("name", "");
        age = sharedPreferences.getString("age", "");
        gender = sharedPreferences.getString("gender", "");
        mobile = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString("mobilNumber", "");
        if(name == "" || age == "" || gender == "" || mobile == ""){
            nameEditText.setVisibility(View.VISIBLE);
            ageEditText.setVisibility(View.VISIBLE);
            spinner.setVisibility(View.VISIBLE);
        }

        final Button button = findViewById(R.id.registeration_button);
        button.setOnClickListener(v -> {
            if (validateFields()) {
                if(checkInternet()) {
                    addInfoToGoogleSheet();
                    firebaseSignUp();
//                    editor.putString("email", email);
//                    editor.putString("password", password);
                }
            }
        });

        newMember = findViewById(R.id.new_member);
        newMember.setOnClickListener(v -> {
            View view = View.inflate(getApplicationContext(), R.layout.alert_dialog_mobile_number, null);
            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(RegistrationActivity.this);
            alertDialog.setView(view);
            final EditText editText = view.findViewById(R.id.edit_text);
            alertDialog.setTitle(R.string.enter_mobile);
            alertDialog.setPositiveButton(R.string.ok, (dialog, which) -> {
                mobile = editText.getText().toString();
                if(mobile.length() != 10)
                    Toast.makeText(getApplicationContext(), R.string.mobile_error, Toast.LENGTH_LONG).show();
                else {
                    Intent intent = new Intent(RegistrationActivity.this, CodeActivity.class);
                    intent.putExtra("mobileNumber", mobile);
                    startActivity(intent);
                    finish();
                }
            }).setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
            alertDialog.show();
            newMember.setVisibility(View.GONE);
        });
    }

    public void firebaseSignUp() {
        AuthCredential credential = EmailAuthProvider.getCredential(email, password);
        try {
            Objects.requireNonNull(firebaseAuth.getCurrentUser()).linkWithCredential(credential)
                    .addOnCompleteListener(this, task -> {
                        if(task.isSuccessful()){
                            editor.putString("email", email);
                            editor.putString("password", password);
                            editor.putString("name", name);
                            editor.putString("age", age);
                            editor.putString("gender", gender);
                            editor.commit();
                            startActivity(new Intent(RegistrationActivity.this, MapsActivity.class));
                            finish();
                        }
                    }).addOnFailureListener(e -> {
                Log.e("sign_up_error", Objects.requireNonNull(e.getMessage()));
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            });
        }
        catch (NullPointerException ignored){}
    }

    boolean validateFields() {
        email = emailEditText.getText().toString().trim();
        password = passwordEditText.getText().toString().trim();
        name = nameEditText.getText().toString().trim();
        age = ageEditText.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError(getResources().getString(R.string.edit_text_error));
            emailEditText.requestFocus();
            return false;
        }
        else if (TextUtils.isEmpty(password)) {
            passwordEditText.setError(getResources().getString(R.string.edit_text_error));
            passwordEditText.requestFocus();
            return false;
        }
        else if (TextUtils.isEmpty(name)) {
            nameEditText.setError(getResources().getString(R.string.edit_text_error));
            nameEditText.requestFocus();
            return false;
        }
        else if (TextUtils.isEmpty(age)) {
            ageEditText.setError(getResources().getString(R.string.edit_text_error));
            ageEditText.requestFocus();
            return false;
        }
        else
            return true;
    }

    private boolean checkInternet() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        try {
            if (networkInfo != null && networkInfo.isConnected())
                return true;
            else
                Toast.makeText(this, R.string.check_connection, Toast.LENGTH_LONG).show();
        } catch (NullPointerException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return false;
    }

    void addInfoToGoogleSheet() {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, "https://script.google.com/macros/s/AKfycbwnTWEg5KbXCbYjPzyDJyfsbVP7CpRyjJVJkAypmuF-9nOaXpfz/exec",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map params = new HashMap();
                params.put("name", name);
                params.put("age", age);
                params.put("gender", gender);
                return params;
            }
        };
        int socketTimeOut = 30000;  //30 seconds
        RetryPolicy retryPolicy = new DefaultRetryPolicy(socketTimeOut, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        stringRequest.setRetryPolicy(retryPolicy);
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(stringRequest);
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
        if(langCode.equals("ar"))
            language(langCode);
        else
            language("en");
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkLanguage();
    }
}