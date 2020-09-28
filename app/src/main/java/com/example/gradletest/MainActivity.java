package com.example.gradletest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_start);
        Test test = new Test();
        Hello hello = new Hello();
        Book book = new Book("java", 12.34) ;
        System.out.println(book);
        String info = "Success ！！！" ;
        TextView textView = findViewById(R.id.text_info) ;
        textView.setText(info);
        Button button = findViewById(R.id.btn_submit) ;
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "略略略", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
