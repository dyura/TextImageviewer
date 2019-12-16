package viewr.my.textimageviewer;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;

public class OpenPDFFile extends AppCompatActivity {
    String targetPdf = "storage/emulated/0/Download/BackFlightBooking.pdf";

    ImageView pdfView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.openpdffile);

        pdfView = (ImageView)findViewById(R.id.pdfview);
        Intent intent = getIntent();
        targetPdf=getIntent().getStringExtra("uri");

        try {
            openPDF();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this,
                    "Something Wrong: " + e.toString(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void openPDF() throws IOException {

        Uri uri2 = Uri.parse(targetPdf) ;

        ParcelFileDescriptor fileDescriptor = null;

        try {
            fileDescriptor= this.getContentResolver().
                    openFileDescriptor(uri2,"r");
        } catch (Exception e) {
            Log.d("Exception:", e.getMessage());
        }

        //min. API Level 21
        PdfRenderer pdfRenderer = null;
        pdfRenderer = new PdfRenderer(fileDescriptor);

        final int pageCount = pdfRenderer.getPageCount();
        Toast.makeText(this,
                "pageCount = " + pageCount,
                Toast.LENGTH_LONG).show();

        //Display page 0
        PdfRenderer.Page rendererPage = pdfRenderer.openPage(0);
        int rendererPageWidth = rendererPage.getWidth();
        int rendererPageHeight = rendererPage.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(
                rendererPageWidth,
                rendererPageHeight,
                Bitmap.Config.ARGB_8888);
        try {
        rendererPage.render(bitmap, null, null,
                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        } catch (Exception e) {
            Log.d("Exception:", e.getMessage());
        }

        try {
        pdfView.setImageBitmap(bitmap);
        } catch (Exception e) {
            Log.d("Exception:", e.getMessage());
        }

        rendererPage.close();

        pdfRenderer.close();
        fileDescriptor.close();
    }
}

