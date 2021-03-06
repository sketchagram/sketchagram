package sketchagram.chalmers.com.sketchagram;

import android.app.Dialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.app.Fragment;    //v4 only used for android version 3 or lower.
import android.support.v4.widget.DrawerLayout;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;

import com.google.android.gms.wearable.DataMap;

import java.util.Observable;
import java.util.Observer;

import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import sketchagram.chalmers.com.model.IUserManager;
import sketchagram.chalmers.com.model.UserManager;
import sketchagram.chalmers.com.network.Connection;


public class MainActivity extends ActionBarActivity
        implements SendFragment.OnFragmentInteractionListener,
        ConversationFragment.OnFragmentInteractionListener,
        InConversationFragment.OnFragmentInteractionListener,
        Handler.Callback, ContactSendFragment.OnFragmentInteractionListener,
        ContactManagementFragment.OnFragmentInteractionListener,
        AddContactFragment.OnFragmentInteractionListener,
        DrawingFragment.OnFragmentInteractionListener, NavigationDrawerFragment.NavigationDrawerCallbacks, Observer {

    private final String FILENAME = "user";
    private final String MESSAGE = "message";
    private final String TAG = "Sketchagram";
    private Fragment sendFragment;
    private Fragment contactSendFragment;
    private ConversationFragment conversationFragment;
    private InConversationFragment inConversationFragment;
    private Fragment contactManagementFragment;
    private DrawingFragment drawingFragment;
    private FragmentManager fragmentManager;
    private Handler mHandler;
    private DataMap dataMap;
    private IUserManager userManager;

    private DrawerLayout mDrawerLayout;
    private NavigationDrawerFragment mNavigationDrawerFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userManager = UserManager.getInstance();

        // Check if logged in, else start LoginActivity
        sendFragment = new SendFragment();
        contactSendFragment = new ContactSendFragment();
        conversationFragment = new ConversationFragment();
        contactManagementFragment = new ContactManagementFragment();
        drawingFragment = new DrawingFragment();

        mHandler = new Handler(this);

        fragmentManager = getFragmentManager();

        /*
         * Navigation drawer
         */
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                fragmentManager.findFragmentById(R.id.navigation_drawer);

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        dataMap = new DataMap();

        //Set observer
        userManager.addUserObserver(this);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if(bundle != null) {    //Notification passed a conversationId.
            inConversationFragment = InConversationFragment.newInstance(bundle.getInt("ConversationId"));
            displayFragment(inConversationFragment);
        } else {    //Normal startup
            displayFragment(conversationFragment);
        }
     }

    public void startDrawingFragment(View v) {
        displayFragment(drawingFragment);
    }

    public void showContactSendFragment() {
        displayFragment(contactSendFragment);
    }

    public void newMessage(View view) {
        displayFragment(contactSendFragment);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        SharedPreferences preferences = getSharedPreferences(MESSAGE, 0);
        preferences.edit()
                .clear()
                .putString(MESSAGE, ":D")
                .apply();
        showContactSendFragment();
    }

    @Override
    public void onFragmentInteraction(int conversationId) {
        inConversationFragment = InConversationFragment.newInstance(conversationId);
        displayFragment(inConversationFragment);
    }

    /**
     * Change fragment displayed in the fragment frame.
     *
     * @param fragment
     */
    private void displayFragment(Fragment fragment) {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_frame, fragment)
                .addToBackStack(null).commit();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        //Logic for item selection in navigation drawer.
        Fragment fragment = null;
        switch (position) {
            case 0:
                fragment = conversationFragment;
                break;
            case 1:
                fragment = contactManagementFragment;
                break;
            case 2:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                finish();
                break;
            case 3:
                final Dialog dialog = new Dialog(this);
                dialog.setContentView(R.layout.about_dialog);
                dialog.setTitle("About");
                ((Button) dialog.findViewById(R.id.dialogButtonOK)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                dialog.show();
                break;
            case 4:
                SharedPreferences pref = getSharedPreferences(FILENAME, 0);
                SharedPreferences.Editor prefs = pref.edit();
                prefs.clear();
                prefs.apply();
                userManager.logout();
                MyApplication.getInstance().getDatabase().update();
                Intent intent2 = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent2);
                finish();
                break;
            default:
                throw new IllegalStateException("Illegal option chosen in NavigationDrawer!");
        }
        if (fragment != null) {
            displayFragment(fragment);
        }
    }

    /**
     * Displays a dialog allowing one to change the password.
     */
    public void changePassword() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.change_password_dialog);
        dialog.setTitle("Change password");
        ((Button) dialog.findViewById(R.id.change_password_dialog_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Toast toast;
                String password = ((EditText) dialog.findViewById(R.id.change_password_dialog)).getText().toString();
                if (Connection.getInstance().changePassword(password)) {
                    toast = Toast.makeText(getApplicationContext(),"Password changed.", Toast.LENGTH_LONG);
                } else {
                    toast = Toast.makeText(getApplicationContext(), "Password couldn't be changed.", Toast.LENGTH_LONG);
                }
                toast.show();
            }
        });
        ((Button) dialog.findViewById(R.id.cancel_change_password_dialog_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    public void addContact(View view) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.add_contact_dialog);
        dialog.setTitle("Add new contact");
        ((Button) dialog.findViewById(R.id.add_contact_dialog_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                String user = ((EditText) dialog.findViewById(R.id.user_name_dialog)).getText().toString();
                if (userManager.addContact(user)) {
                    Toast.makeText(getApplicationContext(), user + " added to contacts.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), user + " couldn't be added.", Toast.LENGTH_LONG).show();
                }
            }
        });
        ((Button) dialog.findViewById(R.id.cancel_dialog_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }

    @Override
    public void onBackPressed() {
        if (fragmentManager != null) {
            fragmentManager.popBackStack();
        }
    }

    @Override
    public void update (Observable observable, Object data){
        conversationFragment.updateList();
        ((ContactSendFragment)contactSendFragment).updateList();
        ((ContactManagementFragment)contactManagementFragment).update(null, null);
        if (data != null && inConversationFragment != null) {
            //Update relevant ListAdapters.
            inConversationFragment.updateList();
        }
    }

    @Override
    public void onFragmentInteraction(String id) {
        //Currently not in use but has to be implemented, as defined by a fragment.
    }
}