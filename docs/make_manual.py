"""Builds docs/BugReel_Pro_Physical_Test_Manual.pdf (run: python3 docs/make_manual.py docs/BugReel_Pro_Physical_Test_Manual.pdf; needs reportlab + DejaVu fonts)."""
import sys
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (KeepTogether, PageBreak, Paragraph, SimpleDocTemplate, Spacer, Table,
                                TableStyle, ListFlowable, ListItem)

F = "/usr/share/fonts/truetype/dejavu/"
pdfmetrics.registerFont(TTFont("Sans", F + "DejaVuSans.ttf"))
pdfmetrics.registerFont(TTFont("Sans-Bold", F + "DejaVuSans-Bold.ttf"))
pdfmetrics.registerFont(TTFont("Mono", F + "DejaVuSansMono.ttf"))
pdfmetrics.registerFontFamily("Sans", normal="Sans", bold="Sans-Bold", italic="Sans", boldItalic="Sans-Bold")

INK = colors.HexColor("#1f2933")
ACCENT = colors.HexColor("#2b6cb0")
GREEN_BG, GREEN = colors.HexColor("#e6f6ec"), colors.HexColor("#2f855a")
ORANGE_BG, ORANGE = colors.HexColor("#fff4e5"), colors.HexColor("#c05621")
BLUE_BG, BLUE = colors.HexColor("#ebf4ff"), colors.HexColor("#2b6cb0")
GREY_BG = colors.HexColor("#f2f4f7")

base = ParagraphStyle("base", fontName="Sans", fontSize=11, leading=16, textColor=INK, spaceAfter=6)
small = ParagraphStyle("small", parent=base, fontSize=9.5, leading=13)
h1 = ParagraphStyle("h1", parent=base, fontName="Sans-Bold", fontSize=20, leading=26, textColor=ACCENT,
                    spaceBefore=4, spaceAfter=10)
h2 = ParagraphStyle("h2", parent=base, fontName="Sans-Bold", fontSize=14, leading=19, spaceBefore=12, spaceAfter=6)
title = ParagraphStyle("title", parent=h1, fontSize=30, leading=38, alignment=TA_CENTER)
sub = ParagraphStyle("sub", parent=base, fontSize=13, leading=19, alignment=TA_CENTER)
mono = ParagraphStyle("mono", parent=base, fontName="Mono", fontSize=9.5, leading=13)

story = []


def P(t, s=base):
    story.append(Paragraph(t, s))


def H1(t):
    story.append(PageBreak())
    P(t, h1)


def H2(t):
    P(t, h2)


def steps(items, start=1):
    story.append(ListFlowable([ListItem(Paragraph(i, base), leftIndent=22) for i in items],
                              bulletType="1", start=start, leftIndent=22, bulletFontName="Sans-Bold"))


def bullets(items):
    story.append(ListFlowable([ListItem(Paragraph(i, base), leftIndent=16) for i in items],
                              bulletType="bullet", start="•", leftIndent=16))


def box(label, text, bg, fg):
    items = text if isinstance(text, list) else [text]
    cell = [Paragraph(f"<b>{label}</b>", ParagraphStyle("bl", parent=base, textColor=fg))]
    cell += [Paragraph(x, base) for x in items]
    t = Table([[cell]], colWidths=[170 * mm])
    t.setStyle(TableStyle([("BACKGROUND", (0, 0), (-1, -1), bg), ("LINEBEFORE", (0, 0), (0, -1), 4, fg),
                           ("LEFTPADDING", (0, 0), (-1, -1), 10), ("RIGHTPADDING", (0, 0), (-1, -1), 10),
                           ("TOPPADDING", (0, 0), (-1, -1), 8), ("BOTTOMPADDING", (0, 0), (-1, -1), 8)]))
    story.append(Spacer(1, 4))
    story.append(KeepTogether(t))
    story.append(Spacer(1, 6))


def see(t): box("✔  You should see", t, GREEN_BG, GREEN)
def uhoh(t): box("⚠  Uh-oh! If something looks wrong", t, ORANGE_BG, ORANGE)
def why(t): box("ℹ  Why are we doing this?", t, BLUE_BG, BLUE)


def code(t):
    tb = Table([[Paragraph(t.replace("\n", "<br/>"), mono)]], colWidths=[170 * mm])
    tb.setStyle(TableStyle([("BACKGROUND", (0, 0), (-1, -1), GREY_BG), ("LEFTPADDING", (0, 0), (-1, -1), 8),
                            ("TOPPADDING", (0, 0), (-1, -1), 6), ("BOTTOMPADDING", (0, 0), (-1, -1), 6)]))
    story.append(tb)
    story.append(Spacer(1, 6))


def record(test_id):
    t = Table([[Paragraph(f"<b>Result for {test_id}:</b>   ☐ PASS     ☐ FAIL", base)],
               [Paragraph("Notes: ______________________________________________________________", base)],
               [Paragraph("Date: ____________     Tester initials: ________", base)]],
              colWidths=[170 * mm])
    t.setStyle(TableStyle([("BOX", (0, 0), (-1, -1), 1, INK), ("LEFTPADDING", (0, 0), (-1, -1), 8),
                           ("TOPPADDING", (0, 0), (-1, -1), 5), ("BOTTOMPADDING", (0, 0), (-1, -1), 5)]))
    story.append(Spacer(1, 6))
    story.append(KeepTogether(t))


# ---------------------------------------------------------------- cover
story.append(Spacer(1, 50 * mm))
P("BugReel Pro", title)
P("Physical Device Test Manual", sub)
story.append(Spacer(1, 6 * mm))
P("A step-by-step, very-simple walkthrough for every test that needs a real phone", sub)
story.append(Spacer(1, 10 * mm))
P("<b>Test phone:</b> Samsung Galaxy A05s", sub)
P("Version: 2026-10-11 · Covers Sprint 5 sign-off + Launch Readiness Gate", sub)
story.append(Spacer(1, 25 * mm))
why("Robots (automatic tests) already checked a lot of BugReel Pro. But some things only show up on a "
    "real phone held in a real hand: pop-up permission boxes, the floating bubble, Samsung's own "
    "battery rules, and the screen recorder. This little book walks you through each one, like "
    "following a recipe. You do not need to be a programmer. Just go slowly, do one step at a time, "
    "and tick the box at the end of each test.")

# ---------------------------------------------------------------- how to use
H1("How to use this book")
P("Think of this book like building a LEGO set. Each page is one small piece. You do the pieces "
  "<b>in order</b>, and you don't skip ahead.")
H2("The four kinds of coloured boxes")
see("A <b>green box</b> tells you what the phone should look like after you do the steps. If what you "
    "see matches, the test is going well.")
uhoh("An <b>orange box</b> tells you what to do if the phone does something different. Don't panic! "
     "Write down what happened and keep going with the next test unless the box says to stop.")
why("A <b>blue box</b> explains <i>why</i> a test matters, so you know what you are protecting.")
P("At the end of every test there is a little form with <b>☐ PASS</b> and <b>☐ FAIL</b>. Tick one. "
  "If you tick FAIL, write one sentence about what went wrong in the Notes line.")
H2("Words you will see")
bullets([
    "<b>Tap</b> = touch the screen once with one finger and lift it.",
    "<b>Long-press</b> = touch and hold for about two seconds.",
    "<b>Swipe down from the top</b> = put your finger at the very top edge of the screen and slide it "
    "down. This opens the <b>notification shade</b> (the drawer of little messages).",
    "<b>Recents button</b> = the button with three little lines (|||) at the bottom left of the A05s. "
    "It shows all open apps.",
    "<b>Home button</b> = the middle button (□) at the bottom. It takes you to the home screen.",
    "<b>Back button</b> = the arrow (&lt;) at the bottom right.",
    "<b>Computer</b> = the laptop or desktop that has the BugReel Pro code and Android Studio on it.",
])
H2("Golden rules")
steps([
    "Do the parts in order: Part 1 → Part 2 → Part 3 → the Tests.",
    "Only change one thing at a time.",
    "If a test fails, take a screenshot (press <b>Power + Volume Down</b> at the same time) and write "
    "a note. Screenshots go into the Gallery app.",
    "Keep the phone plugged into the computer during Parts 2 and 3.",
])

# ---------------------------------------------------------------- part 0 shopping list
H1("Part 0 — What you need (your shopping list)")
bullets([
    "The <b>Samsung Galaxy A05s</b>, charged to at least 70%.",
    "A <b>USB-C cable</b> that can carry data (not a charge-only cable). If the computer never "
    "notices the phone, try a different cable first.",
    "The <b>computer</b> with Android Studio and this project's code on it.",
    "A <b>Wi-Fi</b> connection for the phone.",
    "Access to the <b>Firebase console</b> (console.firebase.google.com) for the BugReel project.",
    "An <b>OpenAI API key</b> in <font name='Mono'>local.properties</font> (only needed for Test 7, AI Assist).",
    "This book and a pen.",
    "About <b>2 hours</b> for everything, plus a <b>7-day</b> \"just use it\" period at the end (Test 14).",
])
why("If any item is missing, a test later will get stuck. Collecting everything first is like "
    "putting all your ingredients on the table before you start baking.")

# ---------------------------------------------------------------- part 1 prepare phone
H1("Part 1 — Get the phone ready")
H2("1A. Turn on \"Developer options\" (the secret menu)")
P("Phones have a hidden menu for people who build apps. We need to open it once.")
steps([
    "On the phone, open <b>Settings</b> (the grey gear icon).",
    "Scroll all the way down and tap <b>About phone</b>.",
    "Tap <b>Software information</b>.",
    "Find the line that says <b>Build number</b>. Tap it <b>7 times</b>, quickly, like knocking on a door.",
    "If the phone asks for your PIN or pattern, enter it.",
])
see("A small message at the bottom: <b>\"Developer mode has been turned on.\"</b>")
H2("1B. Let the computer talk to the phone (USB debugging)")
steps([
    "Press <b>Back</b> twice to return to the main Settings list.",
    "Scroll to the very bottom. There is now a new item called <b>Developer options</b>. Tap it.",
    "Find <b>USB debugging</b> and turn the switch <b>on</b> (it turns blue). Tap <b>OK</b> on the warning.",
    "Plug the phone into the computer with the USB-C cable.",
    "A box pops up on the phone: <b>\"Allow USB debugging?\"</b>. Tick <b>\"Always allow from this "
    "computer\"</b> and tap <b>Allow</b>.",
])
see("In Android Studio, at the top of the window, the device dropdown now says "
    "<b>samsung SM-A057...</b> (that's the A05s's model code).")
uhoh(["No pop-up? Swipe down from the top of the phone. Tap the notification that says "
      "<b>\"USB for file transfer\"</b> or <b>\"Charging this device via USB\"</b>, and pick "
      "<b>Transferring files / Android Auto</b>.",
      "Still nothing? Try another cable, then another USB port on the computer."])
H2("1C. Write down the phone's software version")
steps([
    "Settings → <b>About phone</b> → <b>Software information</b>.",
    "Write down the <b>One UI version</b> and <b>Android version</b> here: "
    "One UI ______  Android ______",
])
why("Samsung changes how permissions look between versions. If someone else repeats this test "
    "later, they need to know which version you had.")

# ---------------------------------------------------------------- part 2 computer
H1("Part 2 — Get the computer and Firebase ready")
why("The latest code changes (security rules, sign-in, permission fixes) were written in a cloud "
    "computer that could <b>not</b> build Android apps. So the very first job is to let your computer "
    "build it and run the robot tests. If the robots fail, stop and fix that first. Testing a broken "
    "build on the phone wastes your time.")
H2("2A. Get the newest code")
P("In a terminal (Command Prompt or PowerShell on Windows) inside the project folder, type:")
code("git fetch origin\ngit checkout cl/loving-wright-i8p6q4\ngit pull")
H2("2B. Run the robot tests")
code(".\\gradlew test\n.\\gradlew lint")
see("Both commands finish with <b>BUILD SUCCESSFUL</b>.")
uhoh("If you see <b>BUILD FAILED</b>, copy the red error lines and send them to the developer (or to "
     "Claude). Do not continue to the phone tests until this passes.")
H2("2C. Check your secret files are in place")
bullets([
    "<font name='Mono'>local.properties</font> contains <font name='Mono'>OPENAI_API_KEY=sk-...</font>",
    "<font name='Mono'>app/google-services.json</font> exists. Download it from Firebase console → "
    "Project settings (gear icon) → Your apps → Android app → <b>google-services.json</b>.",
])
H2("2D. Switch on \"Anonymous sign-in\" in Firebase")
P("BugReel signs each phone in quietly with an invisible guest account. Firebase must allow that.")
steps([
    "Open <b>console.firebase.google.com</b> and pick the BugReel project.",
    "In the left menu, tap <b>Build → Authentication</b>.",
    "Open the <b>Sign-in method</b> tab.",
    "Tap <b>Anonymous</b>, flip the switch to <b>Enable</b>, and press <b>Save</b>.",
])
see("Anonymous shows as <b>Enabled</b> in the list.")
H2("2E. Lock the doors (deploy the Security Rules)")
P("Security rules are like a bouncer at a club door. They only let each tester see their "
  "<i>own</i> reports. In the terminal:")
code("npm install -g firebase-tools\nfirebase login\nfirebase use --add        (pick the BugReel project)\n"
     "cd firebase-rules-tests\nnpm install\nnpm test                  (should say: pass 18, fail 0)\ncd ..\n"
     "firebase deploy --only firestore:rules,storage")
see("<b>Deploy complete!</b> In Firebase console → Firestore Database → <b>Rules</b> tab, the "
    "rules start with <font name='Mono'>rules_version = '2';</font> and mention <font name='Mono'>"
    "ownsExisting()</font>.")

# ---------------------------------------------------------------- part 3 install
H1("Part 3 — Put the app on the phone")
H2("3A. Start completely fresh")
P("We want the phone to act like a brand-new user's phone. If BugReel Pro is already on it, remove it:")
steps([
    "On the phone, long-press the <b>BugReel Pro</b> icon.",
    "Tap <b>Uninstall</b>, then <b>OK</b>.",
])
H2("3B. Install the debug build")
P("On the computer, with the phone plugged in:")
code(".\\gradlew installDebug")
see("<b>BUILD SUCCESSFUL</b> and <b>Installed on 1 device</b>. A <b>BugReel Pro</b> icon "
    "appears in the phone's app drawer (swipe up on the home screen to see all apps).")
uhoh("\"No connected devices\"? Go back to Part 1B. Unplug and replug the cable, and check the "
     "\"Allow USB debugging\" box on the phone.")

# ---------------------------------------------------------------- tests
H1("Test 1 — The \"permission slip\" (Consent screen)")
why("The law says we must ask testers before we record anything, the same way a school asks "
    "parents to sign a permission slip before a trip. <b>This is a Launch Readiness Gate item. "
    "It must pass.</b>")
steps([
    "Tap the <b>BugReel Pro</b> icon to open the app for the very first time.",
    "Look at the screen. <b>Don't tap anything yet.</b>",
])
see(["The title <b>\"Before you start testing\"</b>.",
     "Text explaining that BugReel records the screen, screenshots and taps, backs up reports to "
     "your private cloud storage, and sends crash diagnostics.",
     "One button: <b>\"I agree, continue\"</b>.",
     "You <b>cannot</b> see the reports list or any capture button behind it."])
steps([
    "Press the <b>Back</b> button (&lt;).",
], start=3)
see("The app closes or goes to the background. It does <b>not</b> sneak you past the permission "
    "slip into the app.")
steps([
    "Open BugReel Pro again. The consent screen should still be there.",
    "Read the text out loud once. Does it make sense to a normal person? Write any confusing "
    "words in the Notes.",
    "Tap <b>\"I agree, continue\"</b>.",
], start=4)
see("A new screen called <b>\"Help improve BugReel's AI\"</b>, with a checkbox <b>\"Yes, use my "
    "reports to help train BugReel's AI\"</b> and a <b>Continue</b> button.")
record("Test 1")

H1("Test 2 — The \"separate yes\" (AI training opt-in)")
why("Agreeing to use the app and agreeing to help train the AI are two <b>different</b> questions. "
    "It's like two separate stickers. Saying yes to one must not secretly say yes to the other.")
steps([
    "On the \"Help improve BugReel's AI\" screen, look at the checkbox.",
])
see("The checkbox is <b>empty</b> (not ticked) to start with.")
steps([
    "For this first run, <b>leave the box empty</b>, and tap <b>Continue</b>.",
], start=2)
see("The <b>Reports</b> screen appears with an empty-state message: <b>\"Your first report will "
    "appear here. Start by recording a test session.\"</b> There is a <b>Start Capture</b> button, a "
    "<b>\"Try Practice Mode first -&gt;\"</b> link, and an <b>Analytics</b> button at the top.")
steps([
    "Swipe up from the bottom to go home, then open the <b>Recents</b> (|||) view and swipe BugReel "
    "Pro away to fully close it.",
    "Open BugReel Pro again.",
], start=3)
see("You go <b>straight</b> to the Reports screen. The consent screens do not come back, because "
    "the phone remembered your answers.")
P("<b>Write it down:</b> AI training opt-in for this run = <b>NO</b>. We'll test <b>YES</b> in Test 11.")
record("Test 2")

H1("Test 3 — Practice Mode (the game)")
why("Practice Mode should feel like a game: write, get a score straight away, try again. It only "
    "appears while you have zero reports, so we test it now, before making real reports.")
steps([
    "On the Reports screen, tap <b>\"Try Practice Mode first -&gt;\"</b>.",
    "In the box <b>\"Write your bug report attempt\"</b>, type something lazy, like: <i>app broke</i>.",
    "Tap <b>Score it</b>.",
])
see("A score like <b>\"Score: 5/100\"</b> (a low number) and some tips that start with a dot (•).")
steps([
    "Now write a better attempt, e.g.: <i>Login button does nothing. 1. Open app 2. Type email "
    "3. Tap Login. Expected: home screen. Actual: nothing happens. Happens every time.</i>",
    "Tap <b>Score it</b> again.",
], start=4)
see("The score goes <b>up</b> compared to your lazy try.")
steps(["Tap <b>Try another scenario</b>, then <b>Done practicing</b>."], start=6)
see("You are back on the Reports screen.")
record("Test 3")

H1("Test 4 — Asking for phone permissions")
why("On the A05s, Android won't let an app show a notification or float a bubble over other apps "
    "until you say \"yes\". These were missing before and were added in the latest update, so watch "
    "closely.")
steps([
    "On the Reports screen, tap <b>Start Capture</b>.",
])
see(["A pop-up: <b>\"Allow BugReel Pro to send you notifications?\"</b>",
     "Behind it, the capture screen says <b>\"Capture bugs like a pro\"</b>."])
steps(["Tap <b>Allow</b>."], start=2)
see("A card called <b>\"Floating capture button\"</b> with a button <b>\"Allow floating button\"</b>.")
steps([
    "Tap <b>Allow floating button</b>.",
    "Samsung opens a settings page called <b>\"Appear on top\"</b> (this is Samsung's name for "
    "\"display over other apps\"). Find <b>BugReel Pro</b> and turn its switch <b>on</b>.",
    "Press <b>Back</b> until you return to BugReel Pro.",
], start=3)
see("The \"Floating capture button\" card has <b>disappeared</b> by itself.")
uhoh(["If the card is still there after you return, write \"overlay card did not refresh\". Then "
      "close and reopen the app and see if it goes away.",
      "If Samsung's page opened the full app list instead, look for BugReel Pro in that list."])
record("Test 4")

H1("Test 5 — Recording a bug (the big one)")
why("This is the heart of the app: record the screen while the tester uses <i>another</i> app, then "
    "turn that recording into a report.")
H2("5A. Start recording")
steps([
    "On the capture screen, tap <b>Start Capture</b>.",
])
see(["Samsung shows a box like <b>\"Start recording or casting with BugReel Pro?\"</b>.",
     "On newer Samsung software there's a choice: <b>\"A single app\"</b> or <b>\"Entire screen\"</b>."])
steps([
    "Choose <b>Entire screen</b> (if asked) and tap <b>Start</b> (or <b>Start now</b>).",
], start=2)
see(["The BugReel screen says <b>\"Recording in progress\"</b> with a <b>Stop Capture</b> button.",
     "A small round <b>floating bubble</b> appears on the edge of the screen.",
     "Swipe down from the top: there's a BugReel Pro notification and Samsung's own red "
     "<b>screen-recording/casting icon</b> in the status bar."])
H2("5B. Use another app")
steps([
    "Press <b>Home</b>. The bubble should stay floating on top of the home screen.",
    "Open <b>Settings</b> (or any app) and tap around for about <b>20 seconds</b>. Pretend you are "
    "testing it.",
    "Drag the bubble with your finger to a new spot. It should follow your finger.",
], start=3)
H2("5C. Stop recording")
steps([
    "Press the <b>Recents</b> button (|||) and tap <b>BugReel Pro</b> to go back to it.",
    "Tap <b>Stop Capture</b>.",
], start=6)
see(["Briefly: <b>\"Capture complete — building your report…\"</b>",
     "Then the report builder opens, saying <b>\"Step 1 of 5\"</b>.",
     "The floating bubble disappears."])
uhoh(["Samsung asks for permission <b>every time</b> you start recording. That is normal on "
      "Samsung, and the app expects it.",
      "If you tap <b>Cancel</b> on Samsung's box, BugReel should say <b>\"Screen recording permission "
      "was denied.\"</b> and go back to the Start Capture button. Try this once on purpose and tick "
      "it below.",
      "Tapping the bubble while recording does nothing in this version. That is expected; stop "
      "from inside the app."])
P("Extra check: ☐ Cancelling Samsung's box showed \"permission was denied\" and did not crash.")
record("Test 5")

H1("Test 6 — Keep recording when the screen is off")
why("Samsung phones try to save battery by stopping apps in the background. If they stop BugReel "
    "mid-recording, the tester loses their evidence.")
steps([
    "Go back to the Reports screen (press Back if you're in the report builder; it's fine to "
    "lose that draft) and start a new capture like in Test 5A.",
    "Press <b>Home</b>, then press the <b>Power</b> button to lock the phone.",
    "Wait <b>2 full minutes</b> (use a clock).",
    "Unlock the phone, open BugReel Pro from Recents, and tap <b>Stop Capture</b>.",
])
see("The report builder opens normally (\"Step 1 of 5\"), and the app did not crash or forget the "
    "recording.")
uhoh(["If recording stopped by itself: open <b>Settings → Apps → BugReel Pro → Battery</b> and pick "
      "<b>Unrestricted</b>. Then repeat this test and write down that you needed this.",
      "That note matters: it tells the team whether to add Samsung to the \"needs battery setup\" "
      "list in the code."])
record("Test 6")

H1("Test 7 — Building a report (5 small steps)")
why("A good report is the product. This test walks every step of the report builder and the AI "
    "helper.")
P("Start a capture and stop it (Test 5) so you are on <b>\"Step 1 of 5\"</b>.")
H2("Step 1 of 5 — Title and description")
steps([
    "Tap <b>Title</b> and type: <i>Settings search crashes on emoji</i>",
    "Tap <b>Description</b> and type at least two sentences about the pretend bug (more than 50 letters).",
    "Pick a <b>Platform</b> from the dropdown (e.g. UTEST). Tap <b>Next</b>.",
])
H2("Step 2 of 5 — Severity and frequency")
steps(["Pick a <b>Severity</b> (e.g. MEDIUM) and a <b>Frequency</b> (e.g. ALWAYS). Tap <b>Next</b>."], start=4)
H2("Step 3 of 5 — Repro steps (how to make the bug happen)")
steps([
    "In <b>Add step</b>, type <i>Open Settings</i> and tap <b>Add</b>.",
    "Add two more: <i>Tap the search icon</i> and <i>Type an emoji</i>.",
    "Try the little <b>↑</b> and <b>↓</b> arrows to move a step, and <b>✕</b> to delete one, then add it "
    "back. Tap <b>Next</b>.",
], start=5)
H2("Step 4 of 5 — Evidence")
steps([
    "Tap <b>Add Evidence</b>. The \"Attach Evidence\" screen opens.",
    "Tap <b>Add Screenshot</b> and pick the screenshot you took earlier from the Gallery.",
    "Optionally paste some text into <b>Paste log text</b>. Tap <b>Done</b>, then <b>Next</b>.",
], start=8)
see("The evidence summary shows <b>Recording: attached</b> (from your capture) and <b>Screenshots: 1</b>.")
H2("Step 5 of 5 — Review and AI Assist")
steps([
    "Tap <b>AI Assist</b>. Wait a few seconds (this calls OpenAI over the internet).",
], start=11)
see(["AI suggestions for title, summary and <b>Repro steps</b> appear, <b>coloured or labelled "
     "differently</b> from your own text so you can tell them apart.",
     "OR the message <b>\"AI suggestion was blocked — showing your original text\"</b>. That is the "
     "safety checker doing its job, and still counts as a pass."])
steps([
    "Change one word in the AI's title (so we record that you edited it). Tap <b>Apply</b>.",
    "Back on the review step, tap <b>Submit</b>.",
], start=12)
see(["The <b>Quality Score</b> screen, with a big number counting up (animated) to the score.",
     "Five rows: Reproduction, Evidence, Description, Severity, Metadata.",
     "If the score is below 70: a message about scores below 70 being less likely to be accepted, plus tips.",
     "Buttons <b>Report Outcome</b> and <b>Done</b>."])
uhoh(["Turn on <b>Airplane mode</b> and try AI Assist once more on a new report. The app must "
      "<b>not</b> freeze or go blank. It should keep your own text.",
      "Tap AI Assist many times fast (more than 20 in a minute). You should see a friendly countdown "
      "<b>\"You've reached the AI limit. Try again in X seconds.\"</b>, never a silent failure."])
record("Test 7")

H1("Test 8 — Telling BugReel what the platform decided")
why("This is <b>the flywheel</b>, the company's most important data. When uTest/Testlio accepts or "
    "rejects a report, the tester tells BugReel. Over time, that teaches BugReel what good reports look "
    "like.")
steps([
    "On the Quality Score screen, tap <b>Report Outcome</b>.",
])
see("A screen asking <b>\"What did the platform decide?\"</b> with two round buttons: ACCEPTED and REJECTED.")
steps([
    "Tap <b>REJECTED</b>.",
])
see("A new box appears: <b>\"Why was it rejected?\"</b>")
steps([
    "Type: <i>Duplicate of an existing bug</i>. Tap <b>Submit</b>.",
    "Make a second report (repeat Test 5 + Test 7 quickly) and this time mark it <b>ACCEPTED</b>.",
], start=3)
record("Test 8")

H1("Test 9 — Peek inside the phone's notebook (database)")
why("The Sprint 5 stop condition says: <b>\"OutcomeFeedbackDao has non-zero rows from internal "
    "testing.\"</b> In plain words: did the phone actually <i>write down</i> the outcomes from Test 8?")
steps([
    "Keep the phone plugged in and BugReel Pro <b>open on screen</b>.",
    "In Android Studio, open <b>View → Tool Windows → App Inspection</b>.",
    "At the top of that panel, choose the A05s and the process <b>com.bugreel.pro</b>.",
    "Click the <b>Database Inspector</b> tab, then open <b>bugreel.db</b> → <b>outcome_feedback</b>.",
])
see(["At least <b>2 rows</b>, one for each report you submitted.",
     "One row with platformOutcome = <b>REJECTED</b> and rejectionReason = <i>Duplicate of an existing "
     "bug</i>; one with <b>ACCEPTED</b>.",
     "preAiQualityScore and postAiQualityScore have numbers in them (not both 0 on the report where you "
     "used AI Assist).",
     "testerOverrodeSuggestion = <b>1</b> (true) on the report where you changed the AI's title."])
P("Write the number of rows here: ______")
uhoh("If the table is empty, this is a <b>launch blocker</b>. Take a screenshot of the Database "
     "Inspector and mark FAIL.")
record("Test 9")

H1("Test 10 — The Analytics dashboard")
steps([
    "Go back to the Reports screen (tap <b>Done</b> on the Quality Score screen).",
])
see("Your reports are listed. Tapping one opens its Quality Score screen again.")
steps([
    "Tap <b>Analytics</b> at the top right.",
], start=2)
see(["An <b>Acceptance rate</b> card (with 1 accepted and 1 rejected, that's <b>50%</b>).",
     "A <b>Quality trend</b> section showing your scores."])
P("<b>Bonus empty-state check:</b> after Test 13 (fresh install) open Analytics with zero reports. "
  "It should show a faded preview of what the chart <i>will</i> look like, not just \"No data\".")
record("Test 10")

H1("Test 11 — Does the cloud get the right things?")
why("Reports are backed up to Firebase. But outcome data (the AI-training data) must <b>only</b> be "
    "uploaded if the tester said <b>YES</b> in the AI opt-in. In Test 2 you said NO.")
H2("11A. With opt-in = NO (your current setup)")
steps([
    "Make sure the phone has Wi-Fi. Wait about <b>2 minutes</b>.",
    "Open Firebase console → <b>Firestore Database</b> → <b>Data</b> tab.",
])
see(["A <b>reports</b> collection with your reports. Each document has a <b>userId</b> that is a long "
     "random code (NOT the word \"anonymous\") and a <b>data</b> field.",
     "<b>No outcome_feedback</b> documents from this phone."])
H2("11B. With opt-in = YES")
steps([
    "On the phone: <b>Settings → Apps → BugReel Pro → Storage → Clear data</b>. (This makes the app "
    "forget everything, like a brand-new install.)",
    "Open BugReel Pro. Agree to consent, and this time <b>tick</b> the AI training box, then Continue.",
    "Make one report and mark its outcome as ACCEPTED (Tests 5, 7, 8).",
    "Wait 2 minutes, then refresh the Firestore page.",
], start=3)
see("An <b>outcome_feedback</b> collection now exists. The document's name is the report's ID, and its "
    "userId matches the new report's userId.")
H2("11C. Offline, then online")
steps([
    "Turn on <b>Airplane mode</b>. Make and submit one more report.",
    "Wait 1 minute. Check Firestore: the new report is <b>not</b> there yet (correct, you're offline).",
    "Turn Airplane mode <b>off</b>. Wait 2 minutes and refresh.",
], start=7)
see("The new report appears in Firestore by itself.")
record("Test 11")

H1("Test 12 — Crash reports only after consent")
why("Crashlytics tells the team when the app crashes. But it should stay quiet until the tester "
    "agrees on the consent screen.")
steps([
    "In Firebase console, open <b>Release &amp; Monitor → Crashlytics</b>.",
    "Check that the app appears and is receiving data. It can take up to 24 hours after the first "
    "launch to show up.",
])
see("Crashlytics shows the app and a <b>crash-free users/sessions</b> number.")
P("<b>Note:</b> this test can be finished the next day. Write the crash-free % you see: ______ %")
record("Test 12")

H1("Test 13 — The \"real shop\" version (release build)")
why("The version testers get from Google Play is squeezed and scrambled (\"minified\") to make it "
    "smaller. Sometimes squeezing breaks things that worked in the normal version, so we test it "
    "separately.")
steps([
    "Ask the developer to add the signing keys to <font name='Mono'>local.properties</font> "
    "(KEYSTORE_PATH, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD).",
    "Uninstall BugReel Pro from the phone.",
    "On the computer run:",
])
code(".\\gradlew assembleRelease\nadb install app\\build\\outputs\\apk\\release\\app-release.apk")
steps([
    "Quickly repeat: <b>Test 1</b> (consent), <b>Test 5</b> (record), <b>Test 7</b> (build + submit, "
    "with AI Assist), <b>Test 8</b> (outcome), <b>Test 10</b> (analytics).",
], start=4)
see("Everything works exactly like the debug version. No sudden closing of the app.")
uhoh("If the release app closes by itself (\"BugReel Pro keeps stopping\"), plug in the phone and run "
     "<font name='Mono'>adb logcat -d &gt; crash.txt</font>, then send crash.txt to the developer. "
     "It usually means a missing \"keep\" rule in proguard-rules.pro.")
record("Test 13")

H1("Test 14 — One week of normal use")
why("The Launch Readiness Gate needs <b>7 days</b> and <b>at least 100 sessions</b> (app openings) "
    "with more than <b>99.5% crash-free</b>. This is just using the app like a real tester would.")
steps([
    "Install the <b>release</b> build (Test 13) on the A05s, plus any other team phones.",
    "Each day for 7 days, open BugReel Pro several times. Record at least one real bug a day and "
    "enter its real outcome when the platform replies.",
    "Write any problem in a list with the date, what you did, and what happened.",
    "On day 7, check Crashlytics (Test 12) and the outcome_feedback count (Test 9).",
])
t = Table([["Day", "Sessions", "Reports", "Crashes?", "Notes"]] + [[str(d), "", "", "", ""] for d in range(1, 8)],
          colWidths=[15 * mm, 25 * mm, 25 * mm, 25 * mm, 80 * mm], rowHeights=[8 * mm] * 8)
t.setStyle(TableStyle([("GRID", (0, 0), (-1, -1), 0.6, INK), ("FONTNAME", (0, 0), (-1, -1), "Sans"),
                       ("FONTNAME", (0, 0), (-1, 0), "Sans-Bold"), ("BACKGROUND", (0, 0), (-1, 0), GREY_BG),
                       ("VALIGN", (0, 0), (-1, -1), "MIDDLE")]))
story.append(t)
record("Test 14")

# ---------------------------------------------------------------- troubleshooting
H1("When something goes wrong: a quick helper")
bullets([
    "<b>The app closed by itself.</b> Plug in, run <font name='Mono'>adb logcat -d &gt; crash.txt</font> "
    "right away, and keep the file.",
    "<b>A button does nothing.</b> Wait 5 seconds. Still nothing? Screenshot, note which button, move on.",
    "<b>Samsung keeps asking to allow recording.</b> Normal for Samsung. Not a bug.",
    "<b>The floating bubble never appears.</b> Check Settings → Apps → BugReel Pro → <b>Appear on "
    "top</b> is ON. If it is ON and there's still no bubble, mark Test 5 FAIL.",
    "<b>No notification.</b> Check Settings → Apps → BugReel Pro → <b>Notifications</b> is ON.",
    "<b>Nothing reaches Firebase.</b> Check Anonymous sign-in is enabled (Part 2D) and the phone has "
    "internet. Then wait a few minutes. Uploads happen in the background, not instantly.",
    "<b>To start over completely:</b> Settings → Apps → BugReel Pro → Storage → <b>Clear data</b>.",
])

# ---------------------------------------------------------------- sign off
H1("Final sign-off sheet")
P("When every test above is ticked, fill this in. It is what CLAUDE.md calls the "
  "\"human device-test sign-off\".")
rows = [["#", "Test", "PASS / FAIL"],
        ["1", "Consent screen (permission slip)", ""], ["2", "Separate AI training opt-in", ""],
        ["3", "Practice Mode", ""], ["4", "Notification + Appear-on-top permissions", ""],
        ["5", "Screen recording + floating bubble", ""], ["6", "Recording survives screen-off", ""],
        ["7", "Report builder + AI Assist + Quality Score", ""], ["8", "Outcome entry (flywheel)", ""],
        ["9", "outcome_feedback rows in database (non-zero)", ""], ["10", "Analytics dashboard", ""],
        ["11", "Cloud sync + opt-in respected", ""], ["12", "Crashlytics receiving after consent", ""],
        ["13", "Release (minified) build smoke test", ""], ["14", "7-day internal use, >99.5% crash-free", ""]]
t = Table(rows, colWidths=[12 * mm, 118 * mm, 40 * mm], rowHeights=[8 * mm] * len(rows))
t.setStyle(TableStyle([("GRID", (0, 0), (-1, -1), 0.6, INK), ("FONTNAME", (0, 0), (-1, -1), "Sans"),
                       ("FONTNAME", (0, 0), (-1, 0), "Sans-Bold"), ("BACKGROUND", (0, 0), (-1, 0), GREY_BG),
                       ("VALIGN", (0, 0), (-1, -1), "MIDDLE")]))
story.append(t)
story.append(Spacer(1, 10))
P("Phone: Samsung Galaxy A05s · One UI ______ · Android ______")
P("Tester name: ______________________   Signature: ______________________   Date: ____________")
story.append(Spacer(1, 6))
box("Still needed outside this book (not phone tests)", [
    "Legal review of the consent screen wording.",
    "Google Play Closed Testing submission, plus the Play review result for the floating bubble. If it "
    "comes back RED, set <font name='Mono'>BUBBLE_FORCE_FALLBACK</font> to true and repeat Tests 4–6.",
    "No open P0/P1 bugs from the notes in this book."], GREY_BG, INK)


def footer(canvas, doc):
    canvas.saveState()
    canvas.setFont("Sans", 8.5)
    canvas.setFillColor(colors.HexColor("#6b7280"))
    canvas.drawString(20 * mm, 10 * mm, "BugReel Pro · Physical Device Test Manual · Galaxy A05s")
    canvas.drawRightString(190 * mm, 10 * mm, f"Page {doc.page}")
    canvas.restoreState()


doc = SimpleDocTemplate(sys.argv[1], pagesize=A4, leftMargin=20 * mm, rightMargin=20 * mm,
                        topMargin=18 * mm, bottomMargin=18 * mm, title="BugReel Pro — Physical Device Test Manual",
                        author="BugReel Pro team")
doc.build(story, onFirstPage=footer, onLaterPages=footer)
