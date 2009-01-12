package com.geekyouup.android.bookmarker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
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
    private static final int DIALOG_YES_NO_MESSAGE = 1;
    private static final int DIALOG_WELCOME = 2;
    private int currentPos = -1;
    
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
       
       upButton.setOnClickListener(this);
       downButton.setOnClickListener(this);
       deleteButton.setOnClickListener(this);
       
       //if we have some bookmarks select the first and start everything
       if(bookmarkAdapter.getCount()>0)
       {
    	   currentPos=0;
    	   bookmarkAdapter.setSelected(currentPos);
    	   
	   		upButton.setVisibility(View.VISIBLE);
			downButton.setVisibility(View.VISIBLE);
			deleteButton.setVisibility(View.VISIBLE);
       }
       
       showDialog(DIALOG_WELCOME);
    }


   
	public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
		currentPos = position;
		upButton.setVisibility(View.VISIBLE);
		downButton.setVisibility(View.VISIBLE);
		deleteButton.setVisibility(View.VISIBLE);
		bookmarkAdapter.setSelected(position);
	}

	public void onClick(View v) {

		//Toast.makeText(this, "Click: id="+v.getId() + " parent=" + v.getParent().toString(), Toast.LENGTH_SHORT).show();
		if(v == upButton && currentPos >=0)
		{
			bookmarkAdapter.moveItemUp(currentPos);
			currentPos--;
		}else if(v== downButton && currentPos >=0)
		{
			bookmarkAdapter.moveItemDown(currentPos);
			currentPos++;
		}else if(v== deleteButton)
		{
			showDialog(DIALOG_YES_NO_MESSAGE);
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
	             }
	         })
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
	            	if(bookmarkAdapter.getCount()<1) {Bookmarker.this.finish();}
	             }
	         });
            
            dialog.setCancelable(true);
            return dialog;
		}else return null;
	}

}