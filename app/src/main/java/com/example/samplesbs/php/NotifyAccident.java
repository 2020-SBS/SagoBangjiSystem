package com.example.samplesbs.php;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import com.example.samplesbs.data_model.LocationData;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Queue;

public class NotifyAccident extends AsyncTask<String, String, String> {
    public static final String TAG = "NotifyAccident";
    private String errorString = null;
    private Context mContext;
    private String mJsonString = null;
    private Queue<LocationData> queue;

    public NotifyAccident() {
    }

    public NotifyAccident(Context context) {
        mContext = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();


    }


    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if (result == null) {
            Log.e("error", errorString);
        } else {
            mJsonString = result;
            Log.e("result",result);
            showResult();
        }
    }

    @Override
    protected String doInBackground(String... params) {
        String latitude = (String) params[1];
        String longitude = (String) params[2];
        String accident = (String) params[3];
        String token = (String) params[4];

        String serverURL = (String) params[0];

        // 여기에 적어준 이름을 나중에 PHP에서 사용하여 값을 얻게 됩니다
        String postParameters = "latitude=" + latitude + "&longitude=" + longitude+"&accident="+accident+"&token="+token;
        postParameters=setPostParameter(postParameters);

        try {
            URL url = new URL(serverURL);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();


            //5초안에 응답, 연결이 되지 않으면 예외 처리
            httpURLConnection.setReadTimeout(5000);
            httpURLConnection.setConnectTimeout(5000);

            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setDoInput(true);

            httpURLConnection.connect();

            OutputStream outputStream = httpURLConnection.getOutputStream();
            outputStream.write(postParameters.getBytes("UTF-8"));//전송할 데이터가 저장된 변수를 이곳에 입력합니다. 인코딩을 고려해줘야 합니다.

            outputStream.flush();
            outputStream.close();


            //3. 응답을 읽는다.
            int responseStatusCode = httpURLConnection.getResponseCode();
            Log.d(TAG, "POST response code - " + responseStatusCode);

            InputStream inputStream;
            if (responseStatusCode == HttpURLConnection.HTTP_OK) {
                //정상적인 응답 데이터
                inputStream = httpURLConnection.getInputStream();
            } else { //에러 발생
                inputStream = httpURLConnection.getErrorStream();
                Log.e("error", inputStream.toString());
            }


            //String builder를 사용하여 수신되는 데이터를 저장한다.
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            StringBuilder sb = new StringBuilder();
            String line = null;

            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }


            bufferedReader.close();

            // 5. 저장된 데이터를 스트링으로 변환하여 리턴합니다.
            return sb.toString().trim();

        } catch (Exception e) {
            Log.d("ERROR", "NotifyAccident: Error ", e);
            return new String("Error: " + e.getMessage());
        }
    }

    private void showResult() {
        String TAG_JSON = "location";
        String TAG_ID = "id";
        String TAG_LATITUDE = "latitude";
        String TAG_LONGITUDE = "longitude";
        String TAG_TOKEN = "token";


        try {
            //php에서 unseen한 스트링이 같이 넘어온다
            //https://stackoverflow.com/questions/10267910/jsonexception-value-of-type-java-lang-string-cannot-be-converted-to-jsonobject
            JSONObject jsonObject = new JSONObject(mJsonString);
            JSONArray jsonArray = jsonObject.getJSONArray(TAG_JSON);

            for (int i = 0; i < jsonArray.length(); i++) {

                JSONObject item = jsonArray.getJSONObject(i);
                String id = item.getString(TAG_ID);
                String latitude = item.getString(TAG_LATITUDE);
                String longitude = item.getString(TAG_LONGITUDE);
                String token = item.getString(TAG_TOKEN);
                Log.d("location", "(" + latitude + "," + longitude + ")");
                Log.d("token", token);

            }

        } catch (JSONException e) {
            if (mJsonString.trim().equals("empty data set"))
                Log.d(TAG, e.getMessage());
        }

    }

    public void setQueue(Queue<LocationData> queue) {
        this.queue=queue;
    }

    private String setPostParameter(String parameters) {
        String temp = parameters;
        int firstQueueSize = queue.size();
        temp += "&size=" + queue.size();
        for (int i = 0; i < firstQueueSize; i++) {
            LocationData location = queue.poll();
            temp += "&latitude" + i + "=" + location.getLatitude() + "&longitude" + i + "=" + location.getLongitude() + "&angle" + i + "=" + location.getAngle();
        }
        return temp;
    }

}