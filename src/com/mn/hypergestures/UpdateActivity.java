package com.mn.hypergestures;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
/** Compatibility entry for notifications posted by 0.4.4. */
public final class UpdateActivity extends Activity {
 @Override public void onCreate(Bundle state){super.onCreate(state);startActivity(new Intent(this,MainActivity.class));finish();}
}
