import 'dart:convert';
import 'dart:math';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class magClientStorageHandler {
  static var _magClientSecureStorage = const FlutterSecureStorage();

  Future writeValuetoSecureStorage(String key, String value) async {
    var writeData = _magClientSecureStorage.write(key: key, value: value);

    print('Key [' + key + '] has been written with value: [' + value + ']');
    return writeData;
  }

  Future<String> getValuefromSecureStorage(String key) async {
    try {
      var keyValue = await _magClientSecureStorage.read(key: key);
      if (keyValue == null)
        return '';
      else
        return keyValue.toString();
    } catch (e) {
      print(e);
      _magClientSecureStorage.deleteAll();
    }
    return '';
  }

  Future deleteEntryfromSecureStorage(String key) async {
    var deleteData = await _magClientSecureStorage.delete(key: key);
    return deleteData;
  }
}
