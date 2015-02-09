package com.example.sadarik.reproductordemusica;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;
import java.math.BigDecimal;

public class Adaptador extends CursorAdapter {

    public Adaptador(Context context, Cursor data) {
        super(context, data, true);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        LayoutInflater i = LayoutInflater.from(viewGroup.getContext());
        View v = i.inflate(R.layout.detalle, viewGroup, false);
        return v;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = null;

        if (view != null) {
            holder = new ViewHolder();
            holder.tvCancion = (TextView) view.findViewById(R.id.tvCancion);
            holder.tvArtista = (TextView) view.findViewById(R.id.tvArtista);
            holder.tvDuracion = (TextView)view.findViewById(R.id.tvDuracion);

            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }
      //Titulo de la canción
        int titulo = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
        String tituloCancion = cursor.getString(titulo);
        holder.tvCancion.setText(tituloCancion);

      //Nombre del artista
        int artista = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        String nombre = cursor.getString(artista);
        holder.tvArtista.setText(nombre);

       //Duración de la pista
        long durationInMs = Long.parseLong(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION)));
        double durationInMin = ((double)durationInMs/1000.0)/120.0;
        durationInMin = new BigDecimal(Double.toString(durationInMin)).setScale(2, BigDecimal.ROUND_UP).doubleValue();
        holder.tvDuracion.setText("" + durationInMin);
    }

    static class ViewHolder {
        TextView tvCancion;
        TextView tvArtista;
        TextView tvDuracion;
    }
}
