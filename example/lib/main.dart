import 'dart:io';

import 'package:bundle_id_launch/bundle_id_launch.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'dart:async';

import 'package:flutter_overlay_apps/flutter_overlay_apps.dart';
import 'package:package_info_plus/package_info_plus.dart';

void main() {
  runApp(const MyApp());
  if (Platform.isAndroid) {
    // 设置状态栏背景及颜色
    SystemUiOverlayStyle systemUiOverlayStyle =
    const SystemUiOverlayStyle(statusBarColor: Colors.transparent);
    SystemChrome.setSystemUIOverlayStyle(systemUiOverlayStyle);
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.manual, overlays: []); //隐藏状态栏
  }
}

// overlay entry point
@pragma("vm:entry-point")
void showOverlay() {
  runApp(
    const MaterialApp(
      // color: Colors.transparent,
      debugShowCheckedModeBanner: false,
      home: MyOverlaContent(),
    ),
  );
  if (Platform.isAndroid) {
    // 设置状态栏背景及颜色
    SystemUiOverlayStyle systemUiOverlayStyle =
    const SystemUiOverlayStyle(statusBarColor: Colors.transparent);
    SystemChrome.setSystemUIOverlayStyle(systemUiOverlayStyle);
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.manual, overlays: []); //隐藏状态栏
  }
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  List<String> _debugInfoList = [];
  StreamSubscription? _overlayListener;

  @override
  void initState() {
    super.initState();
    _overlayListener = FlutterOverlayApps.overlayListener().listen((event) {
      debugPrint('---> _MyAppState.initState overlayListener event: ${event}');
      _debugInfoList.add(event.toString());
      setState((){});
    });
    _prepare();
  }

  late PackageInfo _packageInfo;
  Future<bool> _prepare() async{
    _packageInfo = await PackageInfo.fromPlatform();
    return true;
  }
  Future<bool> _launchSelf() async{
    var packageBundleId = _packageInfo.packageName;
    return BundleIdLaunch.launch(bundleId: packageBundleId);
  }
  @override
  void dispose() {
    _overlayListener?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      color: Colors.blue,
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        backgroundColor: Colors.white,
        body: Center(
          child: SingleChildScrollView(
            child: Column(
              children: [
                ElevatedButton(
                  onPressed: () async {
                    var res = await FlutterOverlayApps.checkPermission();
                    _debugInfoList.add(res ? 'has Permission' : 'no Permission');
                    setState(() {});
                  },
                  child: const Text("checkPermission"),
                ),
                ElevatedButton(
                  onPressed: () async {
                    var resOfRequest = await FlutterOverlayApps.requestPermission();
                    debugPrint('---> _MyAppState.build resOfRequest: ${resOfRequest}');
                    if (resOfRequest) _launchSelf();
                    _debugInfoList.add('requestPermission: $resOfRequest');
                    setState(() {});
                  },
                  child: const Text("requestPermission"),
                ),
                ElevatedButton(
                  onPressed: () async {
                    // Open overlay
                    // await FlutterOverlayApps.showOverlay(height: 300, width: 400, alignment: OverlayAlignment.center);
                    await FlutterOverlayApps.showOverlay();
                    // send data to ovelay
                    await Future.delayed(const Duration(seconds: 2));
                    FlutterOverlayApps.sendDataToAndFromOverlay("Hello from main app");
                  },
                  child: const Text("showOverlay"),
                ),
                ..._debugInfoList.map((e) => Text(e)).toList()
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class MyOverlaContent extends StatefulWidget {
  const MyOverlaContent({Key? key}) : super(key: key);

  @override
  State<MyOverlaContent> createState() => _MyOverlaContentState();
}

class _MyOverlaContentState extends State<MyOverlaContent> {
  String _dataFromApp = "Hey send data";

  @override
  void initState() {
    super.initState();

    // lisent for any data from the main app
    FlutterOverlayApps.overlayListener().listen((event) {
      setState(() {
        _dataFromApp = event.toString();
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      // backgroundColor: Colors.red.withOpacity(0.4),
      backgroundColor: Colors.red,
      body: Center(
        child: SingleChildScrollView(
          child: Column(
            children: [
              Card(
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Center(
                  child: Text(
                    _dataFromApp,
                  ),
                ),
              ),
              OutlinedButton(
                onPressed: () {
                  FlutterOverlayApps.sendDataToAndFromOverlay("Hello from Sub App");
                },
                child: const Text(
                  'Send Message From Sub App',
                ),
              ),
              OutlinedButton(
                onPressed: () {
                  FlutterOverlayApps.closeOverlay();
                },
                child: const Text(
                  'Close',
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
