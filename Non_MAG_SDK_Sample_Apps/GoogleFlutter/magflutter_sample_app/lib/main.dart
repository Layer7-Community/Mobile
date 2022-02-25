import 'dart:io';
import 'dart:math';
import 'dart:convert';
import 'dart:isolate';
import 'dart:async';

import 'package:flutter_countdown_timer/countdown_timer_controller.dart';
import 'package:flutter_countdown_timer/current_remaining_time.dart';
import 'package:uuid/uuid.dart';

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:http/io_client.dart';
import 'package:device_info/device_info.dart';

import 'package:basic_utils/basic_utils.dart';

import 'package:flutter/services.dart';
import 'package:geolocator/geolocator.dart';
import 'package:flutter_countdown_timer/flutter_countdown_timer.dart';

import 'button.dart';

import 'magClientStorageHandler.dart';

import 'package:rflutter_alert/rflutter_alert.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'MAG Sample App',
      theme: ThemeData(
        // This is the theme of your application.
        //
        // Try running your application with "flutter run". You'll see the
        // application has a blue toolbar. Then, without quitting the app, try
        // changing the primarySwatch below to Colors.green and then invoke
        // "hot reload" (press "r" in the console where you ran "flutter run",
        // or simply save your changes to "hot reload" in a Flutter IDE).
        // Notice that the counter didn't reset back to zero; the application
        // is not restarted.
        primarySwatch: Colors.blue,
      ),
      home: const MagHomePage(title: ''),
    );
  }
}

class MagHomePage extends StatefulWidget {
  const MagHomePage({Key? key, required this.title}) : super(key: key);

  // This widget is the home page of your application. It is stateful, meaning
  // that it has a State object (defined below) that contains fields that affect
  // how it looks.

  // This class is the configuration for the state. It holds the values (in this
  // case the title) provided by the parent (in this case the App widget) and
  // used by the build method of the State. Fields in a Widget subclass are
  // always marked "final".

  final String title;

  @override
  State<MagHomePage> createState() => _MagHomePageState();
}

class _MagHomePageState extends State<MagHomePage> {
  String _magServerHostname = '';
  int _magServerHostPort = 0;
  String _magServerPEMEncodedCertificate = '';
  String _magClientPEMEncodedCertificate = '';
  String _magMasterClientId = '';
  String _magMasterClientScope = '';

  final _magUsernameController = TextEditingController();
  final _magPasswordController = TextEditingController();

  String _appTitle = 'MAG Native Flutter App';

  String _userAuthenticatedStatus = 'User Status: Not Authenticated';

  bool _hideLoginButton = false;
  bool _showProgressIndicator = false;

  String _apiJsonResult = "Please Login!";
  SecurityContext _magClientSecurityContext = SecurityContext.defaultContext;

  magClientStorageHandler _magSecureStorage = magClientStorageHandler();

  bool _magEnablePkiPinning = false;
  int _accessTokenExpirySec = 0;

  var _countDownTimer;
  bool positionServiceEnabled = false;
  late LocationPermission deviceLocationPermission;

  @override
  void initState() {
    // TODO: implement initState
    super.initState();
    _initAccessTokenRefreshTimeout();
  }

  void _initAccessTokenRefreshTimeout() async {
    int currentTime = (DateTime.now().millisecondsSinceEpoch / 1000).floor();

    String expiresIn = await _magSecureStorage.getValuefromSecureStorage('expires_in');
    if (expiresIn != '') {
      int expiryTimeAccessToken = int.parse(expiresIn);
      _accessTokenExpirySec = (expiryTimeAccessToken - currentTime);
      _countDownTimer = CountdownTimer(
          onEnd: refreshAccessToken,
          endTime: DateTime.now().millisecondsSinceEpoch + 1000 * _accessTokenExpirySec,
          widgetBuilder: (_, CurrentRemainingTime? time) {
            if (time == null) {
              return Text('Access Token has expired');
            }
            return Text('Access Token Auto-Refreshing in: ${time.min} mins,  ${time.sec} secs');
          });
    } else
      _accessTokenExpirySec = 0;
  }

  //
  // Handler for invoking the unprotected API exposed on the API Gateway
  //
  void _upProtectedAPIHandler() {
    setState(() {
      _showProgressIndicator = true;
    });

    fetchUnProtectedData();

    setState(() {
      _showProgressIndicator = false;
    });
  }

//
//
//
  void _protectedAPIHandler() {
    setState(() {
      _showProgressIndicator = true;
    });

    fetchProtectedData();

    setState(() {
      _showProgressIndicator = false;
    });
  }

  Future<void> _loginHandler() async {
    //
    // Step 1: Invoke the /connect/client/initialize MAG Server EndPoint with the master Client Id (msso_config_file), a nonce and device Identifier (as headers)
    //

    String magBase64ClientDeviceIdentifier = await _magSecureStorage.getValuefromSecureStorage('device_id');

    print('LoginHandler: Client Device Identifier [' + magBase64ClientDeviceIdentifier + ']');

    String magClientAccessToken = await _magSecureStorage.getValuefromSecureStorage('access_token');

    if (magClientAccessToken == '' || await _hasAccessTokenExpired()) {
      await _onAlertWithCustomContentPressed(context);
    } else {
      String magUserName = await _magSecureStorage.getValuefromSecureStorage('magUserName');
      setState(() {
        _apiJsonResult = '';
        _userAuthenticatedStatus = 'Status: User Authenticated';
        _appTitle = 'Welcome user: [' + magUserName + ']';
        _hideLoginButton = true;
      });
    }
  }

  void _logoutHandler() async {
    final String magLogoutEndPointUrl =
        'https://' + _magServerHostname + ':' + _magServerHostPort.toString() + '/connect/session/logout';

    try {
      String pemEncodedPrivateKey = await _magSecureStorage.getValuefromSecureStorage('private-key');
      _magClientSecurityContext.usePrivateKeyBytes(pemEncodedPrivateKey.codeUnits);

      String pemEncodedMagClientCertificate = await _magSecureStorage.getValuefromSecureStorage('x-cert');

      _magClientSecurityContext.useCertificateChainBytes(pemEncodedMagClientCertificate.codeUnits);

      HttpClient _httpClient = new HttpClient(context: _magClientSecurityContext)
        ..badCertificateCallback = ((X509Certificate cert, String host, int port) {
          print('Server Name is: [' + host + ']');
          print('Server PEM Certificate: [' + cert.pem + ']');
          showAlertDialog(this.context, "Alert", "Imvalid certificate presented by the MAG Server");
          return false;
        });

      IOClient ioClient = new IOClient(_httpClient);

      Map<String, String> headers = {
        "Content-Type": 'application/x-www-form-urlencoded',
        "mag-identifier": await _magSecureStorage.getValuefromSecureStorage('mag-identifier')
      };

      Map<String, String> params = Map();

      params['client_id'] = await _magSecureStorage.getValuefromSecureStorage('client_id');
      params['client_secret'] = await _magSecureStorage.getValuefromSecureStorage('client_secret');
      params['id_token'] = await _magSecureStorage.getValuefromSecureStorage('id-token');
      params['id_token_type'] = await _magSecureStorage.getValuefromSecureStorage('id-token-type');
      params['logout_apps'] = 'true';

      //
      // Need to ensure we are performing Mutual SSL with the MAG Server
      //

      final response = await ioClient.post(Uri.parse(magLogoutEndPointUrl), headers: headers, body: params);

      if (response.statusCode == 200) {
        // If the server did return a 200 OK response,
        // then parse the JSON response.

        final data = json.decode(response.body);
        setState(() {
          _apiJsonResult = data['session_status'];
        });
        _magSecureStorage.writeValuetoSecureStorage('access_token', '');
        _magSecureStorage.writeValuetoSecureStorage('id-token', '');
        _magSecureStorage.writeValuetoSecureStorage('id-token-type', '');
      } else {
        // If the server did not return a 200 OK response,
        // then throw an exception.
        print('Logout : Failed to load json response from API Gateway');
      }
    } catch (e) {
      print(Exception(e));
    }
    _magSecureStorage.writeValuetoSecureStorage('magUserName', '');
    _magSecureStorage.writeValuetoSecureStorage('magUserPassword', '');

    print('LogoutHandler: Logout at the MAG Server');

    setState(() {
      _hideLoginButton = false;
      _apiJsonResult = '';
      _userAuthenticatedStatus = 'Not Authenticated';
      _appTitle = 'MAG Native Flutter App';
    });
  }

  void _deRegisterHandler() async {
    final String magDeRegisterEndPointUrl =
        'https://' + _magServerHostname + ':' + _magServerHostPort.toString() + '/connect/device/remove';

    try {
      //
      // Need to ensure we are performing Mutual SSL with the MAG Server
      //

      String pemEncodedPrivateKey = await _magSecureStorage.getValuefromSecureStorage('private-key');
      _magClientSecurityContext.usePrivateKeyBytes(pemEncodedPrivateKey.codeUnits);

      String pemEncodedMagClientCertificate = await _magSecureStorage.getValuefromSecureStorage('x-cert');

      _magClientSecurityContext.useCertificateChainBytes(pemEncodedMagClientCertificate.codeUnits);

      HttpClient _httpClient = new HttpClient(context: _magClientSecurityContext)
        ..badCertificateCallback = ((X509Certificate cert, String host, int port) {
          print('Server Name is: [' + host + ']');
          print('Server PEM Certificate: [' + cert.pem + ']');
          showAlertDialog(this.context, "Alert", "Imvalid certificate presented by the MAG Server");
          return false;
        });

      IOClient ioClient = new IOClient(_httpClient);

      Map<String, String> headers = {
        "Content-Type": 'text/plain',
        "mag-identifier": await _magSecureStorage.getValuefromSecureStorage('mag-identifier'),
        "Authorization": 'Bearer ' + await _magSecureStorage.getValuefromSecureStorage('access_token'),
        'x-cert': base64.encode(utf8.encode(pemEncodedMagClientCertificate)),
      };

      final response = await ioClient.delete(Uri.parse(magDeRegisterEndPointUrl), headers: headers);

      if (response.statusCode == 200) {
        // If the server did return a 200 OK response,
        // then parse the JSON response.

        final data = json.decode(response.body);

        _magSecureStorage.writeValuetoSecureStorage('access_token', '');
        _magSecureStorage.writeValuetoSecureStorage('id-token', '');
        _magSecureStorage.writeValuetoSecureStorage('mag-identifier', '');
        _magSecureStorage.writeValuetoSecureStorage('private-key', '');
        _magSecureStorage.writeValuetoSecureStorage('public-key', '');
        _magSecureStorage.writeValuetoSecureStorage('client_id', '');

        _magClientSecurityContext = new SecurityContext(withTrustedRoots: false);
        _readJson();
      } else {
        // If the server did not return a 200 OK response,
        // then throw an exception.
        print('Logout : Failed to load json response from API Gateway');
      }
    } catch (e) {
      print(Exception(e));
    }
    _magSecureStorage.writeValuetoSecureStorage('magUserName', '');
    _magSecureStorage.writeValuetoSecureStorage('magUserPassword', '');

    print('LogoutHandler: Logout at the MAG Server');

    setState(() {
      _hideLoginButton = false;
      _apiJsonResult = '';
      _userAuthenticatedStatus = 'Not Authenticated';
      _appTitle = 'MAG Native Flutter App';
    });
  }

  Future<void> initialiseMagClient() async {
    print('Stage 1: About to Initialise the MAG Client with the MAG Server');

    try {
      final String magInitialisationUrl =
          'https://' + _magServerHostname + ':' + _magServerHostPort.toString() + '/connect/client/initialize';

      String magClientId = await _magSecureStorage.getValuefromSecureStorage('client_id');

      String magBase64ClientDeviceIdentifier = await _magSecureStorage.getValuefromSecureStorage('device_id');

      if (magClientId == '') {
        Map<String, String> headers = {
          "Content-type": "application/x-www-form-urlencoded",
          "device-id": magBase64ClientDeviceIdentifier
        };
        Map<String, String> params = Map();

        params['client_id'] = _magMasterClientId;

        var uuid = Uuid();
        final utf8MagClientInitialisationNonceBytes = utf8.encode(uuid.v1().toString());

        params['nonce'] = base64.encode(utf8MagClientInitialisationNonceBytes);

        HttpClient _httpClient = new HttpClient(context: _magClientSecurityContext)
          ..badCertificateCallback = ((X509Certificate cert, String host, int port) {
            print('Server Name is: [' + host + ']');
            print('Server PEM Certificate: [' + cert.pem + ']');
            showAlertDialog(this.context, "Alert", "Invalid certificate presented by the MAG Server");
            return false;
          });

        IOClient ioClient = new IOClient(_httpClient);
        final response = await ioClient.post(Uri.parse(magInitialisationUrl), headers: headers, body: params);

        // final response = await http.post(Uri.parse(magInitialisationUrl), headers: headers, body: params);

        // check the status code for the result
        int statusCode = response.statusCode;
        print("MAG Client Initialisation Response: [" + response.body.toString() + ']');

        if (statusCode == 200) {
          //
          // Need to extract the returned client_id, client_secret and client_expiration which are in the response body
          //
          final data = json.decode(response.body);

          _magSecureStorage.writeValuetoSecureStorage('client_id', data['client_id']);
          _magSecureStorage.writeValuetoSecureStorage('client_secret', data['client_secret']);
          _magSecureStorage.writeValuetoSecureStorage('client_expiration', data['client_expiration'].toString());

          print('Received Client Id: [' + data['client_id'] + ']');
          print('Received Client Secret: [' + data['client_secret'] + ']');
          print('Received Client Expiration [' + data['client_expiration'] + ']');
        } else {
          throw Exception("Error in login");
        }
      } else {
        print('MAG Client is already initialised with a client id and accompanying secret');
      }
    } catch (e) {
      print(Exception(e));
    }
    await registerMagClient();
  }

  Future<void> registerMagClient() async {
    print('Stage 2: About to Register the MAG Client with the MAG Server');

    String magIdentifier = await _magSecureStorage.getValuefromSecureStorage('mag-identifier');

    String magClientIdToken = await _magSecureStorage.getValuefromSecureStorage('id-token');

    //
    // Do I have a mag identifier so I can skip the actual registration process?
    //

    if (magIdentifier == '' || magClientIdToken == '') {
      try {
        String magUserName = _magUsernameController.text;
        String magUserPassword = _magPasswordController.text;

        const String magOrganziationName = "Broadcom Software";

        //
        // Generate a RSA key pair and a signed CSR
        //

        String deviceName = await _getDeviceName();
        String magBase64ClientDeviceIdentifier = await _magSecureStorage.getValuefromSecureStorage('device_id');

        Map<String, String> csrDN = {
          "CN": magUserName,
          "OU": magBase64ClientDeviceIdentifier,
          "O": magOrganziationName,
          "DC": deviceName,
        };

        String pemEncodedRSAPrivateKey = await _magSecureStorage.getValuefromSecureStorage('private-key');
        String pemEncodedRSAPublicKey = '';
        String pemEncodedCSR = '';

        if (pemEncodedRSAPrivateKey == '') {
          AsymmetricKeyPair keyPair = CryptoUtils.generateRSAKeyPair();
          RSAPrivateKey magClientPrivateKey = keyPair.privateKey as RSAPrivateKey;
          RSAPublicKey magClientPublicKey = keyPair.publicKey as RSAPublicKey;
          pemEncodedCSR = X509Utils.generateRsaCsrPem(csrDN, magClientPrivateKey, magClientPublicKey);
          debugPrint(pemEncodedCSR);
          // debugPrint(encodeRSAPublicKeyToPem(magClientPublicKey));
          pemEncodedRSAPrivateKey = CryptoUtils.encodeRSAPrivateKeyToPem(magClientPrivateKey);
          pemEncodedRSAPublicKey = CryptoUtils.encodeRSAPublicKeyToPem(magClientPublicKey);
        } else {
          pemEncodedRSAPrivateKey = await _magSecureStorage.getValuefromSecureStorage('private-key');

          RSAPrivateKey magClientPrivateKey = CryptoUtils.rsaPrivateKeyFromPem(pemEncodedRSAPrivateKey);
          pemEncodedRSAPublicKey = await _magSecureStorage.getValuefromSecureStorage('public-key');
          RSAPublicKey magClientPublicKey = CryptoUtils.rsaPublicKeyFromPem(pemEncodedRSAPublicKey);
          pemEncodedCSR = X509Utils.generateRsaCsrPem(csrDN, magClientPrivateKey, magClientPublicKey);
        }

        final String magRegistrationUrl =
            'https://' + _magServerHostname + ':' + _magServerHostPort.toString() + '/connect/device/register';

        final utf8UserAuthorizationBytes = utf8.encode(magUserName + ':' + magUserPassword);
        String magClientId = await _magSecureStorage.getValuefromSecureStorage('client_id');
        String magClientSecret = await _magSecureStorage.getValuefromSecureStorage('client_secret');

        final utf8MagClientAuthorizationBytes = utf8.encode(magClientId + ':' + magClientSecret);

        final utf8DeviceNameBytes = utf8.encode(deviceName);

        Map<String, String> headers = {
          "Content-type": "text/plain",
          "device-id": magBase64ClientDeviceIdentifier,
          "device-name": base64.encode(utf8DeviceNameBytes),
          'Authorization': 'Basic ' + base64.encode(utf8UserAuthorizationBytes),
          'client-authorization': 'Basic ' + base64.encode(utf8MagClientAuthorizationBytes),
        };

        HttpClient _httpClient = new HttpClient(context: _magClientSecurityContext)
          ..badCertificateCallback = ((X509Certificate cert, String host, int port) {
            print('Server Name is: [' + host + ']');
            print('Server PEM Certificate: [' + cert.pem + ']');
            showAlertDialog(this.context, "Alert", "Imvalid certificate presented by the MAG Server");
            return false;
          });

        IOClient ioClient = new IOClient(_httpClient);
        final response = await ioClient.post(Uri.parse(magRegistrationUrl), headers: headers, body: pemEncodedCSR);

        int statusCode = response.statusCode;
        debugPrint("MAG Client Initialisation Response: [" + response.body.toString() + ']');

        if (statusCode == 200) {
          //
          // Need to save the Mag Client Private key
          //
          if (await _magSecureStorage.getValuefromSecureStorage('private-key') == '') {
            _magClientSecurityContext.usePrivateKeyBytes(pemEncodedRSAPrivateKey.codeUnits);
            _magSecureStorage.writeValuetoSecureStorage('private-key', pemEncodedRSAPrivateKey);
            _magSecureStorage.writeValuetoSecureStorage('public-key', pemEncodedRSAPublicKey);
          }
          //CryptoUtils.encodeRSAPrivateKeyToPem(magClientPrivateKey));
          //
          // We need to save the Signed certificate to a trust store to be utilised later, when accessing a protected API
          // Extract the values from the following Headers: device-status, id-token, id-token-type, mag-identifier and Server
          //

          if (response.headers['device-status'] == 'activated') {
            print(' MAG Client has been successfuly registered');

            String magClientIdToken = response.headers['id-token'] ?? '';
            String magClientIdTokenType = response.headers['id-token-type'] ?? '';

            String magIdentifier = response.headers['mag-identifier'] ?? '';

            _magSecureStorage.writeValuetoSecureStorage('id-token', magClientIdToken);
            _magSecureStorage.writeValuetoSecureStorage('id-token-type', magClientIdTokenType);
            _magSecureStorage.writeValuetoSecureStorage('mag-identifier', magIdentifier);

            _magClientPEMEncodedCertificate = response.body.toString();

            //
            // Need to remove line feeds in the certificate byte stream
            //

            _magSecureStorage.writeValuetoSecureStorage('x-cert', _magClientPEMEncodedCertificate);

            _magClientSecurityContext.useCertificateChainBytes(_magClientPEMEncodedCertificate.codeUnits);

            //
            // Save the validated username and password
            //
            _magSecureStorage.writeValuetoSecureStorage('magUserName', magUserName);
            _magSecureStorage.writeValuetoSecureStorage('magUserPassword', magUserPassword);
          }
        } else if (statusCode == 401) {
          showAlertDialog(this.context, "Login Failed", "Invalid Login Credentials provided");
          return;
        } else {
          showAlertDialog(this.context, "Login Failed", "Unknown Error returned");
          return;
        }
      } catch (e) {
        print(Exception(e));
      }
    } else {
      print('MAG Client is already Registered with a valid MAG Identifier and Client Certificate');
    }
    await retrieveAccessToken(false);
  }

  Future<void> retrieveAccessToken(bool isIdTokenRequired) async {
    print('Stage 3: About to retrieve an Access Token the MAG Server');

    final String magTokenEndPointUrl =
        'https://' + _magServerHostname + ':' + _magServerHostPort.toString() + '/auth/oauth/v2/token';

    String magClientAccessToken = await _magSecureStorage.getValuefromSecureStorage('access_token');

    if (magClientAccessToken == '' || await _hasAccessTokenExpired()) {
      try {
        String magIdentifier = await _magSecureStorage.getValuefromSecureStorage('mag-identifier');
        String magClientId = await _magSecureStorage.getValuefromSecureStorage('client_id');
        String magClientSecret = await _magSecureStorage.getValuefromSecureStorage('client_secret');

        Map<String, String> headers = {
          "Content-type": "application/x-www-form-urlencoded",
          "mag-identifier": magIdentifier,
        };

        Map<String, String> params = Map();

        params['client_id'] = magClientId;
        params['client_secret'] = magClientSecret;
        params['scope'] = _magMasterClientScope;

        if (isIdTokenRequired) {
          String magUserName = await _magSecureStorage.getValuefromSecureStorage('magUserName');
          String magUserPassword = await _magSecureStorage.getValuefromSecureStorage('magUserPassword');
          params['grant_type'] = 'password';
          params['username'] = magUserName;
          params['password'] = magUserPassword;
        } else {
          params['assertion'] = await _magSecureStorage.getValuefromSecureStorage('id-token');
          params['grant_type'] = await _magSecureStorage.getValuefromSecureStorage('id-token-type');
        }

        HttpClient _httpClient = new HttpClient(context: _magClientSecurityContext)
          ..badCertificateCallback = ((X509Certificate cert, String host, int port) {
            print('Server Name is: [' + host + ']');
            print('Server PEM Certificate: [' + cert.pem + ']');
            showAlertDialog(this.context, "Alert", "Imvalid certificate presented by the MAG Server");
            return false;
          });

        IOClient ioClient = new IOClient(_httpClient);
        final response = await ioClient.post(Uri.parse(magTokenEndPointUrl), headers: headers, body: params);

        //
        // final response = await http.post(Uri.parse(magTokenEndPointUrl), headers: headers, body: params);
        //
        // check the status code for the result
        int statusCode = response.statusCode;
        print("MAG Client Access Token Retrieval Response: [" + response.body.toString() + ']');

        if (statusCode == 200) {
          print('Successfully retrived an Access Token from the MAG Server');
          final data = json.decode(response.body);
          _magSecureStorage.writeValuetoSecureStorage('access_token', data["access_token"]);
          _magSecureStorage.writeValuetoSecureStorage('token_type', data["token_type"]);
          _magSecureStorage.writeValuetoSecureStorage('refresh_token', data["refresh_token"]);
          _magSecureStorage.writeValuetoSecureStorage('access_token_scope', data["scope"]);

          int magClientAccessTokenExpiresIn = data["expires_in"];

          //
          // Need to set the actual expiry time = Time.now + expiry secs
          //

          int expiryTimeStamp = (DateTime.now().millisecondsSinceEpoch / 1000).floor() + magClientAccessTokenExpiresIn;

          print('Access Token Expiry Timestamp is [' + expiryTimeStamp.toString() + ']');
          _magSecureStorage.writeValuetoSecureStorage('expires_in', expiryTimeStamp.toString());

          setState(() {
            _hideLoginButton = true;
            _accessTokenExpirySec = magClientAccessTokenExpiresIn;
            _countDownTimer = CountdownTimer(
                onEnd: refreshAccessToken,
                endTime: DateTime.now().millisecondsSinceEpoch + 1000 * _accessTokenExpirySec,
                widgetBuilder: (_, CurrentRemainingTime? time) {
                  if (time == null) {
                    return Text('Access Token has expired');
                  }
                  return Text('Access Token Auto-Refreshing in: ${time.min} mins,  ${time.sec} secs');
                });
          });
        }
      } catch (e) {
        print(Exception(e));
      }
    } else {
      print('MAG Client already has an access token');
      setState(() {
        _hideLoginButton = true;
      });
    }
  }

  Future<bool> _hasAccessTokenExpired() async {
    int currentTime = (DateTime.now().millisecondsSinceEpoch / 1000).floor();
    int expiryTimeAccessToken = int.parse(await _magSecureStorage.getValuefromSecureStorage('expires_in'));

    if (expiryTimeAccessToken < currentTime) {
      print('MAG Client Access Token has expired!');
      refreshAccessToken();
      return true;
    } else {
      print('Access Token will expire in [' + (expiryTimeAccessToken - currentTime).toString() + '] secs');
      return false;
    }
  }

  Future<String> _getDeviceName() async {
    DeviceInfoPlugin deviceInfo = DeviceInfoPlugin();

    String deviceName = '';
    if (Platform.isAndroid) {
      AndroidDeviceInfo androidInfo = await deviceInfo.androidInfo;
      deviceName = androidInfo.model;
    } else if (Platform.isIOS) {
      IosDeviceInfo iosInfo = await deviceInfo.iosInfo;
      deviceName = iosInfo.name;
    }
    deviceName = deviceName.replaceAll(' ', '-');
    return deviceName;
  }

  void _initDeviceId() async {
    String magBase64ClientDeviceIdentifier = await _magSecureStorage.getValuefromSecureStorage('device_id');

    if (magBase64ClientDeviceIdentifier == '') {
      var uuid = Uuid();
      final utf8MagClientDeviceIdBytes = utf8.encode(uuid.v1().toString());

      magBase64ClientDeviceIdentifier = base64.encode(utf8MagClientDeviceIdBytes);
      _magSecureStorage.writeValuetoSecureStorage('device_id', magBase64ClientDeviceIdentifier);

      print('Device-ID is [' + magBase64ClientDeviceIdentifier + ']');
    }
  }

  void _initMagClientDialog() async {
    String magClientAccessToken = await _magSecureStorage.getValuefromSecureStorage('access_token');

    print('_initMagClientDialog invoked');
    if (magClientAccessToken != '' && !await _hasAccessTokenExpired()) {
      int currentTime = (DateTime.now().millisecondsSinceEpoch / 1000).floor();
      int expiryTimeAccessToken = int.parse(await _magSecureStorage.getValuefromSecureStorage('expires_in'));

      String magUserName = await _magSecureStorage.getValuefromSecureStorage('magUserName');
      setState(() {
        _apiJsonResult = '';
        _userAuthenticatedStatus = 'Status: User Authenticated';
        _appTitle = 'Welcome user: [' + magUserName + ']';
        _hideLoginButton = true;
      });
    } else if (magClientAccessToken != '' && await _hasAccessTokenExpired()) {
      setState(() {
        _hideLoginButton = false;
        _apiJsonResult = '';
        _userAuthenticatedStatus = 'Not Authenticated';
        _appTitle = 'MAG Native Flutter App';
      });
    }
  }

  Future<void> fetchUnProtectedData() async {
    final String unProtectedUri =
        'https://' + _magServerHostname + ':' + _magServerHostPort.toString() + '/unprotected/products';

    try {
      HttpClient _httpClient = new HttpClient(context: _magClientSecurityContext)
        ..badCertificateCallback = ((X509Certificate cert, String host, int port) {
          print('Server Name is: [' + host + ']');
          print('Server PEM Certificate: [' + cert.pem + ']');
          showAlertDialog(this.context, "Alert", "Imvalid certificate presented by the MAG Server");
          return false;
        });

      IOClient ioClient = new IOClient(_httpClient);

      final response = await ioClient.get(Uri.parse(unProtectedUri));

      //final response = await http.get(Uri.parse(unProtectedUri));

      if (response.statusCode == 200) {
        // If the server did return a 200 OK response,
        // then parse the JSON.
        print('Got a JSON response');
        print(response.body);
        final data = json.decode(response.body);
        setState(() {
          _apiJsonResult = 'Gateway Time is: [' + data["TimeStamp"].toString() + ']';
        });
      } else {
        print('UnProtected API : Failed to load json response from API Gateway');
      }
    } catch (e) {
      print(Exception(e));
    }
  }

  Future<void> _initCurrentLocation() async {
    positionServiceEnabled = await Geolocator.isLocationServiceEnabled();
    if (!positionServiceEnabled) {
      // Location services are not enabled don't continue
      // accessing the position and request users of the
      // App to enable the location services.
      return Future.error('Location services are disabled.');
    }

    deviceLocationPermission = await Geolocator.checkPermission();
    if (deviceLocationPermission == LocationPermission.denied) {
      deviceLocationPermission = await Geolocator.requestPermission();
      if (deviceLocationPermission == LocationPermission.denied) {
        // Permissions are denied, next time you could try
        // requesting permissions again (this is also where
        // Android's shouldShowRequestPermissionRationale
        // returned true. According to Android guidelines
        // your App should show an explanatory UI now.
        return Future.error('Location permissions are denied');
      }
    }
    if (deviceLocationPermission == LocationPermission.deniedForever) {
      // Permissions are denied forever, handle appropriately.
      return Future.error('Location permissions are permanently denied, we cannot request permissions.');
    }

    // When we reach here, permissions are granted and we can
    // continue accessing the position of the device.

    await Geolocator.getCurrentPosition(desiredAccuracy: LocationAccuracy.best, forceAndroidLocationManager: true)
        .then((Position position) {
      print(
          'Device Location: Latitude:' + position.latitude.toString() + ', Longitude:' + position.longitude.toString());
    }).catchError((e) {
      print(e);
    });
  }

  Future<String> getDeviceCurrentLocation() async {
    String deviceLocation = '';
    await Geolocator.getCurrentPosition(desiredAccuracy: LocationAccuracy.best, forceAndroidLocationManager: true)
        .then((Position position) {
      deviceLocation = (position.latitude.toString() + ',' + position.longitude.toString());
    }).catchError((e) {
      print(e);
    });
    return deviceLocation;
  }

  Future<void> fetchProtectedData() async {
    final String protectedUri = 'https://' +
        _magServerHostname +
        ':' +
        _magServerHostPort.toString() +
        '/protected/resource/products?operation=listProducts';

    try {
      String pemEncodedPrivateKey = await _magSecureStorage.getValuefromSecureStorage('private-key');

      _magClientSecurityContext.usePrivateKeyBytes(pemEncodedPrivateKey.codeUnits);

      String pemEncodedMagClientCertificate = await _magSecureStorage.getValuefromSecureStorage('x-cert');

      _magClientSecurityContext.useCertificateChainBytes(pemEncodedMagClientCertificate.codeUnits);

      HttpClient _httpClient = new HttpClient(context: _magClientSecurityContext)
        ..badCertificateCallback = ((X509Certificate cert, String host, int port) {
          print('Server Name is: [' + host + ']');
          print('Server PEM Certificate: [' + cert.pem + ']');
          showAlertDialog(this.context, "Alert", "Imvalid certificate presented by the MAG Server");
          return false;
        });

      IOClient ioClient = new IOClient(_httpClient);

      Map<String, String> headers = {
        "mag-identifier": await _magSecureStorage.getValuefromSecureStorage('mag-identifier'),
        "Authorization": 'Bearer ' + await _magSecureStorage.getValuefromSecureStorage('access_token'),
        "x-cert": base64.encode(utf8.encode(pemEncodedMagClientCertificate)),
        "geo-location": await getDeviceCurrentLocation(),
      };

      final response = await ioClient.get(Uri.parse(protectedUri), headers: headers);

      if (response.statusCode == 200) {
        //
        // If the server did return a 200 OK response,then parse the JSON.
        //
        print('Got a JSON response');
        print(response.body);
        final data = json.decode(response.body);
        setState(() {
          JsonEncoder encoder = new JsonEncoder.withIndent('  ');
          String protectedDataRespPrettyPrint = encoder.convert(data['products']);

          _apiJsonResult = 'GW Protected Products:\n' + protectedDataRespPrettyPrint;
        });
      } else {
        // If the server did not return a 200 OK response,
        // then throw an exception.
        print('Protected API : Failed to load json response from API Gateway');
      }
    } catch (e) {
      print(Exception(e));
    }
  }

  _MagHomePageState() {
    _readJson();

    _initCurrentLocation();

    _initDeviceId();

    _initMagClientDialog();
  }

  // Fetch content from the MAG Configuration Json file
  Future<void> _readJson() async {
    try {
      final String response = await rootBundle.loadString('assets/msso_config.json');

      final data = await json.decode(response);

      _magServerHostname = data["server"]["hostname"];
      _magServerHostPort = data["server"]["port"];
      _magEnablePkiPinning = data['mag']['mobile_sdk']['enable_public_key_pinning'];

      print('SSL Pinning Status: [' + _magEnablePkiPinning.toString() + ']');

      if (data["server"]["server_certs"].isNotEmpty) {
        //
        // Need to clean up the Server certificate
        //

        String extractPemCertificate = data["server"]["server_certs"][0].toString();

        _magServerPEMEncodedCertificate = extractPemCertificate.replaceAll(', ', "\n");
        _magServerPEMEncodedCertificate = _magServerPEMEncodedCertificate.replaceAll("[", '');
        _magServerPEMEncodedCertificate = _magServerPEMEncodedCertificate.replaceAll("]", '');

        var pemEncodedMagCertBytes = utf8.encode(_magServerPEMEncodedCertificate);

        // If SSL Pinning is enabled then w need to check the MAG Server presented certificate that is provided
        // is acceptable to the one provifded in the msso config file.

        if (_magEnablePkiPinning) {
          _magClientSecurityContext = new SecurityContext(withTrustedRoots: false);
          _magClientSecurityContext.setTrustedCertificatesBytes(pemEncodedMagCertBytes);
        }

        HttpClient _httpClient = new HttpClient(context: _magClientSecurityContext)
          ..badCertificateCallback = ((X509Certificate cert, String host, int port) {
            print('Server Name is: [' + host + ']');
            print('Server PEM Certificate: [' + cert.pem + ']');
            showAlertDialog(this.context, "Alert", "Imvalid certificate presented by the MAG Server");
            return false;
          });
      } else {
        print("Invalid msso json config file found - no server certificate");

        showAlertDialog(this.context, "Alert", "Missing MSSO Server certificate in config file");
      }

      //
      // Check the length of the array present array is actually 1
      //
      List _magClientIds = data["oauth"]["client"]["client_ids"];

      if (_magClientIds.isNotEmpty) {
        print(_magServerHostname);
        _magMasterClientId = data["oauth"]["client"]["client_ids"][0]["client_id"];
        _magMasterClientScope = data["oauth"]["client"]["client_ids"][0]["scope"];
      } else {
        print("Invalid msso json config file found");
      }

      print(_magServerHostPort);
    } catch (e) {
      showAlertDialog(this.context, "Alert", "Improper or missing MSSO Config file");
    }
  }

  @override
  Widget build(BuildContext context) {
    Widget userAuthenticatedStatusSection = Container(
      padding: EdgeInsets.all(40),
      child: Text(
        _userAuthenticatedStatus,
        softWrap: true,
        style: TextStyle(
          color: Colors.red,
        ),
      ),
    );

    Widget apiResultsSection = Container(
        margin: const EdgeInsets.all(15.0),
        padding: const EdgeInsets.all(3.0),
        alignment: Alignment.center,
        width: MediaQuery.of(context).size.width,
        height: MediaQuery.of(context).size.height * 0.30,
        decoration: BoxDecoration(
          color: Colors.grey.withOpacity(0.6),
          border: Border.all(
            color: Colors.blueGrey,
            width: 2.0,
          ),
          borderRadius: BorderRadius.all(
            Radius.circular(10.0),
          ),
        ),
        child: new SingleChildScrollView(
          child: Text(
            _apiJsonResult,
            style: TextStyle(
              color: Colors.red,
              fontSize: 15,
            ),
          ),
        ));

    Color color = Theme.of(context).primaryColor;

    Widget magLoginButtonSection = Row(
      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
      children: [
        if (!_hideLoginButton) Button(color, 'Login', _loginHandler),
      ],
    );

    Widget apiInteractionSection = Row(
      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
      children: [
        Button(color, 'UnProtected API', _upProtectedAPIHandler),
        if (_hideLoginButton) Button(color, 'Protected API', _protectedAPIHandler),
      ],
    );

    Widget magLogoutSection = Row(
      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
      children: [
        if (_hideLoginButton) Button(color, 'Logout', _logoutHandler),
      ],
    );
    Widget magDeRegisterSection = Row(
      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
      children: [
        if (_hideLoginButton) Button(color, 'De-Register', _deRegisterHandler),
      ],
    );

    Widget accessTokenCountDownSection = Row(
      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
      children: [
        if (_hideLoginButton) _countDownTimer,
      ],
    );

    double sizedBoxHeight = 20.0;

    return Scaffold(
      appBar: AppBar(
        // Here we take the value from the MyHomePage object that was created by
        // the App.build method, and use it to set our appbar title.
        title: Text(_appTitle),
      ),
      body: Column(
        children: [
          SizedBox(height: sizedBoxHeight),
          magLoginButtonSection,
          SizedBox(height: sizedBoxHeight),
          apiInteractionSection,
          SizedBox(height: sizedBoxHeight),
          apiResultsSection,
          if (_showProgressIndicator) Container(child: CircularProgressIndicator(), width: 32, height: 32),
          SizedBox(height: sizedBoxHeight),
          magLogoutSection,
          SizedBox(height: sizedBoxHeight),
          magDeRegisterSection,
          SizedBox(height: sizedBoxHeight),
          accessTokenCountDownSection,
          Expanded(
            child: Align(
              alignment: FractionalOffset.bottomCenter,
              child: userAuthenticatedStatusSection,
            ),
          ),
        ],
      ),
    );
  }

  void refreshAccessToken() async {
    final String magTokenEndPointUrl =
        'https://' + _magServerHostname + ':' + _magServerHostPort.toString() + '/auth/oauth/v2/token';

    //
    // Auto refresh the Access Token when it has expired
    //
    print('Refreshing the current Access Token');

    try {
      String pemEncodedPrivateKey = await _magSecureStorage.getValuefromSecureStorage('private-key');
      _magClientSecurityContext.usePrivateKeyBytes(pemEncodedPrivateKey.codeUnits);

      String pemEncodedMagClientCertificate = await _magSecureStorage.getValuefromSecureStorage('x-cert');

      _magClientSecurityContext.useCertificateChainBytes(pemEncodedMagClientCertificate.codeUnits);

      HttpClient _httpClient = new HttpClient(context: _magClientSecurityContext)
        ..badCertificateCallback = ((X509Certificate cert, String host, int port) {
          print('Server Name is: [' + host + ']');
          print('Server PEM Certificate: [' + cert.pem + ']');
          showAlertDialog(this.context, "Alert", "Imvalid certificate presented by the MAG Server");
          return false;
        });

      IOClient ioClient = new IOClient(_httpClient);

      String magClientId = await _magSecureStorage.getValuefromSecureStorage('client_id');
      String magClientSecret = await _magSecureStorage.getValuefromSecureStorage('client_secret');

      final utf8MagClientAuthorizationBytes = utf8.encode(magClientId + ':' + magClientSecret);

      Map<String, String> headers = {
        "Content-Type": 'application/x-www-form-urlencoded',
        "mag-identifier": await _magSecureStorage.getValuefromSecureStorage('mag-identifier'),
        "Authorization": 'Basic ' + base64.encode(utf8MagClientAuthorizationBytes),
      };

      Map<String, String> params = Map();

      params['grant_type'] = 'refresh_token';
      params['refresh_token'] = await _magSecureStorage.getValuefromSecureStorage('refresh_token');

      //
      // Need to ensure we are performing Mutual SSL with the MAG Server
      //

      final response = await ioClient.post(Uri.parse(magTokenEndPointUrl), headers: headers, body: params);

      if (response.statusCode == 200) {
        print('Successfully retrived an Access Token from the MAG Server');
        final data = json.decode(response.body);
        _magSecureStorage.writeValuetoSecureStorage('access_token', data["access_token"]);
        _magSecureStorage.writeValuetoSecureStorage('token_type', data["token_type"]);
        _magSecureStorage.writeValuetoSecureStorage('refresh_token', data["refresh_token"]);
        _magSecureStorage.writeValuetoSecureStorage('access_token_scope', data["scope"]);

        int magClientAccessTokenExpiresIn = data["expires_in"];

        //
        // Need to set the actual expiry time = Time.now + expiry secs
        //

        int expiryTimeStamp = (DateTime.now().millisecondsSinceEpoch / 1000).floor() + magClientAccessTokenExpiresIn;

        print('Access Token Expiry Timestamp is [' + expiryTimeStamp.toString() + ']');
        _magSecureStorage.writeValuetoSecureStorage('expires_in', expiryTimeStamp.toString());

        setState(() {
          _hideLoginButton = true;
          _accessTokenExpirySec = magClientAccessTokenExpiresIn;
          _countDownTimer = CountdownTimer(
              onEnd: refreshAccessToken,
              endTime: DateTime.now().millisecondsSinceEpoch + 1000 * _accessTokenExpirySec,
              widgetBuilder: (_, CurrentRemainingTime? time) {
                if (time == null) {
                  return Text('Access Token has expired');
                }
                return Text('Access Token Auto-Refreshing in: ${time.min} mins,  ${time.sec} secs');
              });
        });
      }
    } catch (e) {
      print(Exception(e));
    }
  }

  showAlertDialog(BuildContext context, String alertTitle, String alertMessage) {
    // set up the button
    Widget okButton = TextButton(
      child: Text("OK"),
      onPressed: () => Navigator.of(context).pop(), // dismiss dialog,
    );

    // set up the AlertDialog
    AlertDialog alert = AlertDialog(
      title: Text(alertTitle),
      content: Text(alertMessage),
      actions: [
        okButton,
      ],
    );

    // show the dialog
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return alert;
      },
    );
  }

  Future<void> _onLoginPressed() async {
    Navigator.pop(context);
    await initialiseMagClient();

    String magUserName = await _magSecureStorage.getValuefromSecureStorage('magUserName');

    if (magUserName != '') {
      setState(() {
        _apiJsonResult = '';
        _userAuthenticatedStatus = 'Status: User Authenticated';
        _appTitle = 'Welcome user: [' + magUserName + ']';
      });
    }
  }

  _onAlertWithCustomContentPressed(context) async {
    Alert(
        context: context,
        title: "LOGIN",
        content: Column(
          children: <Widget>[
            TextField(
              controller: _magUsernameController,
              decoration: InputDecoration(
                icon: Icon(Icons.account_circle),
                labelText: 'Username',
              ),
            ),
            TextField(
              controller: _magPasswordController,
              obscureText: true,
              decoration: InputDecoration(
                icon: Icon(Icons.lock),
                labelText: 'Password',
              ),
            ),
          ],
        ),
        buttons: [
          DialogButton(
            color: Colors.green,
            onPressed: _onLoginPressed,
            child: Text(
              "LOGIN",
              style: TextStyle(color: Colors.white, fontSize: 20),
            ),
          )
        ]).show();
  }
}
