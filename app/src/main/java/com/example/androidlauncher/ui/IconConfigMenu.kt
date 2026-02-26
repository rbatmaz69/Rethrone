package com.example.androidlauncher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.composables.icons.lucide.*
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalLiquidGlassEnabled
import com.example.androidlauncher.LauncherLogic
import java.lang.reflect.Method

@Composable
fun IconConfigMenu(
    apps: List<AppInfo>,
    customIcons: Map<String, String>,
    onIconSelected: (String, String?) -> Unit,
    onClose: () -> Unit
) {
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val isLiquidGlassEnabled = LocalLiquidGlassEnabled.current
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White
    
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps else apps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    var selectedAppForIcon by remember { mutableStateOf<AppInfo?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "App-Icons anpassen",
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                color = mainTextColor
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = mainTextColor)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar with Liquid Glass Optic
        val searchBarModifier = if (isLiquidGlassEnabled) {
            val glassBrush = if (isDarkTextEnabled) {
                Brush.linearGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.10f), Color.Black.copy(alpha = 0.03f)),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.04f)),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            }
            val borderBrush = if (isDarkTextEnabled) {
                Brush.linearGradient(colors = listOf(Color.Black.copy(alpha = 0.2f), Color.Black.copy(alpha = 0.05f)))
            } else {
                Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.05f)))
            }
            Modifier
                .background(glassBrush, RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, borderBrush), RoundedCornerShape(12.dp))
        } else {
            Modifier.background(mainTextColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
        }

        Box(modifier = Modifier.fillMaxWidth().then(searchBarModifier).padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, contentDescription = null, tint = mainTextColor.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(color = mainTextColor, fontSize = 16.sp),
                    cursorBrush = SolidColor(mainTextColor),
                    singleLine = true,
                    decorationBox = { if (searchQuery.isEmpty()) Text("Apps durchsuchen...", color = mainTextColor.copy(alpha = 0.4f), fontSize = 16.sp); it() }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(filteredApps) { app ->
                val customIconName = customIcons[app.packageName]
                val customIcon = if (customIconName != null) getLucideIconByName(customIconName) else null
                
                val itemBackgroundModifier = if (isLiquidGlassEnabled) {
                    val glassBrush = if (isDarkTextEnabled) {
                        Brush.linearGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.02f)),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.02f)),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    }
                    val borderBrush = if (isDarkTextEnabled) {
                        Brush.linearGradient(colors = listOf(Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.03f)))
                    } else {
                        Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.03f)))
                    }
                    Modifier
                        .background(glassBrush, RoundedCornerShape(16.dp))
                        .border(BorderStroke(1.dp, borderBrush), RoundedCornerShape(16.dp))
                } else {
                    Modifier.background(mainTextColor.copy(alpha = 0.03f), RoundedCornerShape(16.dp))
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .then(itemBackgroundModifier)
                        .clickable { selectedAppForIcon = app },
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (customIcon != null) {
                            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                Icon(customIcon, contentDescription = null, tint = mainTextColor, modifier = Modifier.size(24.dp))
                            }
                        } else {
                            AppIconView(app, modifier = Modifier.size(40.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.label, color = mainTextColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            if (customIconName != null) {
                                Text(customIconName, color = mainTextColor.copy(alpha = 0.5f), fontSize = 12.sp)
                            }
                        }
                        if (customIconName != null) {
                            TextButton(onClick = { onIconSelected(app.packageName, null) }) {
                                Text("Reset", color = Color.Red.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedAppForIcon != null) {
        LucideIconPicker(
            onIconSelected = { iconName ->
                onIconSelected(selectedAppForIcon!!.packageName, iconName)
                selectedAppForIcon = null
            },
            onDismiss = { selectedAppForIcon = null },
            isLiquidGlassEnabled = isLiquidGlassEnabled,
            isDarkTextEnabled = isDarkTextEnabled
        )
    }
}

@Composable
fun LucideIconPicker(
    onIconSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    isLiquidGlassEnabled: Boolean,
    isDarkTextEnabled: Boolean
) {
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White
    var searchQuery by remember { mutableStateOf("") }
    
    // Comprehensive list of Lucide icons
    val commonIconNames = remember {
        listOf(
            "Airplay", "AlarmCheck", "AlarmClock", "AlarmClockOff", "Album", "Anchor", "Aperture", "AppWindow", "Apple", "AreaChart", "Armchair", "Asterisk", "Axis3d", "BaggageClaim", "Ban", "Banknote", "BarChart", "BarChart2", "BarChart3", "BarChart4", "BarChartBig", "BarChartHorizontal", "BarChartHorizontalBig", "Barrier", "Baseline", "Bath", "Battery", "BatteryCharging", "BatteryFull", "BatteryLow", "BatteryMedium", "BatteryWarning", "Beaker", "Bean", "BeanOff", "Bed", "BedDouble", "BedSingle", "Beef", "Beer", "Bell", "BellMinus", "BellOff", "BellPlus", "BellRing", "Bike", "Binary", "Biohazard", "Bird", "Bitcoin", "Blinds", "Bluetooth", "BluetoothConnected", "BluetoothOff", "BluetoothSearching", "Bold", "Bomb", "Bone", "Book", "BookCopy", "BookDown", "BookKey", "BookLock", "BookOpen", "BookOpenCheck", "BookPlus", "BookTemplate", "BookType", "BookUp", "Bookmark", "BookmarkMinus", "BookmarkPlus", "Bot", "BoxSelect", "Boxes", "Brackets", "Brain", "BrainCircuit", "BrainCog", "BrickWall", "Briefcase", "Brush", "Bug", "Building", "Building2", "Bus", "Cake", "Calculator", "Calendar", "CalendarCheck", "CalendarCheck2", "CalendarClock", "CalendarDays", "CalendarHeart", "CalendarMinus", "CalendarOff", "CalendarPlus", "CalendarRange", "CalendarSearch", "CalendarX", "CalendarX2", "Camera", "CameraOff", "CandlestickChart", "Car", "Cast", "Castle", "Cat", "Check", "CheckCircle", "CheckCircle2", "CheckSquare", "ChefHat", "Cherry", "ChevronDown", "ChevronFirst", "ChevronLast", "ChevronLeft", "ChevronRight", "ChevronUp", "ChevronsDown", "ChevronsDownUp", "ChevronsLeft", "ChevronsLeftRight", "ChevronsRight", "ChevronsRightLeft", "ChevronsUp", "ChevronsUpDown", "Chrome", "Church", "Cigarette", "CigaretteOff", "Circle", "CircleDashed", "CircleDollarSign", "CircleDot", "CircleDotLab", "CircleEllipsis", "CircleEqual", "CircleOff", "CircleSlash", "CircleSlash2", "CircleUser", "CircleUserRound", "CircuitBoard", "Clipboard", "ClipboardCheck", "ClipboardCopy", "ClipboardEdit", "ClipboardList", "ClipboardPaste", "ClipboardSignature", "ClipboardType", "ClipboardX", "Clock", "Clock1", "Clock10", "Clock11", "Clock12", "Clock2", "Clock3", "Clock4", "Clock5", "Clock6", "Clock7", "Clock8", "Clock9", "Cloud", "CloudDrizzle", "CloudFog", "CloudLightning", "CloudMoon", "CloudMoonRain", "CloudOff", "CloudRain", "CloudRainWind", "CloudSnow", "CloudSun", "CloudSunRain", "Clapperboard", "Code", "Component", "ConciergeBell", "Contact", "Contact2", "Contrast", "Cookie", "Copy", "CopyCheck", "CopyMinus", "CopyPlus", "CopySlash", "CopyX", "Copyleft", "Copyright", "CornerDownLeft", "CornerDownRight", "CornerLeftDown", "CornerLeftUp", "CornerRightDown", "CornerRightUp", "CornerUpLeft", "CornerUpRight", "Cpu", "CreativeCommons", "CreditCard", "Croissant", "Crop", "Cross", "Crosshair", "Crown", "CupSoap", "Currency", "Database", "DatabaseBackup", "DatabaseZap", "Delete", "Diamond", "Dice1", "Dice2", "Dice3", "Dice4", "Dice5", "Dice6", "Dices", "Diff", "Disc", "Disc2", "Disc3", "Divide", "DivideCircle", "DivideSquare", "Dna", "DnaOff", "Dog", "DollarSign", "DoorClosed", "DoorOpen", "Dot", "Download", "DownloadCloud", "Dribbble", "Droplet", "Droplets", "Drumstick", "Dumbbell", "Ear", "EarOff", "Edit", "Edit2", "Edit3", "Egg", "EggFried", "EggOff", "Equal", "EqualNot", "Eraser", "Euro", "Expand", "ExternalLink", "Eye", "EyeOff", "Facebook", "Factory", "Fan", "FastForward", "Feather", "FerrisWheel", "Figma", "File", "FileArchive", "FileAudio", "FileAudio2", "FileBadge", "FileBadge2", "FileBarChart", "FileBarChart2", "FileBox", "FileCheck", "FileCheck2", "FileCode", "FileCode2", "FileCog", "FileCog2", "FileDiff", "FileDigit", "FileDown", "FileEdit", "FileHeart", "FileImage", "FileJson", "FileJson2", "FileKey", "FileKey2", "FileLineChart", "FileLock", "FileLock2", "FileMinus", "FileMinus2", "FileMusic", "FileOutput", "FilePlus", "FilePlus2", "FileQuestion", "FileScan", "FileSearch", "FileSearch2", "FileSignature", "FileSpreadsheet", "FileStack", "FileSymlink", "FileTerminal", "FileText", "FileType", "FileType2", "FileUp", "FileVideo", "FileVideo2", "FileWarning", "FileX", "FileX2", "Files", "Film", "Filter", "FilterX", "Fingerprint", "Fish", "FishOff", "Flag", "FlagOff", "FlagTriangleLeft", "FlagTriangleRight", "Flame", "Flashlight", "FlashlightOff", "FlaskConical", "FlaskConicalOff", "FlaskRound", "FlipHorizontal", "FlipHorizontal2", "FlipVertical", "FlipVertical2", "Flower", "Flower2", "Focus", "Folder", "FolderArchive", "FolderCheck", "FolderCog", "FolderCog2", "FolderDown", "FolderEdit", "FolderHeart", "FolderInput", "FolderKey", "FolderLock", "FolderMinus", "FolderOpen", "FolderOutput", "FolderPlus", "FolderSearch", "FolderSearch2", "FolderSymlink", "FolderUp", "FolderX", "Folders", "Footprints", "Forklift", "FormInput", "Forward", "Frame", "Framer", "Frown", "Fuel", "FunctionSquare", "Gamepad", "Gamepad2", "Gavel", "Gem", "Ghost", "Gift", "GitBranch", "GitBranchPlus", "GitCommit", "GitCompare", "GitFork", "GitMerge", "GitPullRequest", "GitPullRequestClosed", "GitPullRequestDraft", "GitPullRequestCreate", "GitPullRequestCreateArrow", "Github", "Gitlab", "GlassWater", "Globe", "Grid", "Grip", "GripHorizontal", "GripVertical", "GroupBox", "Hammer", "Hand", "HandMetal", "HardDrive", "HardDriveDownload", "HardDriveUpload", "HardHat", "Hash", "Haze", "HdmiPort", "Heading", "Heading1", "Heading2", "Heading3", "Heading4", "Heading5", "Heading6", "Headphones", "Heart", "HeartHandshake", "HeartOff", "HeartPulse", "HelpCircle", "HelpingHand", "Hexagon", "Highlighter", "History", "Home", "Hop", "HopOff", "Hotel", "Hourglass", "IceCream", "IceCream2", "Image", "ImageMinus", "ImageOff", "ImagePlus", "Import", "Inbox", "Indented", "Infinity", "Info", "Inspect", "Instagram", "Italic", "IterationCcw", "IterationCw", "JapaneseYen", "Joystick", "Kanban", "KanbanSquare", "Key", "Keyboard", "Lamp", "LampCeiling", "LampDesk", "LampFloor", "LampWallDown", "LampWallUp", "Landmark", "Languages", "Laptop", "Layers", "Layout", "LayoutDashboard", "LayoutList", "LayoutPanelLeft", "LayoutPanelTop", "LayoutTemplate", "Leaf", "Library", "LifeBuoy", "Lightbulb", "LightbulbOff", "LineChart", "Link", "Link2", "Link2Off", "Linkedin", "List", "ListCheck", "ListChecks", "ListEnd", "ListFilter", "ListMinus", "ListMusic", "ListOrdered", "ListPlus", "ListRestart", "ListStart", "ListTodo", "ListVideo", "ListX", "Loader", "Loader2", "Locate", "LocateFixed", "LocateOff", "Lock", "LogOut", "LogIn", "Luggage", "Magnet", "Mail", "MailOpen", "MailWarning", "Mailbox", "Mails", "Map", "MapPin", "MapPinOff", "Martini", "Maximize", "Maximize2", "Medal", "Megaphone", "MegaphoneOff", "Menu", "MessageCircle", "MessageSquare", "Mic", "Mic2", "MicOff", "Microscope", "Microwave", "Milestone", "Milk", "MilkOff", "Minimize", "Minimize2", "Mission", "Monitor", "MonitorCheck", "MonitorDot", "MonitorOff", "MonitorSpeaker", "MonitorX", "Moon", "Mountain", "MountainSnow", "Mouse", "MousePointer", "MousePointer2", "MousePointerClick", "Move", "Move3d", "MoveDiagonal", "MoveDiagonal2", "MoveHorizontal", "MoveVertical", "Music", "Music2", "Music3", "Music4", "Navigation", "Navigation2", "Navigation2Off", "NavigationOff", "Network", "Newspaper", "Nfc", "Nut", "NutOff", "Octagon", "Option", "Outdent", "Package", "Package2", "PackageCheck", "PackageMinus", "PackageOpen", "PackagePlus", "PackageSearch", "PackageX", "PaintBucket", "Paintbrush", "Paintbrush2", "Palette", "PanelBottom", "PanelBottomClose", "PanelBottomInactive", "PanelBottomOpen", "PanelLeft", "PanelLeftClose", "PanelLeftInactive", "PanelLeftOpen", "PanelRight", "PanelRightClose", "PanelRightInactive", "PanelRightOpen", "PanelTop", "PanelTopClose", "PanelTopInactive", "PanelTopOpen", "Paperclip", "Parentheses", "ParkingCircle", "ParkingCircleOff", "ParkingSquare", "ParkingSquareOff", "PartyPopper", "Pause", "PauseCircle", "PauseOctagon", "PcCase", "Pencil", "Percent", "PersonStanding", "Phone", "PhoneCall", "PhoneForwarded", "PhoneIncoming", "PhoneMissed", "PhoneOff", "PhoneOutgoing", "PieChart", "PiggyBank", "Pilate", "Pill", "Pin", "PinOff", "Pipette", "Pizza", "Plane", "PlaneLanding", "PlaneTakeoff", "Play", "PlayCircle", "PlaySquare", "Plug", "Plug2", "Plus", "PlusCircle", "PlusSquare", "Pocket", "PocketKnife", "Podcast", "Pointer", "PoundSterling", "Power", "PowerOff", "Presentation", "Printer", "Projector", "Puzzle", "Pyramid", "QrCode", "Quote", "Rabbit", "Radar", "Radiation", "Radio", "RadioReceiver", "RadioTower", "Railcar", "Rat", "Ratio", "Receipt", "RectangleHorizontal", "RectangleVertical", "Recycle", "Redo", "Redo2", "RedoDot", "RefreshCcw", "RefreshCcwDot", "RefreshCw", "RefreshCwDot", "Refrigerator", "Regex", "Repeat", "Repeat1", "Repeat2", "Replace", "ReplaceAll", "Reply", "ReplyAll", "Rewind", "Rocket", "RockingChair", "RotateCcw", "RotateCw", "Route", "Router", "Rows", "Rss", "Ruler", "RussianRuble", "Sailboat", "Salad", "Sandwich", "Save", "Satellite", "SatelliteDish", "Scale", "Scale3d", "Scan", "ScanFace", "ScanLine", "Scissors", "ScreenShare", "ScreenShareOff", "Scroll", "ScrollText", "Search", "Send", "SeparatorHorizontal", "SeparatorVertical", "Server", "ServerCog", "ServerCrash", "ServerOff", "Settings", "Settings2", "Shapes", "Share", "Share2", "Sheet", "Shield", "ShieldAlert", "ShieldCheck", "ShieldClose", "ShieldOff", "ShieldQuestion", "Ship", "Shirt", "ShoppingBag", "ShoppingCart", "Shovel", "ShowerHead", "Shrink", "Shrub", "Shuffle", "Sigma", "Signal", "SignalHigh", "SignalLow", "SignalMedium", "SignalZero", "Siren", "SkipBack", "SkipForward", "Skull", "Slack", "Slice", "Sliders", "SlidersHorizontal", "Smartphone", "SmartphoneCharging", "SmartphoneNfc", "Smile", "SmilePlus", "Snail", "Snowflake", "Sofa", "Soup", "Space", "Sprout", "Speaker", "Spline", "Split", "Smartphone", "Square", "SquareAsterisk", "SquareCode", "SquareDollarSign", "SquareDot", "SquareEqual", "SquareSlash", "Squirrel", "Stamp", "Star", "StarHalf", "StarOff", "StepBack", "StepForward", "Stethoscope", "Sticker", "StickyNote", "StopCircle", "StretchHorizontal", "StretchVertical", "Strikethrough", "Smartphone", "Subscript", "Subtitles", "Sun", "SunDim", "SunMedium", "SunMoon", "SunRain", "SunSnow", "Sunrise", "Sunset", "Superscript", "SwissFranc", "SwitchCamera", "Sword", "Swords", "Syringe", "Table", "Table2", "TableProperties", "Tablet", "Tag", "Tags", "Tally1", "Tally2", "Tally3", "Tally4", "Tally5", "Target", "Tent", "Terminal", "TerminalSquare", "Text", "TextCursor", "TextCursorInput", "TextQuote", "TextSelect", "Thermometer", "ThermometerSnowflake", "ThermometerSun", "ThumbsDown", "ThumbsUp", "Ticket", "Timer", "TimerOff", "TimerReset", "ToggleLeft", "ToggleRight", "Tornado", "TowerControl", "ToyBrick", "Train", "Trash", "Trash2", "TreePine", "TreePalm", "Trees", "Trello", "TrendingDown", "TrendingUp", "Triangle", "Trophy", "Truck", "Tv", "Tv2", "Twitch", "Twitter", "Type", "Umbrella", "Underline", "Undo", "Undo2", "UndoDot", "UnfoldHorizontal", "UnfoldVertical", "Ungroup", "Unlink", "Unlink2", "Unlock", "Upload", "UploadCloud", "Usb", "User", "UserCheck", "UserMinus", "UserPlus", "UserX", "Users", "Utensils", "UtensilsCrossed", "Variable", "VenetianMask", "Vibrate", "Video", "VideoOff", "View", "Voicemail", "Volume", "Volume1", "Volume2", "VolumeX", "Vote", "Wallet", "Wand", "Wand2", "Watch", "Waves", "Webcam", "Webhook", "Wheat", "WheatOff", "WholeWord", "Wifi", "WifiOff", "Wind", "Wine", "WineOff", "WrapText", "X", "XCircle", "XOctagon", "XSquare", "Youtube", "Zap", "ZapOff", "ZoomIn", "ZoomOut"
        ).distinct().sorted()
    }

    val allIcons = remember { 
        commonIconNames.mapNotNull { name ->
            val icon = getLucideIconByName(name)
            if (icon != null) name to icon else null
        }
    }

    val filteredIcons = remember(searchQuery, allIcons) {
        if (searchQuery.isBlank()) allIcons else allIcons.filter { it.first.contains(searchQuery, ignoreCase = true) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = mainTextColor)
                    }
                    Text("Icon wählen", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = mainTextColor)
                    Spacer(modifier = Modifier.width(48.dp)) // Placeholder for balance
                }

                // Search Bar - FULL WIDTH with Liquid Glass style
                val searchBarModifier = if (isLiquidGlassEnabled) {
                    val glassBrush = if (isDarkTextEnabled) {
                        Brush.linearGradient(colors = listOf(Color.Black.copy(alpha = 0.10f), Color.Black.copy(alpha = 0.03f)))
                    } else {
                        Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.04f)))
                    }
                    val borderBrush = if (isDarkTextEnabled) {
                        Brush.linearGradient(colors = listOf(Color.Black.copy(alpha = 0.2f), Color.Black.copy(alpha = 0.05f)))
                    } else {
                        Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.05f)))
                    }
                    Modifier
                        .background(glassBrush, RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, borderBrush), RoundedCornerShape(12.dp))
                } else {
                    Modifier.background(mainTextColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                }

                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).then(searchBarModifier)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Icons suchen...", color = mainTextColor.copy(alpha = 0.4f)) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = mainTextColor.copy(alpha = 0.4f)) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = mainTextColor,
                            unfocusedTextColor = mainTextColor,
                            cursorColor = mainTextColor,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(72.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 32.dp, start = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredIcons) { (name, icon) ->
                        val iconBoxModifier = if (isLiquidGlassEnabled) {
                            val glassBrush = if (isDarkTextEnabled) {
                                Brush.linearGradient(colors = listOf(Color.Black.copy(alpha = 0.08f), Color.Black.copy(alpha = 0.02f)))
                            } else {
                                Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.03f)))
                            }
                            Modifier.background(glassBrush, RoundedCornerShape(12.dp))
                                .border(BorderStroke(0.5.dp, mainTextColor.copy(alpha = 0.1f)), RoundedCornerShape(12.dp))
                        } else {
                            Modifier.background(mainTextColor.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        }

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .then(iconBoxModifier)
                                .clickable { onIconSelected(name) }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(icon, contentDescription = name, tint = mainTextColor, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(name, fontSize = 9.sp, color = mainTextColor.copy(alpha = 0.6f), maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}
