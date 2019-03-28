package com.example.sd_txt;


import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    //"外部存储设备(实际为内部)/Download/包名/FjLog/FjLog.txt"
    private String packageName; //包名
    private String logFolderName = "FjLog"; //log文件所在的子目录
    private String logFileName = "FjLog.txt";//log文件名称
    private File logFolderDir;
    private File logFile;
    private FileOutputStream fileOutputStream;
    private OutputStreamWriter outputStreamWriter;
    private BufferedWriter bufferedWriter;
    //定义时间戳的格式
    final SimpleDateFormat myformat = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");//"yyyy-MM-dd HH:mm:ss.SSS"
    private boolean logFileIsOK_F; //true=logFile有效；false=logFile无效

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE" };
    private static boolean permissionWriteExternalStorage_F;//读写外部存储器权限的标志

    private EditText mInput;
    private Button mWrite, mRead, mDel;
    private TextView tv1, tv2, textView_permission, textView_filename;


    public MainActivity() {
    }

    //当Activity被创建时
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //============================================================================
        //为本Activity引入指定的布局
        setContentView(R.layout.activity_main);

        //============================================================================
        //初始化布局内的各个控件
        //1.将布局中的控件与本Activity中的变量关联起来
        //2.设置各个按键的回调
        initialView();

        //============================================================================
        //检测是否有写外部存储设备的权限
        //1.读出指定的功能权限的状态
        //2.判断该功能权限是否已被授权
        //3.若尚未被授权，则用ActivityCompat.requestPermissions()弹出对话框申请
        //      onRequestPermissionsResult()为用户操作授权操作后的回调
        verifyPermissions();

        //用一个TextView来显示当前的权限状态
        textView_permission.setText("读写外部存储器的权限="+permissionWriteExternalStorage_F);

        logFileIsOK_F = logfileIsOK();

        String fileName = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS)+"/" + packageName+"/"+ logFolderName + "/" + logFileName;

        textView_filename.setText(fileName);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            bufferedWriter.close();
            outputStreamWriter.close();
            fileOutputStream.close();
            Log.d(TAG, "输出流已关闭！");
        } catch (IOException e) {
            Log.e(TAG, "e.printStackTrace(): ", e);
        }
    }

    //============================================================================
    //检测是否有写外部存储设备的权限
    //1.读出指定的功能权限的状态
    //2.判断该功能权限是否已被授权
    //3.若尚未被授权，则用ActivityCompat.requestPermissions()弹出对话框申请
    //      onRequestPermissionsResult()为用户操作授权操作后的回调
    //============================================================================
    public void verifyPermissions(){
        permissionWriteExternalStorage_F = false;//先默认无权限

        //1.读出指定的功能权限的状态
        int permission = ActivityCompat.checkSelfPermission(this,
                "android.permission.WRITE_EXTERNAL_STORAGE");
        //2.判断该功能权限是否已被授权
        if (permission != PackageManager.PERMISSION_GRANTED) {
            //若尚未被授权，则弹出对话框申请
            //参数1=传入的activity
            //参数2=String数组，待申请的权限名
            //参数3=请求码，只要是唯一值就行
            ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
        }
        else {
            permissionWriteExternalStorage_F = true;//表示有权限
            Log.d(TAG, "恭喜！已有操作外部存储器的权限");
        }
    }

    //ActivityCompat.requestPermissions运行时弹出的用户授权窗口的被用户操作后的回调
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    permissionWriteExternalStorage_F = true;//表示有权限
                    Log.d(TAG, "动态授权成功！");
                    logFileIsOK_F = logfileIsOK();
                }
                else{
                    Log.d(TAG, "用户禁止授权！");
                }
                break;
            default:
        }
    }


    //============================================================================
    //初始化布局内的各个控件
    //1.将布局中的控件与本Activity中的变量关联起来
    //2.设置各个按键的回调
    //============================================================================
    public void initialView() {
        mInput = findViewById(R.id.ed_input);
        mWrite = findViewById(R.id.btn_write);
        mRead = findViewById(R.id.btn_read);
        mDel = findViewById(R.id.btn_del);
        tv1 = findViewById(R.id.tv_tv1);
        tv2 = findViewById(R.id.tv_tv2);
        textView_permission = findViewById(R.id.tv_permisson);
        textView_filename = findViewById(R.id.tv_filename);

        mRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //读取文件
                tv2.setText(readTxt());
            }
        });

        mWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //获得文本框中输入的内容
                String msg = mInput.getText().toString();
                tv1.setText(writeToLogFile(msg));
            }
        });

        mDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delLogFile();
            }
        });
    }

    //判断log文件是否有效
    public boolean logfileIsOK() {
        boolean OK_F = false;//先默认log文件是error的

        packageName = this.getPackageName();

        //"外部存储设备(实际为内部)/Download/包名/FjLog/FjLog.txt"
        String dir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS)+"/" + packageName+"/"+logFolderName;
        logFolderDir = new File(dir);

        //若文件夹路径不存在，则创建
        if (!(OK_F = logFolderDir.exists())) {
            OK_F = logFolderDir.mkdirs();
        }
        //若文件夹路径不存在，且创建失败
        if (!OK_F) {
            Log.d(TAG, "文件夹不存在，且创建失败！");
            Toast.makeText(MainActivity.this, "文件夹不存在，且创建失败！", Toast.LENGTH_SHORT).show();
            return OK_F;
        }
        //若文件夹路径已存在，或创建成功
        else {
            Log.d(TAG, "文件夹已存在");
            try {
                //获取指定的文件对象("外部存储设备(实际为内部)/Download/包名/FjLog/FjLog.txt"),并用logFile指向它
                logFile = new File(logFolderDir, logFileName);
                //若此文件不存在，则创建
                if (!logFile.exists()) {
                    logFile.createNewFile();
                    Log.d(TAG, "logfileIsOK: 创建log文件成功！");
                    Toast.makeText(MainActivity.this, "创建log文件成功！", Toast.LENGTH_SHORT).show();
                }else{
                    Log.d(TAG, "文件已存在");
                }
                //执行到此处，说明文件已存在，或已创建成功
                fileOutputStream = new FileOutputStream(logFile, true);
                outputStreamWriter = new OutputStreamWriter(fileOutputStream);
                bufferedWriter = new BufferedWriter(outputStreamWriter);
                OK_F = true;
            } catch (IOException e) {
                Log.e(TAG, "Exception:", e);
                return OK_F;
            }
        }
        return OK_F;
    }

    public String writeToLogFile(String msg) {

        //若log文件有效
        if(logFileIsOK_F) {
            try {
                //获取当前Calendar类对象
                //注意：必须每次当场获取当前Calendar类对象
                Calendar calendar = Calendar.getInstance();
                //通过当前Calendar类对象来获取当前时间，并按指定格式格式化为时间戳
                String timeStamp = myformat.format(calendar.getTime());

                //向此输出流末尾追加指定内容
                //“时间戳 包名/TAG: 消息”
                msg = String.format("%s %s/%s: %s \n", timeStamp, packageName, TAG, msg);
                //outStream.write(msg.getBytes());
                bufferedWriter.write(msg);
                //写完后关闭输出流
                bufferedWriter.flush();
                Log.d(TAG, msg);
            } catch (IOException e) {
                msg = "写入失败";
                Log.e(TAG, "Exception:", e);
                Toast.makeText(MainActivity.this, "写入失败", Toast.LENGTH_SHORT).show();
            }
        }
        //若log文件无效
        else{
            msg = "log文件无效，无法写入！";
            Log.d(TAG, "log文件无效，无法写入！");
            Toast.makeText(MainActivity.this, "log文件无效，无法写入！", Toast.LENGTH_SHORT).show();
        }

        return msg;
    }

    public String readTxt() {

        BufferedReader bufferedReader = null;
        String str = null;
        String str_all = null;

        try {
            //定义文件路径及文件名("外部存储设备(实际为内部)/Download/包名/FjLog/FjLog.txt")
            String file = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS)+"/" + packageName+"/"+ logFolderName + "/" + logFileName;
            //1.获取指定的FileReader型文件对象("外部存储设备/Download/FjLog/FjLog.txt")，此处为匿名对象，作为入口参数
            //2.获取指定的文件对象的BufferedReader流，并用bre指向它
            bufferedReader = new BufferedReader(new FileReader(file));

            while ((str = bufferedReader.readLine()) != null){// 判断最后一行不存在，为空结束循环

                if(str_all==null){
                    str_all = str + "\n";
                }
                else {
                    str_all += str + "\n";
                }
                Log.d(TAG, "readTxt: " + str);
            }

            Toast.makeText(MainActivity.this, "Read OK!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            str_all = "发生读错误！";
            Log.e(TAG, "e.printStackTrace():", e);
            Toast.makeText(MainActivity.this, "Read error!", Toast.LENGTH_SHORT).show();
        }
        return str_all;
    }

    public void delLogFile(){
        //定义文件路径及文件名("外部存储设备(实际为内部)/Download/包名/FjLog/FjLog.txt")
        String fileName = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS)+"/" + packageName+"/"+ logFolderName + "/" + logFileName;

        File file = new File(fileName);

        if(file.exists()&&file.isFile()) {
            file.delete();
            Log.d(TAG, "delLogFile: 文件已删除");
            textView_filename.setText("文件已删除");
            Toast.makeText(MainActivity.this, "文件已删除", Toast.LENGTH_SHORT).show();
        }
        else {
            textView_filename.setText("文件已不存在！");
            Toast.makeText(MainActivity.this, "文件已不存在！", Toast.LENGTH_SHORT).show();
        }

        logFileIsOK_F = false;
    }
}
