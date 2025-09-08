# Custom Color Picker

This document describes the custom color picker implemented in this project.

## How to Open the Picker

The color picker can be opened by invoking an instance of `ColorPickerDialog`. Typically, this is done by calling its `newInstance()` method and then `show()`:

```kotlin
// Example from an Activity or Fragment
val dialog = ColorPickerDialog.newInstance(
    initialColor = 0xFFFF0000.toInt(), // Example: Red
    showAlpha = true
)
dialog.setOnColorSelectedListener(object : ColorPickerDialog.OnColorSelectedListener {
    override fun onColorSelected(argbInt: Int, hex: String) {
        // Handle the selected color
        // For example, call a method in your Activity/ViewModel
        (activity as? MainActivity)?.onColorChosen(argbInt, hex)
    }
})
dialog.show(supportFragmentManager, "ColorPickerDialog")
```

## How to Receive the Selected Color

The hosting Activity or Fragment should implement the `ColorPickerDialog.OnColorSelectedListener` interface or use the `setOnColorSelectedListener` method to provide a callback.

The callback provides two values:
- `argbInt`: The selected color as an `Int` in ARGB format (e.g., `0xFFFF0000`).
- `hex`: The selected color as a HEX string (e.g., `"#FFFF0000"` or `"#FF0000"` if alpha is opaque or not shown).

The primary callback to implement in the host (e.g., `MainActivity`) is:
```java
public void onColorChosen(int argbInt, String hex) {
    // Update UI or theme with the selected color
}
```

## Supported HEX Formats

The HEX input field supports the following formats:
- `#RGB` (e.g., `#F00`)
- `#ARGB` (e.g., `#FF00`)
- `#RRGGBB` (e.g., `#FF0000`)
- `#AARRGGBB` (e.g., `#FFFF0000`)

The input will be normalized internally. The output HEX string from the `onColorSelected` callback will be in `#AARRGGBB` format if alpha is shown and used, or `#RRGGBB` if alpha is opaque or not shown.

## Known Limitations

- This is a custom implementation without third-party libraries.
- For full application-wide theming beyond simple color changes (like toolbar background), the Activity might need to be recreated after a color selection to apply a new theme style.
- The Saturation/Value area is a custom view; complex gradient rendering and touch interactions are handled manually.
