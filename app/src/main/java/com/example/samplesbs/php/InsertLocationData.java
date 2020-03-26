package com.example.samplesbs.php;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.example.samplesbs.activity.MainActivity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class InsertLocationData extends AsyncTask<String, Void, String> {
    public static final String TAG ="InsertLocationData";
    private ProgressDialog progressDialog;
    private Activity activity;

    public InsertLocationData(Activity activity) {
        this.activity = activity;
    }

    @Override
    protected String doInBackground(String... params) {
        String latitude = (String)params[1];
        String longitude = (String)params[2];
        String token = (String)params[3];

        String serverURL = (String)params[0];

        // 여기에 적어준 이름을 나중에 PHP에서 사용하여 값을 얻게 됩니다
        String postParameters = "latitude=" + latitude + "&longitude=" + longitude+ "&token=" + token;


        try {
            // 2. HttpURLConnection 클래스를 사용하여 POST 방식으로 데이터를 전송합니다
            URL url = new URL(serverURL);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();


            //10초안에 응답, 연결이 되지 않으면 예외 처리
            httpURLConnection.setReadTimeout(10000);
            httpURLConnection.setConnectTimeout(10000);

            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.connect();


            OutputStream outputStream = httpURLConnection.getOutputStream();
            outputStream.write(postParameters.getBytes("UTF-8"));//전송할 데이터가 저장된 변수를 이곳에 입력합니다. 인코딩을 고려해줘야 합니다.

            outputStream.flush();
            outputStream.close();


            //3. 응답을 읽는다.
            int responseStatusCode = httpURLConnection.getResponseCode();
            Log.d("RESPONSE", "POST response code - " + responseStatusCode);

            InputStream inputStream;
            if(responseStatusCode == HttpURLConnection.HTTP_OK) {
                //정상적인 응답 데이터
                inputStream = httpURLConnection.getInputStream();
            }
            else{ //에러 발생
                inputStream = httpURLConnection.getErrorStream();
                Log.e("error",inputStream.toString());
            }


            //String builder를 사용하여 수신되는 데이터를 저장한다.
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            StringBuilder sb = new StringBuilder();
            String line = null;

            while((line = bufferedReader.readLine()) != null){
                sb.append(line);
            }


            bufferedReader.close();

            // 5. 저장된 데이터를 스트링으로 변환하여 리턴합니다.
            return sb.toString();


        } catch (Exception e) {
            Log.e("ERROR", "InsertLocationData: Error ", e);
            return new String("Error: " + e.getMessage());
        }

    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        progressDialog = ProgressDialog.show(activity,
                "Please Wait", null, true, true);
    }


    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        try {
            progressDialog.dismiss();
        }catch (IllegalArgumentException e){
            Log.e(TAG, e.getMessage());
        }
        ((MainActivity)MainActivity.context).setLocationServiceStatus(result);
        Log.d("RESULT", "POST response  - " + result);
    }

}