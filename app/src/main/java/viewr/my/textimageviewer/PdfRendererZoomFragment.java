package viewr.my.textimageviewer;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class PdfRendererZoomFragment extends Fragment implements View.OnClickListener {
    private String targetPdf;

    private static final String STATE_CURRENT_PAGE_INDEX = "current_page_index";
    /**
     * The filename of the PDF.
     */
    public String FILENAME;
    public String PURCHASE_ID;
    public int TICKETS_NUMBER;
    public Uri uri;

    /**
     * File descriptor of the PDF.
     */
    private ParcelFileDescriptor mFileDescriptor;

    /**
     * {@link android.graphics.pdf.PdfRenderer} to render the PDF.
     */
    private PdfRenderer mPdfRenderer;

    /**
     * Page that is currently shown on the screen.
     */
    private PdfRenderer.Page mCurrentPage;

    /**
     * {@link android.widget.ImageView} that shows a PDF page as a {@link android.graphics.Bitmap}
     */
    private ImageView mImageView;

    /**
     * {@link android.widget.Button} to move to the previous page.
     */
    private Button mButtonPrevious;
    private ImageView mButtonZoomin;
    private ImageView mButtonZoomout;
    private Button mButtonNext;
    private EditText mEdit;
    private TextView mTextView;  // textView
    private float currentZoomLevel = 6;

    /**
     * PDF page index
     */
    private int mPageIndex;

    public PdfRendererZoomFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.pdfrendererzoomfragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Retain view references.
        mImageView = (ImageView) view.findViewById(R.id.image);
        mButtonPrevious = (Button) view.findViewById(R.id.previous);
        mButtonNext = (Button) view.findViewById(R.id.next);
        mEdit = (EditText) view.findViewById(R.id.pageNumber);
        mTextView = (TextView) view.findViewById(R.id.textView);
        mButtonZoomin = view.findViewById(R.id.zoomin);
        mButtonZoomout = view.findViewById(R.id.zoomout);

        // Bind events.
        mButtonPrevious.setOnClickListener(this);
        mButtonNext.setOnClickListener(this);
        mButtonZoomin.setOnClickListener(this);
        mButtonZoomout.setOnClickListener(this);

        mPageIndex = 0;
        // If there is a savedInstanceState (screen orientations, etc.), we restore the page index.
        if (null != savedInstanceState) {
            mPageIndex = savedInstanceState.getInt(STATE_CURRENT_PAGE_INDEX, 0);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        targetPdf=getActivity().getIntent().getStringExtra("uri");  // ?????
        uri = Uri.parse(targetPdf) ;

        FILENAME = new File(uri.getPath()).toString();

        mEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ( (actionId == EditorInfo.IME_ACTION_DONE) || ((event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN ))) {

                    // Do stuff when user presses enter
                    if (Integer.parseInt(mEdit.getText().toString())>0)
                        {
                            int page=Integer.parseInt(mEdit.getText().toString());
                        showPage(page-1);
                        }

                }

                return false;
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            openRenderer(getActivity());
            showPage(mPageIndex);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "file is not found", Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }
    }

    @Override
    public void onStop() {
        try {
            closeRenderer();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (null != mCurrentPage) {
            outState.putInt(STATE_CURRENT_PAGE_INDEX, mCurrentPage.getIndex());
        }
    }

    /**
     * Sets up a {@link android.graphics.pdf.PdfRenderer} and related resources.
     */
    private void openRenderer(Context context) throws IOException {
        ContentResolver resolver = getActivity().getContentResolver();
        mFileDescriptor = resolver.
                openFileDescriptor(uri,"r");
        // This is the PdfRenderer we use to render the PDF.
        if (mFileDescriptor != null) {
            mPdfRenderer = new PdfRenderer(mFileDescriptor);
            String pref="";
            if (mPdfRenderer.getPageCount()<10)
                pref="    ";
            else if (mPdfRenderer.getPageCount()<100)
                pref="  ";
            else if (mPdfRenderer.getPageCount()<1000)
                pref=" ";

            String str=pref+mPdfRenderer.getPageCount()+"\npages";
            mTextView.setText(str);
        }
    }

    /**
     * Closes the {@link android.graphics.pdf.PdfRenderer} and related resources.
     *
     * @throws java.io.IOException When the PDF file cannot be closed.
     */
    private void closeRenderer() throws IOException {
        if (null != mCurrentPage) {
            mCurrentPage.close();
            mCurrentPage = null;
        }
        if (null != mPdfRenderer) {
            mPdfRenderer.close();
        }
        if (null != mFileDescriptor) {
            mFileDescriptor.close();
        }
    }

    /**
     * Zoom level for zoom matrix depends on screen density (dpiAdjustedZoomLevel), but width and height of bitmap depends only on pixel size and don't depend on DPI
     * Shows the specified page of PDF to the screen.
     *
     * @param index The page index.
     */
    public void showPage(int index) {
        if (mPdfRenderer.getPageCount() <= index) {
            return;
        }
        // Make sure to close the current page before opening another one.
        if (null != mCurrentPage) {
            mCurrentPage.close();
        }
        mEdit.setText(Integer.toString(index+1));
        // Use `openPage` to open a specific page in PDF.
        mCurrentPage = mPdfRenderer.openPage(index);
        // Important: the destination bitmap must be ARGB (not RGB).
        int newWidth;
        int newHeight;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT  ) {
             newWidth = (int) (getResources().getDisplayMetrics().widthPixels * mCurrentPage.getWidth() / 72 * currentZoomLevel / 40);
             newHeight = (int) (getResources().getDisplayMetrics().heightPixels * mCurrentPage.getHeight() / 72 * currentZoomLevel / 64);
        }
        else
        {
            newWidth = (int) (getResources().getDisplayMetrics().widthPixels * mCurrentPage.getWidth() / 72 * currentZoomLevel / 64);
            newHeight = (int) (getResources().getDisplayMetrics().heightPixels * mCurrentPage.getHeight() / 72 * currentZoomLevel /40 );

        }
        Bitmap bitmap = Bitmap.createBitmap(
                newWidth,
                newHeight,
                Bitmap.Config.ARGB_8888);
        Matrix matrix = new Matrix();

        float dpiAdjustedZoomLevel = currentZoomLevel * DisplayMetrics.DENSITY_MEDIUM / getResources().getDisplayMetrics().densityDpi;
        matrix.setScale(dpiAdjustedZoomLevel, dpiAdjustedZoomLevel);

        // Here, we render the page onto the Bitmap.
        // To render a portion of the page, use the second and third parameter. Pass nulls to get
        // the default result.
        // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
        mCurrentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY); // just testing

        // We are ready to show the Bitmap to user.
        mImageView.setImageBitmap(bitmap);
        updateUi();
    }

    /**
     * Updates the state of 2 control buttons in response to the current page index.
     */
    private void updateUi() {
        int index = mCurrentPage.getIndex();
        int pageCount = mPdfRenderer.getPageCount();
        if (pageCount == 1) {
            mButtonPrevious.setVisibility(View.GONE);
            mButtonNext.setVisibility(View.GONE);
        } else {
            mButtonPrevious.setEnabled(0 != index);
            mButtonNext.setEnabled(index + 1 < pageCount);
        }
        if (currentZoomLevel == 2) {
            mButtonZoomout.setActivated(false);
        } else {
            mButtonZoomout.setActivated(true);
        }
//        enableButtons();
    }

    /**
     * Gets the number of pages in the PDF. This method is marked as public for testing.
     *
     * @return The number of pages.
     */
    public int getPageCount() {
        return mPdfRenderer.getPageCount();
    }

    @Override
    public void onClick(View view) {
        Log.d("currentZoomLevel:", Float.toString(currentZoomLevel));
//        disableButtons();
        switch (view.getId()) {
            case R.id.previous: {
                // Move to the previous page
//                currentZoomLevel = 12;
                showPage(mCurrentPage.getIndex() - 1);
                break;
            }
            case R.id.next: {
                // Move to the next page
//                currentZoomLevel = 12;
                showPage(mCurrentPage.getIndex() + 1);
                break;
            }
            case R.id.zoomout: {
                // Move to the next page
                if (currentZoomLevel >=4)
                --currentZoomLevel;
                showPage(mCurrentPage.getIndex());
                break;
            }
            case R.id.zoomin: {
                // Move to the next page
                if (currentZoomLevel <=18)
                    ++currentZoomLevel;
                showPage(mCurrentPage.getIndex());
                break;
            }
        }
    }

}

