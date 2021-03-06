package com.gil.finalproject.Location;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.gil.finalproject.Book;
import com.gil.finalproject.MainActivity;
import com.gil.finalproject.R;
import com.gil.finalproject.RetrofitInstance;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static android.content.Context.LOCATION_SERVICE;


/**
 * A simple {@link Fragment} subclass.
 */
public class LocationFrag extends Fragment {


    private RecyclerView locationRV;
    EndPointForLocation service;
    LocationAdpter locAdpter;
    Retrofit retrofit;
    String BASE_URL = "https://maps.googleapis.com/";
    String key = "AIzaSyCAU7syPLk4OKPV7UlInRBfVkCXOH7Xrdw ";
    String decodedQuery;
    LocationManager locationManager;
    Location lastKnowLoc;
    public String textToSearch = "";
    public List<Book> lastBook = null;
    ProgressDialog dialog;


    public LocationFrag() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_location, container, false);
        locationRV = (RecyclerView) v.findViewById(R.id.locationRV);
        locationRV.setLayoutManager(new GridLayoutManager(getActivity(), 1));
        locationRV.setHasFixedSize(true);

        locationManager = (LocationManager) getActivity().getSystemService(LOCATION_SERVICE);
        searchText(textToSearch);

        return v;
    }

    @SuppressLint("MissingPermission")
    public void searchText(String query) {

        try {
            decodedQuery = java.net.URLDecoder.decode(String.valueOf(query), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        dialog = ProgressDialog.show(getActivity(), "searching..", "searching..");

        service = RetrofitInstance.getRetrofitInstance().create(EndPointForLocation.class);

        String locationsData = null;
        if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MainActivity.LOCATION_KEY_REQUEST);
            }
        } else {

            if (locationsData != null) {
                lastKnowLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                locationsData = "" + lastKnowLoc.getLatitude() + "," + lastKnowLoc.getLongitude();
            }
            if (locationsData == null) {

                double lat = MainActivity.newLat;
                double lng = MainActivity.newLng;
                locationsData = "" + lat + " , " + lng;
            }
        }

        Call<MyLocationResults> newCall = service.getAllGeometry(locationsData, "1500", decodedQuery, key);
        Log.d("", "searchText:--------------------------- " + locationsData);
        newCall.enqueue(new Callback<MyLocationResults>() {
            @Override
            public void onResponse(Call<MyLocationResults> call, Response<MyLocationResults> response) {
                if (!response.isSuccessful()) {
                    Log.d("", "onResponse: -------------------------------" + response.code());
                    return;
                }
                MyLocationResults locationResultsWithObject = response.body();
                List<MyLocationModels> allResults = locationResultsWithObject.results;

                //delete all the data
                Book.deleteAll(Book.class);
//running on the list values of the objects
                for (int position = 0; position < allResults.size(); position++) {
                    if (allResults != null) {
                        String nameDB = allResults.get(position).name;
                        String adressDB = allResults.get(position).vicinity;
                        double latDB = allResults.get(position).geometry.location.lat;
                        double lngDB = allResults.get(position).geometry.location.lng;
//creasting new list and all the objects into it
                        List<Book> books = new ArrayList<>();
                        books.add(new Book(nameDB, adressDB, latDB, lngDB));
                        Book.saveInTx(books);

                        lastBook = Book.listAll(Book.class);

                        ArrayList<Book> lastBook = new ArrayList<>();
                        lastBook.addAll(lastBook);
                    }

                    locAdpter = new LocationAdpter(getActivity(), allResults);
                    Log.d("", "onResponse:----------------------- " + allResults.size());
                    locationRV.setAdapter(locAdpter);
                    locAdpter.notifyDataSetChanged();
                }
                dialog.dismiss();
            }

            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onFailure(Call<MyLocationResults> call, Throwable t) {

                Toast.makeText(getContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }


        });


    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 15) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //get location
                @SuppressLint("MissingPermission")
                Location loc = ((LocationManager) getActivity().getSystemService(LOCATION_SERVICE)).getLastKnownLocation(LocationManager.GPS_PROVIDER);


            } else {
                Toast.makeText(getActivity(), "Please allow GPS providers...", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
