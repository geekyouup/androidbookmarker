package com.geekyouup.android.bookmarker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Browser;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;


public class Bookmarker extends Activity implements OnItemClickListener, OnClickListener {
    /** Called when the activity is first created. */
    private ListView mListView;
    private BookmarkAdapter bookmarkAdapter;
    private ImageButton upButton;
    private ImageButton downButton;
    private ImageButton deleteButton;
    private ImageButton launchButton;
    private ImageButton topButton;
    private ImageButton bottomButton;
    private static final int DIALOG_YES_NO_MESSAGE = 1;
    private static final int DIALOG_WELCOME = 2;
    private int currentPos = -1;
    
    private static final String PREFS_NAME = "BookmarkerPrefs";
    private static final String LAUNCHED_KEY="LAUNCHED";
    private static final int MENU_ABOUT = 0;
    private static final int MENU_ADD = 1;
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.main);
       
       bookmarkAdapter = new BookmarkAdapter(this, "");//  new ArrayAdapter<Bookmark>(this, android.R.layout.simple_list_item_single_choice,bookmarks);
       mListView = ((ListView) findViewById(R.id.mylistview));
       mListView.setAdapter(bookmarkAdapter);
       mListView.setOnItemClickListener(this);
       
       upButton = (ImageButton) findViewById(R.id.upbutton);
       downButton = (ImageButton) findViewById(R.id.downbutton);
       deleteButton = (ImageButton) findViewById(R.id.deletebutton);
       launchButton = (ImageButton) findViewById(R.id.launchbutton);
       topButton = (ImageButton) findViewById(R.id.topbutton);
       bottomButton = (ImageButton) findViewById(R.id.bottombutton);
       
       upButton.setOnClickListener(this);
       downButton.setOnClickListener(this);
       deleteButton.setOnClickListener(this);
       launchButton.setOnClickListener(this);
       topButton.setOnClickListener(this);
       bottomButton.setOnClickListener(this);
       
       //if we have some bookmarks select the first and start everything
       if(bookmarkAdapter.getCount()>0)
       {
    	   bookmarkAdapter.initialiseAllRows();
    	   currentPos=0;
    	   bookmarkAdapter.setSelected(currentPos);
       }
       
       //Make sure the welcome message only appears on first launch
       SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
       if(settings !=null)
       {
    	   boolean launchedPreviously = settings.getBoolean(LAUNCHED_KEY, false);
    	   if(!launchedPreviously)
	       {
	    	   showDialog(DIALOG_WELCOME);
	           SharedPreferences.Editor editor = settings.edit();
	           editor.putBoolean(LAUNCHED_KEY, true);
	           editor.commit();
	       }
       }
    }

    /**
     * Invoked during init to give the Activity a chance to set up its Menu.
     * 
     * @param menu the Menu to which entries may be added
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_ADD, 0, "Add New Bookmark").setIcon(android.R.drawable.ic_menu_info_details);
        menu.add(0, MENU_ABOUT, 1, R.string.menu_about).setIcon(android.R.drawable.ic_menu_info_details);

        return true;
    }
    
    /**
     * Invoked when the user selects an item from the Menu.
     * 
     * @param item the Menu entry which was selected
     * @return true if the Menu item was legit (and we consumed it), false
     *         otherwise
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	 if(item.getItemId() == MENU_ABOUT)
    	 {
    		 showDialog(DIALOG_WELCOME);
    		 return true;
    	 }else if(item.getItemId() == MENU_ADD)
    	 {
    		 Browser.saveBookmark(this, "New Bookmark", "http://");
    		 bookmarkAdapter.notifyDataSetChanged();
    		 return true;
    	 }else return false;
    }
   
	public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
		currentPos = position;
		bookmarkAdapter.setSelected(position);
	}

	public void onClick(View v) {
	
		if(v == upButton && currentPos >0)
		{
			bookmarkAdapter.moveItemUp(currentPos);
			currentPos--;
		}else if(v== downButton && currentPos >=0 && currentPos<bookmarkAdapter.getCount()-1)
		{
			bookmarkAdapter.moveItemDown(currentPos);
			currentPos++;
		}else if(v== deleteButton)
		{
			showDialog(DIALOG_YES_NO_MESSAGE);
		}else if(v== topButton)
		{
			currentPos = bookmarkAdapter.moveToTop(currentPos);
		}else if(v== bottomButton)
		{
			currentPos = bookmarkAdapter.moveToBottom(currentPos);
		}else if(v==launchButton)
		{
			String url = bookmarkAdapter.getUrl(currentPos);
			if(url != null)
			{
		        Intent i = new Intent("android.intent.action.VIEW",Uri.parse(url));
		        startActivity(i);
			}
		}
		
	}
	
	protected Dialog onCreateDialog(int id) {
		if(id == DIALOG_YES_NO_MESSAGE)
		{
			 return new AlertDialog.Builder(Bookmarker.this)
	         	.setIcon(android.R.drawable.ic_dialog_alert)
	         	.setTitle("Confirm Delete")
	         	.setMessage("Are you sure you want to delete this bookmark?")
	         	.setPositiveButton("OK", new DialogInterface.OnClickListener() {
	             public void onClick(DialogInterface dialog, int whichButton) {
	            	 bookmarkAdapter.deleteRow(currentPos);
	             }})
	         	.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		             public void onClick(DialogInterface dialog, int whichButton) {
		            	 bookmarkAdapter.deleteRow(currentPos);
	             }})
	         .create();
		}else if(id == DIALOG_WELCOME)
		{
			String message = getString(R.string.welcome);
			if(bookmarkAdapter.getCount()<1)
			{
				message += getString(R.string.nomarks);
			}
			
			AlertDialog dialog = new AlertDialog.Builder(Bookmarker.this).create();//new AlertDialog(Bookmarker.this);
			dialog.setTitle("Welcome");
            dialog.setMessage(message);
            dialog.setButton("OK", new DialogInterface.OnClickListener() {
	             public void onClick(DialogInterface dialog, int whichButton) {
	            	if(bookmarkAdapter.getCount()<1) { finish();}
	             }
	         });
            
            dialog.setCancelable(true);
            return dialog;
		}else return null;
	}
}