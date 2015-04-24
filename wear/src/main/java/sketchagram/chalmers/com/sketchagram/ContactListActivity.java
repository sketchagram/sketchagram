package sketchagram.chalmers.com.sketchagram;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.WatchViewStub;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Bosch on 27/02/15.
 * This is the view where you choose contacts, if ever needed then just use this
 * activity for selecting contacts.
 */
public class ContactListActivity extends Activity implements WearableListView.ClickListener,
        MessageApi.MessageListener,
        GoogleApiClient.ConnectionCallbacks  {

    private WearableListView mListView;
    private MyListAdapter mAdapter;
    private GoogleApiClient mGoogleApiClient;
    private DataMap dataMap;
    private ContactSync contacts;
    private ContactSync receivers;

    private int messageType;

    private ArrayList<String> choices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listview_stub);

        dataMap = new DataMap();
        contacts = new ContactSync();
        choices = new ArrayList<>();

        messageType = getIntent().getIntExtra("messagetype", 0);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mListView = (WearableListView) stub.findViewById(R.id.listView1);
                receivers = new ContactSync();
                messagePhone(BTCommType.GET_CONTACTS.toString(), null);     //sends a message to phone asking for contacts
                loadAdapter();

            }
        });


        //  Is needed for communication between the wearable and the device.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d("WATCH", "onConnectionFailed: " + result);
                    }
                })
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();


        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);
    }

    /**
     * Loads the adapter which holds all the items in the list, call this method if
     * the adapter should be updated.
     */
    private void loadAdapter(){
        choices = contacts.getContacts();
        mAdapter = new MyListAdapter(this, choices);
        mListView.setAdapter(mAdapter);
        mListView.setClickListener(ContactListActivity.this);
    }


    /**
     * This method will generate all the nodes that are attached to a Google Api Client.
     * Now, theoretically, only one should be: the phone. However, they return us more
     * a list. In the case where the phone happens to not be the first/only, I decided to
     * make a List of all the nodes and we'll loop through them and send each of them
     * a message. After getting the list of nodes, it sends a message to each of them telling
     * it to start. One the last successful node, it saves it as our one peerNode.
     */
    private void messagePhone(final String message, final byte[] byteMap){

        new AsyncTask<Void, Void, List<Node>>(){

            @Override
            protected List<Node> doInBackground(Void... params) {
                return getNodes();
            }

            @Override
            protected void onPostExecute(List<Node> nodeList) {
                for(Node node : nodeList) {
                    Log.e("WATCH", "......Phone: Sending Msg:  to node:  " + node.getId());
                    Log.e("WATCH", "Sending to: " + message.toString());
                    PendingResult<MessageApi.SendMessageResult> result = Wearable.MessageApi.sendMessage(
                            mGoogleApiClient,
                            node.getId(),
                            message.toString(),
                            byteMap
                    );

                    result.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            Log.e("DEVELOPER", "......Phone: " + sendMessageResult.getStatus().getStatusMessage());

                        }
                    });
                }
            }
        }.execute();

    }

    /**
     * Gets the nodes to which the wear device is connected to.
     * @return
     */
    private List<Node> getNodes() {
        List<Node> nodes = new ArrayList<Node>();
        NodeApi.GetConnectedNodesResult rawNodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        for (Node node : rawNodes.getNodes()) {
            nodes.add(node);
        }
        return nodes;
    }


    /**
     * When the device has been touched it calls this method.
     * Depending on what has been selected this does different things.
     * When the send alternative has been selected then everything is saved
     * into a DataMap which is then sent to the phone in bytes.
     * If a name has been clicked, then the name will be added to receivers.
     * @param viewHolder
     */
    @Override
    public void onClick(WearableListView.ViewHolder viewHolder) {
            Intent intent = null;
            switch(messageType) {
                case 0: intent = new Intent(this, DrawingActivity.class);
                    intent.putExtra(BTCommType.SEND_CONTACT.toString(), choices.get(viewHolder.getPosition()));
                    startActivity(intent);
                    break;
/*                case 1: intent = new Intent(this, "some emoji".class);
                    intent.putExtra(BTCommType.SEND_CONTACT.toString(), choices.get(viewHolder.getPosition()));
                    startActivity(intent);
                    break;*/
            }

    }

    @Override
    public void onTopEmptyRegionClick() {
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.v("WATCH", "connected to Google Play Services on Wear!");
        Wearable.MessageApi.addListener(mGoogleApiClient, this).setResultCallback(resultCallback);
    }

    /**
     * Not needed, but here to show capabilities. This callback occurs after the MessageApi
     * listener is added to the Google API Client.
     */
    private ResultCallback<Status> resultCallback =  new ResultCallback<Status>() {
        @Override
        public void onResult(Status status) {
            Log.v("WATCH", "Status: " + status.getStatus().isSuccess());
            new AsyncTask<Void, Void, Void>(){
                @Override
                protected Void doInBackground(Void... params) {
                    Log.e("WATCH", "doInBackground");
                    return null;
                }
            }.execute();
        }
    };

    @Override
    public void onConnectionSuspended(int i) {

    }

    /**
     * Messages from the phone are received here.
     * The phone is called earlier to send contacts to the
     * wear-device, when the phone has sent back all the
     * contacts then the adapter has to be notified and reloaded.
     * @param messageEvent
     */
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        dataMap = DataMap.fromByteArray(messageEvent.getData());
        contacts = new ContactSync(dataMap);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
                loadAdapter();
            }
        });
    }

    public class MyListAdapter extends WearableListView.Adapter {

        private final Context context;
        private final List<String> items;

        public MyListAdapter(Context context, List<String> items) {
            this.context = context;
            this.items = items;
        }
        @Override
        public WearableListView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            return new WearableListView.ViewHolder(new MyItemView(ContactListActivity.this));
        }

        @Override
        public void onBindViewHolder(WearableListView.ViewHolder viewHolder, int i) {
            MyItemView mItemView = (MyItemView) viewHolder.itemView;
            final String item = items.get(i);

            TextView txt = (TextView) mItemView.findViewById(R.id.text);
            txt.setText(item.toString());


        }

        @Override
        public int getItemCount() {
            if(items != null)
                return items.size();
            return 0;
        }
    }

    private final class MyItemView extends FrameLayout implements WearableListView.OnCenterProximityListener{

        final ImageView image;
        final TextView txtView;

        public MyItemView(Context context) {
            super(context);
            View.inflate(context, R.layout.activity_contact_view, this);
            image = (ImageView) findViewById(R.id.image);
            txtView = (TextView) findViewById(R.id.text);
        }

        @Override
        public void onCenterPosition(boolean b) {
            image.animate().scaleX(1f).scaleY(1f).alpha(1);
            txtView.animate().scaleX(1f).scaleY(1f).alpha(1);

        }

        @Override
        public void onNonCenterPosition(boolean b) {

            image.animate().scaleX(0.8f).scaleY(0.8f).alpha(0.6f);
            txtView.animate().scaleX(0.8f).scaleY(0.8f).alpha(0.6f);
        }
    }


    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //What to do if a message is received
        }
    }
}