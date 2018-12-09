/*
The MIT License (MIT)

Copyright (c) 2013 pwlin - pwlin05@gmail.com

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package io.github.pwlin.cordova.plugins.fileopener2;

import java.io.File;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.widget.Toast;
//import android.util.Log;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CordovaResourceApi;


public class FileOpener2 extends CordovaPlugin {

    /**
     * Executes the request and returns a boolean.
     *
     * @param action          The action to execute.
     * @param args            JSONArry of arguments for the plugin.
     * @param callbackContext The callback context used when calling back into JavaScript.
     * @return boolean.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (action.equals("open")) {
            this._open(args.getString(0), args.getString(1), callbackContext);
        } else if (action.equals("uninstall")) {
            this._uninstall(args.getString(0), callbackContext);
        } else if (action.equals("appIsInstalled")) {
            JSONObject successObj = new JSONObject();
            if (this._appIsInstalled(args.getString(0))) {
                successObj.put("status", PluginResult.Status.OK.ordinal());
                successObj.put("message", "Installed");
            } else {
                successObj.put("status", PluginResult.Status.NO_RESULT.ordinal());
                successObj.put("message", "Not installed");
            }
            callbackContext.success(successObj);
        } else {
            JSONObject errorObj = new JSONObject();
            errorObj.put("status", PluginResult.Status.INVALID_ACTION.ordinal());
            errorObj.put("message", "Invalid action");
            callbackContext.error(errorObj);
        }
        return true;
    }

    private File file = null;
    private CallbackContext callbackContext = null;

    private boolean installAllowed() {
        boolean installAllowed = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            installAllowed = cordova.getActivity().getPackageManager().canRequestPackageInstalls();
        }
        return installAllowed;
    }

    private void _open(String fileArg, String contentType, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        String fileName = "";
        try {
            CordovaResourceApi resourceApi = webView.getResourceApi();
            Uri fileUri = resourceApi.remapUri(Uri.parse(fileArg));
            fileName = this.stripFileProtocol(fileUri.toString());
        } catch (Exception e) {
            fileName = fileArg;
        }
//        File file = new File(fileName);
        file = new File(fileName);
        if (file.exists()) {
            try {
                Uri path = Uri.fromFile(file);
//				Intent intent = new Intent(Intent.ACTION_VIEW);
//				intent.setDataAndType(path, contentType);
//				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);


                //判读版本是否在8.0以上
                if (Build.VERSION.SDK_INT >= 26) {
                    //来判断应用是否有权限安装apk

                    if (installAllowed()) {
                        installApk();
                    } else {
                        //无权限 申请权限
                        new AlertDialog.Builder(cordova.getActivity())
                                .setTitle("提示")
                                .setMessage("安装应用需要打开未知来源权限，请去设置中开启权限")
                                .setNegativeButton("退出", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                        Toast.makeText(cordova.getActivity(), "没有安装权限，安装失败。", Toast.LENGTH_LONG).show();
//                                        System.exit(0);
                                    }
                                })
                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            Uri packageURI = Uri.parse("package:" + cordova.getActivity().getPackageName());
                                            //注意这个是8.0新API
                                            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageURI);
                                            cordova.setActivityResultCallback(new CordovaPlugin() {
                                                @Override
                                                public void onActivityResult(int requestCode, int resultCode, Intent intent) {
                                                    super.onActivityResult(requestCode, resultCode, intent);
                                                    switch (requestCode) {
                                                        case 20:
                                                            if (installAllowed()) {
                                                                installApk();
                                                            }
                                                            break;
                                                    }

                                                }
                                            });
                                            cordova.getActivity().startActivityForResult(intent, 20);
                                            dialog.cancel();
                                        }
                                    }
                                })
                                .show();

                    }
                }

//                installApk();

//				Intent intent = new Intent(Intent.ACTION_VIEW);
//				// 由于没有在Activity环境下启动Activity,设置下面的标签
//				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//				if(Build.VERSION.SDK_INT>=24) { //判读版本是否在7.0以上
//					//参数1 上下文, 参数2 Provider主机地址 和配置文件中保持一致   参数3  共享的文件
//					Uri apkUri =
//							FileProvider.getUriForFile(cordova.getActivity(), cordova.getActivity().getPackageName()+".fileprovider", file);
//					//添加这一句表示对目标应用临时授权该Uri所代表的文件
//					intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//					intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
//				}else{
//					intent.setDataAndType(Uri.fromFile(file),
//							"application/vnd.android.package-archive");
//				}
//				/*
//				 * @see
//				 * http://stackoverflow.com/questions/14321376/open-an-activity-from-a-cordovaplugin
//				 */
//				cordova.getActivity().startActivity(intent);
//				//cordova.getActivity().startActivity(Intent.createChooser(intent,"Open File in..."));
//				callbackContext.success();
            } catch (android.content.ActivityNotFoundException e) {
                JSONObject errorObj = new JSONObject();
                errorObj.put("status", PluginResult.Status.ERROR.ordinal());
                errorObj.put("message", "Activity not found: " + e.getMessage());
                callbackContext.error(errorObj);
            }
        } else {
            JSONObject errorObj = new JSONObject();
            errorObj.put("status", PluginResult.Status.ERROR.ordinal());
            errorObj.put("message", "File not found");
            callbackContext.error(errorObj);
        }
    }

    private void installApk() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        // 由于没有在Activity环境下启动Activity,设置下面的标签
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= 24) { //判读版本是否在7.0以上
            //参数1 上下文, 参数2 Provider主机地址 和配置文件中保持一致   参数3  共享的文件
            Uri apkUri =
                    FileProvider.getUriForFile(cordova.getActivity(), cordova.getActivity().getPackageName() + ".fileprovider", file);

            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            //添加这一句表示对目标应用临时授权该Uri所代表的文件
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(Uri.fromFile(file),
                    "application/vnd.android.package-archive");
        }
                /*
                 * @see
				 * http://stackoverflow.com/questions/14321376/open-an-activity-from-a-cordovaplugin
				 */
        cordova.getActivity().startActivity(intent);
        //cordova.getActivity().startActivity(Intent.createChooser(intent,"Open File in..."));
        callbackContext.success();
    }

    private void _uninstall(String packageId, CallbackContext callbackContext) throws JSONException {
        if (this._appIsInstalled(packageId)) {
            Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
            intent.setData(Uri.parse("package:" + packageId));
            cordova.getActivity().startActivity(intent);
            callbackContext.success();
        } else {
            JSONObject errorObj = new JSONObject();
            errorObj.put("status", PluginResult.Status.ERROR.ordinal());
            errorObj.put("message", "This package is not installed");
            callbackContext.error(errorObj);
        }
    }

    private boolean _appIsInstalled(String packageId) {
        PackageManager pm = cordova.getActivity().getPackageManager();
        boolean appInstalled = false;
        try {
            pm.getPackageInfo(packageId, PackageManager.GET_ACTIVITIES);
            appInstalled = true;
        } catch (PackageManager.NameNotFoundException e) {
            appInstalled = false;
        }
        return appInstalled;
    }

    private String stripFileProtocol(String uriString) {
        if (uriString.startsWith("file://")) {
            uriString = uriString.substring(7);
        }
        return uriString;
    }


}
