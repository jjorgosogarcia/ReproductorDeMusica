package com.example.sadarik.reproductordemusica;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;


public class Principal extends Activity {

    private ListView lv;
    private TextView tv;
    private SeekBar sb;
    private Cursor cursor;
    private final String AVANCEBARRA = "avanceBarra";
    private final int CTEGRABAR = 1;
    private boolean todas = false;
    private int duracion;
    private ServicioAudio sa;

    /***********************************************************/
    /********************* METODOS ON **************************/
    /**********************************************************/

    @Override
    public void onActivityResult(int requestCode, int
            resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == CTEGRABAR) {
            Uri uri = data.getData();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal);
        initComponents();
        String[] projection = { MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ALBUM_ID };
        Cursor consulta = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, null);

        lv.setAdapter(new Adaptador(this, consulta));
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                todas = false;
                cursor = (Cursor)lv.getItemAtPosition(i);
                reproducir();
            }
        });


        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                    Intent intent = new Intent(Principal.this, ServicioAudio.class);
                    intent.putExtra("posicion", seekBar.getProgress());
                    intent.setAction(ServicioAudio.POSICION);
                    startService(intent);

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.principal, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(recibo);
        stopService(new Intent(this, ServicioAudio.class));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_todo) {
            todas = true;
            cursor = (Cursor)lv.getItemAtPosition(0);
            reproducir();
            return true;
        } else if (id == R.id.action_grabar){
            intentParar();
            grabar();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(recibo, new IntentFilter(AVANCEBARRA));

    }

    /***********************************************************/
    /***************** METODOS AUXILIARES **********************/
    /**********************************************************/

    private void reproducir(){
        int ruta_cancion = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
        int titulo_cancion = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int duracion_cancion = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
        Uri uri = Uri.parse(cursor.getString(ruta_cancion));
        tv.setText(getString(R.string.sonando)+" " + cursor.getString(titulo_cancion));
        duracion = cursor.getInt(duracion_cancion);
        sb.setProgress(0);
        sb.setMax(duracion);
        //Detenemos la cancion que se estaba escuchando previamente
        intentParar();
        //Cargamos la nueva cancion
        Intent intent = new Intent(Principal.this, ServicioAudio.class);
        intent.putExtra("cancion", uri.getPath());
        intent.setAction(ServicioAudio.ADD);
        startService(intent);
        //Reproducimos la nueva cancion
        IntentPlay();


    }

    public void stop(View v){
  intentParar();
    }


    public void play(View v){
      IntentPlay();
    }

    public void pause(View v){
        Intent intent = new Intent(this, ServicioAudio.class);
        intent.setAction(ServicioAudio.PAUSE);
        startService(intent);
    }

    public void next(View v){
        if(cursor.moveToNext())
            reproducir();
        else if(cursor.moveToFirst())
            reproducir();
    }

    public void previous(View v){
        if(cursor.moveToPrevious())
            reproducir();
        else if(cursor.moveToLast())
            reproducir();
    }

    public void grabar(){
        Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
        startActivityForResult(intent, CTEGRABAR);
    }

    public void intentParar(){
        Intent intent = new Intent(Principal.this, ServicioAudio.class);
        intent.setAction(ServicioAudio.STOP);
        startService(intent);
    }

    public void IntentPlay(){
        Intent intent = new Intent(Principal.this, ServicioAudio.class);
        intent.setAction(ServicioAudio.PLAY);
        startService(intent);
    }


    public void initComponents(){
        tv = (TextView)findViewById(R.id.tvSonando);
        lv = (ListView)findViewById(R.id.listView);
        sb = (SeekBar)findViewById(R.id.seekBar);
    }

    //BroadcastReceiver
    private BroadcastReceiver recibo= new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            int posicion = bundle.getInt("ahora");
                sb.setProgress(posicion);
            if(posicion == duracion) {
                if (todas) {
                    if (cursor.moveToNext())
                        reproducir();
                    else if (cursor.moveToFirst())
                        reproducir();
                }
            }
        }
    };
}
