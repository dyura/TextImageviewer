package viewr.my.textimageviewer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

public class openPDFFileScroll extends AppCompatActivity {
    /*      openpdffilescroll                  */

//    public static final String FRAGMENT_PDF_RENDERER_BASIC = "pdf_renderer_basic";
    public static final String FRAGMENT_PDF_RENDERER_BASIC = "pdf_renderer_zoom";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.openpdffilescroll);
        Intent intent = getIntent();
        String targetPdf=getIntent().getStringExtra("uri");

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PdfRendererZoomFragment(),
                            FRAGMENT_PDF_RENDERER_BASIC)
                    .commit();
        }
    }

    @Override
    public void onStart()
    {
        super.onStart();
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

}