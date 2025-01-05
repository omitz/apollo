package ocr;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import pp.facerecognizer.R;

public class LanguageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ocr_activity_language);

        Button ara = findViewById(R.id.ara);
        ara.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UtilsOCR.curLang = "ara";
                UtilsOCR.tessOCR = new TessOCR(UtilsOCR.curLang);
            }
        });
        Button eng = findViewById(R.id.eng);
        eng.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UtilsOCR.curLang = "eng";
                UtilsOCR.tessOCR = new TessOCR(UtilsOCR.curLang);
            }
        });
        Button fra = findViewById(R.id.fra);
        fra.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UtilsOCR.curLang = "fra";
                UtilsOCR.tessOCR = new TessOCR(UtilsOCR.curLang);
            }
        });
        Button rus = findViewById(R.id.rus);
        rus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UtilsOCR.curLang = "rus";
                UtilsOCR.tessOCR = new TessOCR(UtilsOCR.curLang);
            }
        });
    }
}
