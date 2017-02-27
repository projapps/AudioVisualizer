package com.projapps.audiovisualizer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;

public class PolicyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_policy);
        TextView textView = (TextView) findViewById(R.id.policy_content);
        textView.setText(Html.fromHtml(getString(R.string.policy_content)));
    }
}
