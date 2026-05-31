import os
import re

files_to_update = [
    "app/src/main/java/com/example/androidlauncher/ui/HomeScreen.kt",
    "app/src/main/java/com/example/androidlauncher/ui/AppDrawer.kt",
    "app/src/main/java/com/example/androidlauncher/ui/HybridSearch.kt",
    "app/src/main/java/com/example/androidlauncher/ui/SettingsPaletteMenu.kt",
    "app/src/main/java/com/example/androidlauncher/ui/IconConfigMenu.kt",
    "app/src/main/java/com/example/androidlauncher/ui/FavoritesConfigMenu.kt",
    "app/src/main/java/com/example/androidlauncher/ui/FolderConfigMenu.kt",
    "app/src/main/java/com/example/androidlauncher/ui/Utils.kt",
    "app/src/main/java/com/example/androidlauncher/ui/WallpaperCropScreen.kt"
]

def add_import_if_missing(content):
    if "import com.example.androidlauncher.ui.theme.LocalAnimationsEnabled" not in content:
        # insert after package
        content = re.sub(r'^(package .*?\n)', r'\1\nimport com.example.androidlauncher.ui.theme.LocalAnimationsEnabled\nimport androidx.compose.animation.core.snap\n', content)
    return content

def add_val_anim_enabled(content):
    # Find all Composable functions and insert `val animEnabled = LocalAnimationsEnabled.current`
    # at the top level of the composable.
    # This is slightly tricky. Another option:
    return content

for file_path in files_to_update:
    if not os.path.exists(file_path):
        continue
    with open(file_path, "r") as f:
        content = f.read()

    original = content
    content = add_import_if_missing(content)

    # We will replace `spring(` with `if (LocalAnimationsEnabled.current) spring(`
    # but that only works in @Composable.
    # To work everywhere (like inside LaunchedEffect), we can just replace:
    # `spring(` -> `if (com.example.androidlauncher.ui.theme.LocalAnimationsEnabled.current) spring(` - NO, that still needs composable context.

    # Wait, getting LocalAnimationsEnabled.current outside of composable will crash at runtime with "Composable calls are not allowed inside a suspend function"
    pass

