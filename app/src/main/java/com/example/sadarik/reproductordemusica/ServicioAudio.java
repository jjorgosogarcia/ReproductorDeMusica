package com.example.sadarik.reproductordemusica;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;

import java.io.IOException;

public class ServicioAudio extends Service implements MediaPlayer.OnPreparedListener,MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener{

    private MediaPlayer mp;
    private enum Estados{
        idle,
        initialized,
        preparing,
        prepared,
        started,
        paused,
        completed,
        stopped,
        end,
        error
    };
    private Estados estado;
    public final static String PLAY = "play", STOP = "stop", ADD = "add", PAUSE = "pause", POSICION="posicion";
    private String rutaCancion = null;
    private final String AVANCEBARRA = "avanceBarra";
    private boolean reproducir;
    private BarraCancion barra;
    private static final int NOTIFICACION=1;

     /***********************************************************/
     /********************** METODO CONSTRUCTOR ****************/
    /**********************************************************/

    public ServicioAudio() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        int r = am.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if(r==AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
            mp = new MediaPlayer();
            mp.setOnPreparedListener(this);
            mp.setOnCompletionListener(this);
            mp.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
            estado = estado.idle;
        }else{
            //
        }
        estado = Estados.idle;
    }

    /***********************************************************/
    /*******************METODOS SOBREESCRITOS  ****************/
    /**********************************************************/

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action.equals(PLAY)) {
            play();
        }
        else if (action.equals(PAUSE)) {
            pause();
        } else if (action.equals(STOP)) {
            stop();

        } else if(action.equals(ADD)){
            String cancion = intent.getStringExtra("cancion");
            if(cancion!=null) {
                add(cancion);
            }
        } else if(action.equals(POSICION)){
            int pos = intent.getIntExtra("posicion", 0);
            mp.seekTo(pos);
        }

        return super.onStartCommand(intent, flags, startId);
    }


    /***********************************************************/
    /*************INTERFAZ PREPARED, COMPLETED LISTENER ********/
    /**********************************************************/

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        estado = Estados.prepared;
        if(reproducir) {
            if(barra!=null){
                barra.cancel(true);
            }
            mp.start();
            estado = Estados.started;
            barra = new BarraCancion();
            barra.execute();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        estado = Estados.completed;
    }


    /***********************************************************/
    /*************INTERFAZ AUDIOFOCUSCHANGE LISTENER ***********/
    /**********************************************************/

    @Override
    public void onAudioFocusChange(int i) {
        switch (i) {
            case AudioManager.AUDIOFOCUS_GAIN:
                mp.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                mp.setVolume(0.4f, 0.4f);
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mp.reset();
        mp.release();
        mp = null;
    }

    /***********************************************************/
    /********************* METODOS DE AUDIO ********************/
    /**********************************************************/

    private void play(){
        if(rutaCancion!=null){
            if(estado== Estados.error){
                estado = Estados.idle;
            }
            if(estado == Estados.idle){
                reproducir = true;
                try {
                    mp.setDataSource(rutaCancion);
                    estado = Estados.initialized;
                } catch (IOException e) {
                    estado= Estados.error;
                }
            }
            if(estado == Estados.initialized || estado == Estados.stopped){
                reproducir = true;
                mp.prepareAsync();
                estado = Estados.preparing;
            } else if(estado == Estados.preparing){
                reproducir=true;
            } else if(estado == Estados.prepared || estado == Estados.started || estado == Estados.paused || estado == Estados.completed){
                mp.start();
                estado=Estados.started;
                barra = new BarraCancion();
                barra.execute();
            }
        }
    }

    private void pause(){
        if(estado == Estados.started){
            mp.pause();
            estado= Estados.paused;
        } else
            if(estado == Estados.paused){
            estado = Estados.started;
        } else
            if(estado == Estados.completed) {
            mp.seekTo(0);
            mp.start();
            estado = Estados.started;
            barra.cancel(true);
            barra = new BarraCancion();
            barra.execute();
        }
    }


    private void stop(){
        if(estado == Estados.prepared || estado == Estados.started || estado == Estados.paused || estado == Estados.completed ){
            mp.stop();
            estado = Estados.stopped;
            mp.reset();
            estado = Estados.idle;
        }
        reproducir = false;
    }

    private void add(String cancion){
        this.rutaCancion = cancion;
    }

    /***********************************************************/
    /********************* NOTIFICACION ********************/
    /**********************************************************/

  /*  public void notificacion(){
        Intent intent = new Intent(this, Principal.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentIntent(pendInt)
                .setSmallIcon(R.drawable.ic_launcher)
                .setOngoing(true)
                .setContentTitle(getString(R.string.app_name));
        Notification noti = builder.build();
        startForeground(NOTIFICACION, noti);
    }*/

    /***********************************************************/
    /**************************** HILO *************************/
    /**********************************************************/

    private class BarraCancion extends AsyncTask<Void, Integer, Integer> {

        private void avance(int i){
            Intent intent = new Intent(AVANCEBARRA);
            intent.putExtra("ahora", i);
            sendBroadcast(intent);
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            while(mp.getDuration() > mp.getCurrentPosition()){
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(estado == Estados.started && !isCancelled()) {
                    avance(mp.getCurrentPosition());
                } else {
                    if(estado == Estados.completed)
                        return mp.getDuration();
                    return mp.getCurrentPosition();
                }
            }
            return mp.getDuration();
        }

        @Override
        protected void onPostExecute(Integer integer) {;
            avance(integer);
        }
    }


}