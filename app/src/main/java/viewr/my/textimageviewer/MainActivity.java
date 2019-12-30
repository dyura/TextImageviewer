// pinable text/image viewer for the safe document display
package viewr.my.textimageviewer;

import android.app.ActivityManager;
import android.app.FragmentManager;
import android.app.KeyguardManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.pdf.PdfRenderer;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.app.Activity;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {
    private static final int OPEN_REQUEST_CODE = 41;
    private static final int READ_REQUEST_CODE = 42;
    private Button mButtonSelect;
    private Button mPin;
    private Button mRotateOn;
    private Button mRotateOff;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mButtonSelect= findViewById(R.id.button2);
        mPin= findViewById(R.id.button3);
        mRotateOn= findViewById(R.id.rotateon);
        mRotateOff= findViewById(R.id.rotateoff);

    }


    @Override
    public void onResume()
    {
        super.onResume();
        Log.d("tag", "This screen is back");
        ActivityManager activityManager;
        activityManager = (ActivityManager)
                this.getSystemService(Context.ACTIVITY_SERVICE);
        if (android.provider.Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0) == 1){
            mRotateOn.setEnabled(false);
            mRotateOff.setEnabled(true);
        } else {
            mRotateOn.setEnabled(true);
            mRotateOff.setEnabled(false);
        }

        if (activityManager.getLockTaskModeState()==0) {
//            mButtonSelect.setText("Select file");
            mButtonSelect.setEnabled(true);
            mPin.setEnabled(true);
        }
        else {
//            mButtonSelect.setText("Disabled");
            mButtonSelect.setEnabled(false);
            mPin.setEnabled(false);
        }

//        Toast.makeText(this.getApplicationContext(), "pinned???: " + Integer.toString(activityManager.getLockTaskModeState()), Toast.LENGTH_LONG).show();

    }

    public void selectFile(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        intent.setType("text/plain");
        intent.setType("*/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.
        if (requestCode == 5 && resultCode == Activity.RESULT_OK) SelectFile();

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            Uri uri;
            if (resultData != null) {
                uri = resultData.getData();
                Log.d(TAG, "Uri: " + uri.toString());
                Context context = this.getApplicationContext();
//                Toast.makeText(context, "Uri: " + uri.toString(), Toast.LENGTH_LONG).show();
                ContentResolver cr = context.getContentResolver();
                String type = cr.getType(uri);
//                Toast.makeText(context, "type: " + type, Toast.LENGTH_LONG).show();
                Log.d("type:", type);
                String fileExt = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
//                Toast.makeText(context, "fileExt: " + fileExt, Toast.LENGTH_LONG).show();

                if (type.compareTo("text/plain") == 0 || type.compareTo("image/png") == 0 ||
                        type.compareTo("image/gif") == 0 || type.compareTo("image/jpeg") == 0 || type.compareTo("video/mp4") == 0)
                    OpenHTMLViewer(uri);

                if (type.compareTo("application/pdf") == 0) {
//                    Intent viewpdf = new Intent(this, OpenPDFFile.class);
                    Intent viewpdf = new Intent(this, openPDFFileScroll.class);
                    viewpdf.putExtra("uri",uri.toString());
                    startActivity(viewpdf);
                }
            }
        }
    }

        private void OpenHTMLViewer (Uri uri){
            Log.d("text URI:", uri.toString());
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            String mimetype = mime.getMimeTypeFromExtension("txt");

            Intent htmlIntent = new Intent(Intent.ACTION_VIEW);
            htmlIntent.setData(uri);

            htmlIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);


            final ComponentName componentName = new ComponentName("com.android.htmlviewer", "com.android.htmlviewer.HTMLViewerActivity");


            htmlIntent.setComponent(componentName);
            startActivity(htmlIntent);

        }

        public void LockedSelectFile (View view){
            KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);

            if (km.isKeyguardSecure()) {
                Intent authIntent = km.createConfirmDeviceCredentialIntent(getString(R.string.dialog_title_auth), getString(R.string.dialog_msg_auth));
                startActivityForResult(authIntent, 5);
            }
         }


        private void SelectFile ()
        {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            intent.setType("*/*");
            startActivityForResult(intent, READ_REQUEST_CODE);
        }

         public void Pin (View view)
         {
             startLockTask();
         }

    public void RotateOn (View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean retVal = Settings.System.canWrite(this);
            Log.d(TAG, "Can Write Settings: " + retVal);
            if (retVal) {
                Toast.makeText(this, "Rotatong on", Toast.LENGTH_LONG).show();
                setAutoOrientationEnabled(getApplicationContext(),true);
                mRotateOn.setEnabled(false);
                mRotateOff.setEnabled(true);
            } else {
                Toast.makeText(this, "Write Settings not allowed", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                startActivity(intent);
            }

        }
    }

    public void RotateOff (View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean retVal = Settings.System.canWrite(this);
            Log.d(TAG, "Can Write Settings: " + retVal);
            if (retVal) {
                Toast.makeText(this, "Rotatong off", Toast.LENGTH_LONG).show();
                setAutoOrientationEnabled(getApplicationContext(),false);
                mRotateOn.setEnabled(true);
                mRotateOff.setEnabled(false);
            } else {
                Toast.makeText(this, "Write Settings not allowed", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                startActivity(intent);
            }
        }

    }

    public static void setAutoOrientationEnabled(Context context, boolean enabled)
    {
        Settings.System.putInt( context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, enabled ? 1 : 0);
    }

}