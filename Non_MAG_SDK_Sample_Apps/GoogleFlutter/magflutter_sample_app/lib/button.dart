import 'package:flutter/material.dart';

class Button extends StatelessWidget {
  final VoidCallback buttonCallBack;
  final String buttonLabel;
  Color buttonColor;

  Button(this.buttonColor, this.buttonLabel, this.buttonCallBack);

  @override
  Widget build(BuildContext context) {
    final ButtonStyle style =
        ElevatedButton.styleFrom(textStyle: const TextStyle(fontSize: 20));
    return Column(
      mainAxisSize: MainAxisSize.min,
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        ElevatedButton(
          style: style,
          onPressed: this.buttonCallBack,
          child: Text(this.buttonLabel),
        ),
      ],
    );
  }
}
